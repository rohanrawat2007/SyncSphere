import { createClient } from '@supabase/supabase-js';
import { createHash, createHmac, timingSafeEqual } from 'node:crypto';
import { NextResponse } from 'next/server';

export const runtime = 'nodejs';

function sessionToken(userId: number) {
  const payload = Buffer.from(JSON.stringify({ userId, exp: Date.now() + 1000 * 60 * 60 * 24 * 30 })).toString('base64url');
  const secret = process.env.SYNCSPHERE_SESSION_SECRET;
  if (!secret) throw new Error('Session secret is not configured.');
  return `${payload}.${createHmac('sha256', secret).update(payload).digest('base64url')}`;
}

function safeUser(user: Record<string, unknown>) {
  return { id: user.id, username: user.username, email: user.email, google_id: user.google_id, auth_provider: user.auth_provider, role: user.role, status: user.status, created_at: user.created_at };
}

export async function GET(request: Request) {
  const params = new URL(request.url).searchParams;
  const code = params.get('code');
  const state = params.get('state') ?? '';
  const [encoded, signature] = state.split('.');
  const secret = process.env.SYNCSPHERE_SESSION_SECRET;
  if (!code || !encoded || !signature || !secret) return new NextResponse('Google login could not be verified.', { status: 400 });
  const expected = createHmac('sha256', secret).update(encoded).digest('base64url');
  if (signature.length !== expected.length || !timingSafeEqual(Buffer.from(signature), Buffer.from(expected))) return new NextResponse('Invalid OAuth state.', { status: 400 });
  const stateData = JSON.parse(Buffer.from(encoded, 'base64url').toString('utf8')) as { redirectUri: string; exp: number };
  if (stateData.exp < Date.now()) return new NextResponse('OAuth session expired.', { status: 400 });

  const callback = `${new URL(request.url).origin}/api/syncsphere/google/callback`;
  const tokenResponse = await fetch('https://oauth2.googleapis.com/token', { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: new URLSearchParams({ code, client_id: process.env.GOOGLE_CLIENT_ID ?? '', client_secret: process.env.GOOGLE_CLIENT_SECRET ?? '', redirect_uri: callback, grant_type: 'authorization_code' }) });
  const token = await tokenResponse.json() as { access_token?: string };
  if (!token.access_token) return new NextResponse('Google token exchange failed.', { status: 502 });
  const identityResponse = await fetch('https://openidconnect.googleapis.com/v1/userinfo', { headers: { Authorization: `Bearer ${token.access_token}` } });
  const identity = await identityResponse.json() as { sub?: string; email?: string; name?: string };
  if (!identity.sub || !identity.email) return new NextResponse('Google identity is incomplete.', { status: 502 });

  const supabase = createClient(process.env.SUPABASE_URL ?? '', process.env.SUPABASE_SERVICE_ROLE_KEY ?? '', { auth: { persistSession: false } });
  let { data: user } = await supabase.from('users').select('*').eq('google_id', identity.sub).maybeSingle();
  if (!user) {
    const byEmail = await supabase.from('users').select('*').eq('email', identity.email).maybeSingle();
    user = byEmail.data;
  }
  if (!user) {
    const base = (identity.name ?? identity.email.split('@')[0]).replace(/[^A-Za-z0-9_]+/g, '_').replace(/^_+|_+$/g, '').slice(0, 42) || 'google_user';
    const { data: created, error } = await supabase.from('users').insert({ username: `${base}_${createHash('sha256').update(identity.sub).digest('hex').slice(0, 6)}`, email: identity.email, google_id: identity.sub, auth_provider: 'GOOGLE', role: 'USER', status: 'ONLINE' }).select('*').single();
    if (error) {
      console.error('Supabase user insert error:', error);
      return new NextResponse('Google account could not be created.', { status: 500 });
    }
    user = created;
  } else {
    await supabase.from('users').update({ google_id: identity.sub, auth_provider: 'GOOGLE', status: 'ONLINE' }).eq('id', user.id);
    user.status = 'ONLINE';
  }

  const redirect = new URL(stateData.redirectUri);
  redirect.searchParams.set('token', sessionToken(user.id));
  redirect.searchParams.set('user', Buffer.from(JSON.stringify(safeUser(user))).toString('base64url'));
  return NextResponse.redirect(redirect);
}
