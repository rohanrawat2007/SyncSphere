import './globals.css';
import type { Metadata } from 'next';

export const metadata: Metadata = {
  metadataBase: new URL('https://syncsphere.example.com'),
  title: {
    default: 'SyncSphere | Secure Multi-threaded Chat Room',
    template: '%s | SyncSphere'
  },
  description:
    'SyncSphere is a secure multi-threaded chat room application with MySQL persistence, optional Google OAuth, and a polished desktop experience.',
  openGraph: {
    title: 'SyncSphere',
    description: 'Secure multi-threaded chat room with live messaging and flexible authentication.',
    url: 'https://syncsphere.example.com',
    siteName: 'SyncSphere',
    type: 'website'
  },
  twitter: {
    card: 'summary_large_image',
    title: 'SyncSphere',
    description: 'Secure multi-threaded chat room with live messaging and flexible authentication.'
  },
  alternates: {
    canonical: '/'
  }
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
