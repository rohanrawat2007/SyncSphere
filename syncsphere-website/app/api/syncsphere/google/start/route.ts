import { createHmac } from 'node:crypto';
import { NextResponse } from 'next/server';

export const runtime = 'nodejs';

function sign(value: string) {
  const secret = process.env.SYNCSPHERE_SESSION_SECRET;
  if (!secret) throw new Error('Session secret is not configured.');
  return createHmac('sha256', secret).update(value).digest('base64url');
}

export async function GET(request: Request) {
  const redirectUri = new URL(request.url).searchParams.get('redirect_uri') ?? '';
  let callback: URL;
  try { callback = new URL(redirectUri); }
  catch { return NextResponse.json({ error: 'Invalid desktop callback.' }, { status: 400 }); }
  if (callback.protocol !== 'http:' || !['127.0.0.1', 'localhost'].includes(callback.hostname) || !callback.pathname.startsWith('/')) {
    return NextResponse.json({ error: 'Invalid desktop callback.' }, { status: 400 });
  }
  const clientId = process.env.GOOGLE_CLIENT_ID;
  const googleCallback = `${new URL(request.url).origin}/api/syncsphere/google/callback`;
  if (!clientId) return NextResponse.json({ error: 'Google OAuth is not configured.' }, { status: 503 });
  const payload = Buffer.from(JSON.stringify({ redirectUri, exp: Date.now() + 1000 * 60 * 10 })).toString('base64url');
  const state = `${payload}.${sign(payload)}`;
  const params = new URLSearchParams({ client_id: clientId, redirect_uri: googleCallback, response_type: 'code', scope: 'openid email profile', state, access_type: 'online', prompt: 'select_account' });
  return NextResponse.redirect(new URL(`https://accounts.google.com/o/oauth2/v2/auth?${params.toString()}`));
}
