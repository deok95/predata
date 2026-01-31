'use client'

import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";
import { Web3Provider } from "@/lib/Web3Provider";
import '@/lib/localStorage-polyfill'; // localStorage polyfill

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

// 클라이언트 컴포넌트이므로 metadata를 직접 export할 수 없습니다
// 대신 Head를 사용하거나 별도의 서버 컴포넌트로 분리해야 합니다

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko">
      <head>
        <title>Predata - Web3 예측 시장</title>
        <meta name="description" content="투명하고 공정한 예측 시장 플랫폼" />
      </head>
      <body
        className={`${geistSans.variable} ${geistMono.variable} antialiased`}
      >
        <Web3Provider>
          {children}
        </Web3Provider>
      </body>
    </html>
  );
}
