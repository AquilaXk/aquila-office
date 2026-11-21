import type { NextConfig } from "next";

const isDesktop = process.env.BUILD_TARGET === "desktop" || Boolean(process.env.TAURI_ENV_PLATFORM);

const nextConfig: NextConfig = isDesktop
  ? {
      output: "export",
      trailingSlash: true,
      images: { unoptimized: true },
    }
  : {
      output: "standalone",
    };

export default nextConfig;
