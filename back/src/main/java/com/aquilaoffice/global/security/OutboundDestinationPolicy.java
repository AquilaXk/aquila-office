package com.aquilaoffice.global.security;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 아웃바운드 HTTP/WebSocket 대상 URI에 대해 SSRF 차단 및 Fail-Closed 목적지 검증을 수행한다.
 * (MIT-style reference destination policy logic adapted with bitwise IPv4/IPv6 CIDR masks.)
 */
@Component
public class OutboundDestinationPolicy {

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https", "ws", "wss");
    private static final int IPV4_LENGTH = 4;
    private static final int IPV6_LENGTH = 16;

    private final boolean allowLocalModelHosts;
    private final boolean testBypassEnabled;
    private final HostResolver hostResolver;

    @Autowired
    public OutboundDestinationPolicy(
            @Value("${aquila.model-provider.allow-local-hosts:false}") boolean allowLocalModelHosts) {
        this(allowLocalModelHosts, false, InetAddress::getAllByName);
    }

    OutboundDestinationPolicy(
            boolean allowLocalModelHosts,
            boolean testBypassEnabled,
            HostResolver hostResolver) {
        this.allowLocalModelHosts = allowLocalModelHosts;
        this.testBypassEnabled = testBypassEnabled;
        this.hostResolver = Objects.requireNonNull(hostResolver, "hostResolver must not be null");
    }

    public static OutboundDestinationPolicy strict() {
        return new OutboundDestinationPolicy(false, false, InetAddress::getAllByName);
    }

    public static OutboundDestinationPolicy permissiveForTesting() {
        return new OutboundDestinationPolicy(true, true, InetAddress::getAllByName);
    }

    /**
     * 대상 URI의 scheme, userinfo, 호스트명, DNS 해석 IP 주소를 검증한다.
     *
     * @param uri 검증할 외부 URI
     * @param allowLoopbackForLocalProvider 로컬 AI 공급자(예: Ollama)에 한해 loopback 허용 요청 여부
     */
    public URI validateUri(URI uri, boolean allowLoopbackForLocalProvider) {
        if (uri == null) {
            throw new IllegalArgumentException("outbound uri must not be null");
        }
        String scheme = uri.getScheme();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("outbound uri scheme is not allowed");
        }
        if (uri.getRawUserInfo() != null && !uri.getRawUserInfo().isBlank()) {
            throw new IllegalArgumentException("outbound uri must not embed credentials");
        }
        String rawHost = uri.getHost();
        if (rawHost == null || rawHost.isBlank()) {
            throw new IllegalArgumentException("outbound uri host is required");
        }
        if (testBypassEnabled) {
            return uri;
        }

        String host = stripIpv6Brackets(rawHost).toLowerCase(Locale.ROOT);
        boolean allowLoopback = allowLocalModelHosts && allowLoopbackForLocalProvider;
        validateHostnameSuffix(host, allowLoopback);

        InetAddress[] addresses = resolveAddressesFailClosed(host);
        for (InetAddress address : addresses) {
            if (isBlockedAddress(address, allowLoopback)) {
                throw new IllegalArgumentException("outbound destination IP is not allowed: " + host);
            }
        }
        return uri;
    }

    public URI validateUri(URI uri) {
        return validateUri(uri, false);
    }

    private void validateHostnameSuffix(String host, boolean allowLoopback) {
        if ("localhost".equals(host) || host.endsWith(".localhost")) {
            if (!allowLoopback) {
                throw new IllegalArgumentException("localhost destination is not allowed");
            }
            return;
        }
        if (host.endsWith(".internal") || host.endsWith(".local") || host.endsWith(".arpa")) {
            throw new IllegalArgumentException("internal hostname destination is not allowed");
        }
    }

    private InetAddress[] resolveAddressesFailClosed(String host) {
        try {
            InetAddress[] resolved = hostResolver.resolve(host);
            if (resolved == null || resolved.length == 0) {
                throw new IllegalArgumentException("outbound host resolution returned no addresses");
            }
            return resolved;
        } catch (UnknownHostException exception) {
            throw new IllegalArgumentException("outbound host resolution failed", exception);
        }
    }

    private static boolean isBlockedAddress(InetAddress address, boolean allowLoopback) {
        if (address.isAnyLocalAddress()
                || address.isMulticastAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()) {
            return true;
        }
        if (address.isLoopbackAddress()) {
            return !allowLoopback;
        }
        byte[] bytes = address.getAddress();
        if (bytes.length == IPV4_LENGTH) {
            return isBlockedIpv4(packIpv4(bytes, 0), allowLoopback);
        }
        if (bytes.length == IPV6_LENGTH) {
            return isBlockedIpv6(bytes, allowLoopback);
        }
        return true;
    }

    private static boolean isBlockedIpv4(int ip, boolean allowLoopback) {
        if ((ip & 0xFF000000) == 0x00000000) {
            return true; // 0.0.0.0/8
        }
        if ((ip & 0xFF000000) == 0x0A000000) {
            return true; // 10.0.0.0/8 (RFC 1918)
        }
        if ((ip & 0xFFC00000) == 0x64400000) {
            return true; // 100.64.0.0/10 (RFC 6598 CGNAT & Alibaba metadata 100.100.100.200)
        }
        if ((ip & 0xFF000000) == 0x7F000000) {
            return !allowLoopback; // 127.0.0.0/8 loopback
        }
        if ((ip & 0xFFFF0000) == 0xA9FE0000) {
            return true; // 169.254.0.0/16 link-local & cloud metadata
        }
        if ((ip & 0xFFF00000) == 0xAC100000) {
            return true; // 172.16.0.0/12 (RFC 1918)
        }
        if ((ip & 0xFFFFFF00) == 0xC0000000) {
            return true; // 192.0.0.0/24 IETF protocol assignments
        }
        if ((ip & 0xFFFFFF00) == 0xC0000200) {
            return true; // 192.0.2.0/24 TEST-NET-1
        }
        if ((ip & 0xFFFF0000) == 0xC0A80000) {
            return true; // 192.168.0.0/16 (RFC 1918)
        }
        if ((ip & 0xFFFE0000) == 0xC6120000) {
            return true; // 198.18.0.0/15 benchmarking
        }
        if ((ip & 0xFFFFFF00) == 0xC6336400) {
            return true; // 198.51.100.0/24 TEST-NET-2
        }
        if ((ip & 0xFFFFFF00) == 0xCB007100) {
            return true; // 203.0.113.0/24 TEST-NET-3
        }
        if ((ip & 0xF0000000) == 0xE0000000) {
            return true; // 224.0.0.0/4 multicast
        }
        return (ip & 0xF0000000) == 0xF0000000; // 240.0.0.0/4 reserved & broadcast
    }

    private static boolean isBlockedIpv6(byte[] b, boolean allowLoopback) {
        if (isUnspecifiedIpv6(b)) {
            return true;
        }
        if (isLoopbackIpv6(b)) {
            return !allowLoopback;
        }
        int b0 = b[0] & 0xFF;
        int b1 = b[1] & 0xFF;
        if ((b0 & 0xFE) == 0xFC) {
            return true; // fc00::/7 ULA (includes fd00:ec2::254 AWS IPv6 metadata)
        }
        if (b0 == 0xFE && (b1 & 0xC0) == 0x80) {
            return true; // fe80::/10 link-local
        }
        if (b0 == 0xFE && (b1 & 0xC0) == 0xC0) {
            return true; // fec0::/10 deprecated site-local
        }
        if (b0 == 0xFF) {
            return true; // ff00::/8 multicast
        }
        if (isIpv4MappedOrCompatibleIpv6(b)) {
            return isBlockedIpv4(packIpv4(b, 12), allowLoopback);
        }
        if (isNat64WellKnownPrefix(b) || isNat64LocalUsePrefix(b)) {
            return isBlockedIpv4(packIpv4(b, 12), allowLoopback);
        }
        if (b0 == 0x20 && b1 == 0x02) {
            return isBlockedIpv4(packIpv4(b, 2), allowLoopback); // 2002::/16 6to4
        }
        if (b0 == 0x20 && b1 == 0x01 && (b[2] & 0xFF) == 0x0D && (b[3] & 0xFF) == 0xB8) {
            return true; // 2001:db8::/32 documentation
        }
        return b0 == 0x01 && b1 == 0x00 && hasZeroPrefix(b, 2, 8); // 0100::/64 discard prefix
    }

    private static boolean isUnspecifiedIpv6(byte[] b) {
        return hasZeroPrefix(b, 0, IPV6_LENGTH);
    }

    private static boolean isLoopbackIpv6(byte[] b) {
        return hasZeroPrefix(b, 0, 15) && b[15] == 1;
    }

    private static boolean isIpv4MappedOrCompatibleIpv6(byte[] b) {
        if (!hasZeroPrefix(b, 0, 10)) {
            return false;
        }
        int s10 = b[10] & 0xFF;
        int s11 = b[11] & 0xFF;
        return (s10 == 0xFF && s11 == 0xFF) || (s10 == 0x00 && s11 == 0x00);
    }

    private static boolean isNat64WellKnownPrefix(byte[] b) {
        return (b[0] & 0xFF) == 0x00
                && (b[1] & 0xFF) == 0x64
                && (b[2] & 0xFF) == 0xFF
                && (b[3] & 0xFF) == 0x9B
                && hasZeroPrefix(b, 4, 12);
    }

    private static boolean isNat64LocalUsePrefix(byte[] b) {
        return (b[0] & 0xFF) == 0x00
                && (b[1] & 0xFF) == 0x64
                && (b[2] & 0xFF) == 0xFF
                && (b[3] & 0xFF) == 0x9B
                && (b[4] & 0xFF) == 0x00
                && (b[5] & 0xFF) == 0x01;
    }

    private static boolean hasZeroPrefix(byte[] b, int startInclusive, int endExclusive) {
        for (int i = startInclusive; i < endExclusive; i++) {
            if (b[i] != 0) {
                return false;
            }
        }
        return true;
    }

    private static int packIpv4(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24)
                | ((bytes[offset + 1] & 0xFF) << 16)
                | ((bytes[offset + 2] & 0xFF) << 8)
                | (bytes[offset + 3] & 0xFF);
    }

    private static String stripIpv6Brackets(String host) {
        if (host.startsWith("[") && host.endsWith("]") && host.length() > 2) {
            return host.substring(1, host.length() - 1);
        }
        return host;
    }

    @FunctionalInterface
    interface HostResolver {
        InetAddress[] resolve(String host) throws UnknownHostException;
    }
}
