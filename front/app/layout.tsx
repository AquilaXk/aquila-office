import type { Metadata, Viewport } from "next";
import {
  APPLE_TOUCH_ICON_PATH,
  FAVICON_PATH,
  REPRESENTATIVE_IMAGE_PATHS,
  SERVICE_DESCRIPTION,
  SERVICE_NAME,
} from "@/lib/branding";
import DesktopWindowHeader from "@/components/desktop/DesktopWindowHeader";
import Providers from "./providers";
import "./globals.css";

export const metadata: Metadata = {
  applicationName: SERVICE_NAME,
  metadataBase: new URL(process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000"),
  title: SERVICE_NAME,
  description: SERVICE_DESCRIPTION,
  openGraph: {
    title: SERVICE_NAME,
    description: SERVICE_DESCRIPTION,
    siteName: SERVICE_NAME,
    images: [...REPRESENTATIVE_IMAGE_PATHS],
  },
  icons: {
    icon: FAVICON_PATH,
    apple: APPLE_TOUCH_ICON_PATH,
  },
};

export const viewport: Viewport = {
  colorScheme: "dark",
  themeColor: "#101214",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko">
      <body>
        <DesktopWindowHeader />
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
