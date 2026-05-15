import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Aquila Office",
  description: "Aquila Office frontend",
  icons: {
    icon: "/favicon.svg",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko">
      <body>{children}</body>
    </html>
  );
}
