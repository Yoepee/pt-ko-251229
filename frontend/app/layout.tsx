import { Footer } from "@/components/Footer";
import { Header } from "@/components/Header";
import { Providers } from "@/components/Providers";
import { theme } from "@/theme";
import { ColorSchemeScript, MantineProvider, mantineHtmlProps } from "@mantine/core";
import { Notifications } from "@mantine/notifications";
import "@mantine/notifications/styles.css";
import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "My Blog",
  description: "Apple-style blog built with Next.js and Mantine",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" {...mantineHtmlProps} suppressHydrationWarning>
      <head>
        <ColorSchemeScript />
      </head>
      <body className="antialiased bg-gray-50 text-gray-900">
        <Providers>
          <MantineProvider theme={theme}>
            <Notifications />
            <Header />
              <main className="min-h-screen">
                {children}
              </main>
            <Footer />
          </MantineProvider>
        </Providers>
      </body>
    </html>
  );
}
