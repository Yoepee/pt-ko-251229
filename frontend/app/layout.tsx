import { Header } from "@/components/Header"; // Keeping Header for User Profile part only? Or merge.
import { Navbar } from "@/components/Navbar";
import { Providers } from "@/components/Providers";
import { ColorSchemeScript } from "@mantine/core";
import "@mantine/core/styles.css";
// Actually, let's keep Header for top search/profile but make it offset.
import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "VoteBoard",
  description: "Real-time voting platform",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" suppressHydrationWarning>
      <head>
        <ColorSchemeScript defaultColorScheme="dark" />
      </head>
      <body className="antialiased bg-[#0A0A0B] text-gray-200">
        <Providers>
            {/* Sidebar (Fixed Left) */}
            <Navbar />
            
            {/* Main Content Area (Offset Right) */}
            <div className="pl-[240px] flex flex-col min-h-screen">
                {/* Top Header (User Profile, etc) - Reusing Header but styling needs tweak to remove logo */}
                <Header /> 
                
                <main className="flex-1 p-6 pt-[80px]"> 
                    {children}
                </main>
            </div>
        </Providers>
      </body>
    </html>
  );
}
