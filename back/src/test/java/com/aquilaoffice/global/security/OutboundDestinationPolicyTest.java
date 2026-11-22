package com.aquilaoffice.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OutboundDestinationPolicyTest {

    private static OutboundDestinationPolicy policyWithDns(
            boolean allowLocalHosts,
            Map<String, InetAddress[]> records) {
        return new OutboundDestinationPolicy(allowLocalHosts, false, host -> {
            InetAddress[] found = records.get(host);
            if (found == null) {
                throw new UnknownHostException(host);
            }
            return found;
        });
    }

    @Test
    void allowsPublicHttpsDestination() throws Exception {
        OutboundDestinationPolicy policy = policyWithDns(false, Map.of(
                "api.openai.com", new InetAddress[] {InetAddress.getByName("104.18.6.192")}));

        URI validated = policy.validateUri(URI.create("https://api.openai.com/v1/models"));

        assertThat(validated.getHost()).isEqualTo("api.openai.com");
    }

    @Test
    void blocksLoopbackPrivateCgnatLinkLocalAndMetadataAddresses() throws Exception {
        OutboundDestinationPolicy policy = policyWithDns(false, Map.ofEntries(
                Map.entry("loop.example", new InetAddress[] {InetAddress.getByName("127.0.0.1")}),
                Map.entry("loop6.example", new InetAddress[] {InetAddress.getByName("::1")}),
                Map.entry("rfc1918-10.example", new InetAddress[] {InetAddress.getByName("10.0.0.5")}),
                Map.entry("rfc1918-172.example", new InetAddress[] {InetAddress.getByName("172.16.12.1")}),
                Map.entry("rfc1918-192.example", new InetAddress[] {InetAddress.getByName("192.168.1.10")}),
                Map.entry("cgnat.example", new InetAddress[] {InetAddress.getByName("100.64.0.1")}),
                Map.entry("ali-meta.example", new InetAddress[] {InetAddress.getByName("100.100.100.200")}),
                Map.entry("aws-meta.example", new InetAddress[] {InetAddress.getByName("169.254.169.254")}),
                Map.entry("ula6.example", new InetAddress[] {InetAddress.getByName("fd00:ec2::254")}),
                Map.entry("link6.example", new InetAddress[] {InetAddress.getByName("fe80::1")}),
                Map.entry("multicast.example", new InetAddress[] {InetAddress.getByName("224.0.0.1")}),
                Map.entry("nat64.example", new InetAddress[] {InetAddress.getByAddress(new byte[] {
                        0x00, 0x64, (byte) 0xFF, (byte) 0x9B, 0, 0, 0, 0, 0, 0, 0, 0,
                        (byte) 169, (byte) 254, (byte) 169, (byte) 254
                })}),
                Map.entry("nat64-local.example", new InetAddress[] {InetAddress.getByAddress(new byte[] {
                        0x00, 0x64, (byte) 0xFF, (byte) 0x9B, 0x00, 0x01, (byte) 0xA9, (byte) 0xFE,
                        0, (byte) 0xA9, (byte) 0xFE, 0, 0x08, 0x08, 0x08, 0x08
                })}),
                Map.entry("teredo.example", new InetAddress[] {InetAddress.getByAddress(new byte[] {
                        0x20, 0x01, 0x00, 0x00, 0x41, 0x36, (byte) 0xE3, 0x78,
                        (byte) 0x80, 0x00, 0x63, (byte) 0xBF, 0x3F, (byte) 0xFF, (byte) 0xFD, (byte) 0xD2
                })}),
                Map.entry("benchmark6.example", new InetAddress[] {InetAddress.getByAddress(new byte[] {
                        0x20, 0x01, 0x00, 0x02, 0x00, 0x00, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1
                })}),
                Map.entry("orchid6.example", new InetAddress[] {InetAddress.getByAddress(new byte[] {
                        0x20, 0x01, 0x00, 0x20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1
                })}),
                Map.entry("v4mapped.example", new InetAddress[] {InetAddress.getByAddress(new byte[] {
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0xFF, (byte) 0xFF, 127, 0, 0, 1
                })}),
                Map.entry("mixed.example", new InetAddress[] {
                        InetAddress.getByName("104.18.6.192"),
                        InetAddress.getByName("10.1.2.3")
                })));

        for (String host : new String[] {
                "loop.example", "loop6.example", "rfc1918-10.example", "rfc1918-172.example",
                "rfc1918-192.example", "cgnat.example", "ali-meta.example", "aws-meta.example",
                "ula6.example", "link6.example", "multicast.example", "nat64.example",
                "nat64-local.example", "teredo.example", "benchmark6.example", "orchid6.example",
                "v4mapped.example", "mixed.example"
        }) {
            assertThatThrownBy(() -> policy.validateUri(URI.create("https://" + host + "/v1")))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void blocksDnsFailureEmbeddedCredentialsAndInternalSuffixes() {
        OutboundDestinationPolicy policy = policyWithDns(true, Map.of());

        assertThatThrownBy(() -> policy.validateUri(URI.create("https://unknown.example/v1")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> policy.validateUri(URI.create("https://user:pass@104.18.6.192/v1")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> policy.validateUri(URI.create("http://metadata.google.internal/computeMetadata")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> policy.validateUri(URI.create("ftp://104.18.6.192/file")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void allowsLoopbackOnlyWhenLocalProviderAndPolicyFlagAreBothEnabled() throws Exception {
        OutboundDestinationPolicy enabled = policyWithDns(true, Map.of(
                "localhost", new InetAddress[] {InetAddress.getByName("127.0.0.1")},
                "private.example", new InetAddress[] {InetAddress.getByName("192.168.1.50")}));

        assertThat(enabled.validateUri(URI.create("http://localhost:11434/api/tags"), true).getPort())
                .isEqualTo(11434);
        assertThatThrownBy(() -> enabled.validateUri(URI.create("http://localhost:11434/api/tags"), false))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> enabled.validateUri(URI.create("http://private.example:11434/api/tags"), true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pinsResolvedAddressesToPreventSecondDnsLookup() throws Exception {
        InetAddress pinnedIp = InetAddress.getByName("104.18.6.192");
        OutboundDestinationPolicy policy = policyWithDns(false, Map.of(
                "api.openai.com", new InetAddress[] {pinnedIp}));

        OutboundDestinationPolicy.ValidatedDestination destination =
                policy.validateAndResolveUri(URI.create("https://api.openai.com/v1/models"), false);

        assertThat(OutboundDestinationPolicy.lookupPinnedHost(destination, "api.openai.com"))
                .containsExactly(pinnedIp);
        assertThat(OutboundDestinationPolicy.lookupPinnedHost(destination, "API.OPENAI.COM"))
                .containsExactly(pinnedIp);
        assertThatThrownBy(() -> OutboundDestinationPolicy.lookupPinnedHost(destination, "evil.example"))
                .isInstanceOf(UnknownHostException.class);
    }
}
