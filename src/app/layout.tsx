import "./globals.css";
import Script from "next/script";

export const metadata = {
  title: "PRE(D)ATA",
  description: "PRE(D)ATA frontend",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body>
        {children}
        <Script src="https://accounts.google.com/gsi/client" strategy="afterInteractive" />
      </body>
    </html>
  );
}
