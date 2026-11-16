import type { Metadata } from "next";
import { Suspense } from "react";
import { Geist, Geist_Mono } from "next/font/google";
import Nav, { NavShell } from "@/components/Nav";
import "./globals.css";

const geistSans = Geist({ variable: "--font-geist-sans", subsets: ["latin"] });
const geistMono = Geist_Mono({ variable: "--font-geist-mono", subsets: ["latin"] });

export const metadata: Metadata = {
  title: "Transaction Demo",
  description:
    "Create a transaction and watch it move from PENDING to SUCCESS through a transactional outbox, SQS and AWS Lambda.",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="en" className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}>
      <body className="flex min-h-full flex-col font-sans">
        {/* usePathname suspends on routes with request-time params, so keep the shell prerendered. */}
        <Suspense fallback={<NavShell />}>
          <Nav />
        </Suspense>
        <main className="mx-auto w-full max-w-3xl flex-1 px-4 py-8 sm:px-6">{children}</main>
        <footer className="mx-auto w-full max-w-3xl px-4 pb-8 text-xs text-faint sm:px-6">
          Demo data only. Amounts are not charged and phone numbers are not contacted. Recent transactions are public with phone numbers masked.
        </footer>
      </body>
    </html>
  );
}
