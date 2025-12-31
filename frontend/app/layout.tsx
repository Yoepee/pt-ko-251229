import { Footer } from "@/components/Footer";
import { Header } from "@/components/Header";
import { theme } from "@/theme";
import { ColorSchemeScript, MantineProvider, mantineHtmlProps } from "@mantine/core";
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
        <MantineProvider theme={theme}>
          <Header />
            <main className="min-h-screen">
              {children}
            </main>
          <Footer />
        </MantineProvider>
      </body>
    </html>
  );
}
