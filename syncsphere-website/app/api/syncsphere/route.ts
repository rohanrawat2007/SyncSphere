import { createClient } from '@supabase/supabase-js';
import { createHash, createHmac, timingSafeEqual } from 'node:crypto';
import { NextResponse } from 'next/server';

export const runtime = 'nodejs';

function getSupabase() {
  const url = process.env.SUPABASE_URL;
  const serviceKey = process.env.SUPABASE_SERVICE_ROLE_KEY;
  if (!url || !serviceKey) throw new Error('Supabase server configuration is missing.');
  return createClient(url, serviceKey, { auth: { persistSession: false, autoRefreshToken: false } });
}

function jsonError(message: string, status = 400) {
  return NextResponse.json({ error: message }, { status });
}

function publicUser(user: Record<string, unknown>) {
  return {
    id: user.id,
    username: user.username,
    email: user.email,
    google_id: user.google_id,
    auth_provider: user.auth_provider,
    role: user.role,
    status: user.status,
    created_at: user.created_at
  };
}

function hashPassword(password: string) {
  return createHash('sha256').update(password, 'utf8').digest('hex');
}

function signSession(userId: number) {
  const payload = Buffer.from(JSON.stringify({ userId, exp: Date.now() + 1000 * 60 * 60 * 24 * 30 })).toString('base64url');
  const secret = process.env.SYNCSPHERE_SESSION_SECRET;
  if (!secret) throw new Error('SYNCSPHERE_SESSION_SECRET is not configured.');
  const signature = createHmac('sha256', secret).update(payload).digest('base64url');
  return `${payload}.${signature}`;
}

async function authenticatedUser(request: Request) {
  const supabase = getSupabase();
  const token = request.headers.get('authorization')?.replace(/^Bearer\s+/i, '');
  const secret = process.env.SYNCSPHERE_SESSION_SECRET;
  if (!token || !secret) return null;
  const [payload, signature] = token.split('.');
  if (!payload || !signature) return null;
  const expected = createHmac('sha256', secret).update(payload).digest('base64url');
  if (signature.length !== expected.length || !timingSafeEqual(Buffer.from(signature), Buffer.from(expected))) return null;
  const session = JSON.parse(Buffer.from(payload, 'base64url').toString('utf8')) as { userId: number; exp: number };
  if (!session.userId || session.exp < Date.now()) return null;
  const { data } = await supabase.from('users').select('*').eq('id', session.userId).single();
  return data;
}

async function findUsers(ids: number[]) {
  const supabase = getSupabase();
  if (!ids.length) return new Map<number, string>();
  const { data } = await supabase.from('users').select('id, username').in('id', ids);
  return new Map((data ?? []).map((user) => [user.id as number, user.username as string]));
}

export async function POST(request: Request) {
  try {
    const supabase = getSupabase();
    const body = await request.json() as Record<string, unknown>;
    const action = String(body.action ?? '');

    if (action === 'register') {
      const username = String(body.username ?? '').trim();
      const password = String(body.password ?? '');
      if (!/^[A-Za-z0-9_]{3,50}$/.test(username) || password.length < 8) return jsonError('Invalid username or password.');
      const { data: existing } = await supabase.from('users').select('id').eq('username', username).maybeSingle();
      if (existing) return jsonError('Username is already in use.', 409);
      const { data, error } = await supabase.from('users').insert({ username, password_hash: hashPassword(password), role: 'USER', status: 'OFFLINE' }).select('*').single();
      if (error) return jsonError('Account could not be created.', 500);
      return NextResponse.json({ user: publicUser(data), token: signSession(data.id) });
    }

    if (action === 'login') {
      const username = String(body.username ?? '').trim();
      const passwordHash = hashPassword(String(body.password ?? ''));
      const { data, error } = await supabase.from('users').select('*').eq('username', username).eq('password_hash', passwordHash).maybeSingle();
      if (error || !data) return jsonError('Invalid username or password.', 401);
      await supabase.from('users').update({ status: 'ONLINE' }).eq('id', data.id);
      data.status = 'ONLINE';
      return NextResponse.json({ user: publicUser(data), token: signSession(data.id) });
    }

    const user = await authenticatedUser(request);
    if (!user) return jsonError('Authentication required.', 401);

    if (action === 'logout') {
      await supabase.from('users').update({ status: 'OFFLINE' }).eq('id', user.id);
      return NextResponse.json({ ok: true });
    }

    if (action === 'searchUsers') {
      const query = String(body.query ?? '').trim();
      const { data, error } = await supabase.from('users').select('id, username, status, role').neq('id', user.id).ilike('username', `%${query}%`).order('username').limit(20);
      if (error) return jsonError('User search failed.', 500);
      return NextResponse.json({ users: data ?? [] });
    }

    if (action === 'sendFriendRequest') {
      const receiverId = Number(body.receiverId);
      if (!receiverId || receiverId === user.id) return jsonError('Invalid friend request target.');
      const { error } = await supabase.from('friend_requests').insert({ sender_id: user.id, receiver_id: receiverId, status: 'PENDING' });
      if (error) return jsonError('Friend request could not be sent.', 409);
      return NextResponse.json({ ok: true });
    }

    if (action === 'friends') {
      const { data: rows, error } = await supabase.from('friends').select('user_id, friend_id, created_at').or(`user_id.eq.${user.id},friend_id.eq.${user.id}`).order('created_at');
      if (error) return jsonError('Friends could not be loaded.', 500);
      const ids = (rows ?? []).map((row) => row.user_id === user.id ? row.friend_id : row.user_id);
      const { data: people } = await supabase.from('users').select('id, username, email, auth_provider, role, status, created_at').in('id', ids);
      const byId = new Map((people ?? []).map((person) => [person.id, person]));
      return NextResponse.json({ friends: (rows ?? []).map((row) => ({ user: byId.get(row.user_id === user.id ? row.friend_id : row.user_id), createdAt: row.created_at })) });
    }

    if (action === 'incomingRequests') {
      const { data: rows, error } = await supabase.from('friend_requests').select('*').eq('receiver_id', user.id).eq('status', 'PENDING').order('created_at', { ascending: false });
      if (error) return jsonError('Friend requests could not be loaded.', 500);
      const ids = (rows ?? []).map((row) => row.sender_id);
      const { data: people } = await supabase.from('users').select('id, username, email, auth_provider, role, status, created_at').in('id', ids);
      const byId = new Map((people ?? []).map((person) => [person.id, person]));
      return NextResponse.json({ requests: (rows ?? []).map((row) => ({ ...row, sender: byId.get(row.sender_id), receiver: publicUser(user) })) });
    }

    if (action === 'respondFriendRequest') {
      const requestId = Number(body.requestId);
      const status = String(body.status ?? '');
      if (!['ACCEPTED', 'DECLINED'].includes(status)) return jsonError('Invalid request response.');
      const { data: request } = await supabase.from('friend_requests').select('*').eq('id', requestId).eq('receiver_id', user.id).eq('status', 'PENDING').single();
      if (!request) return jsonError('Friend request is unavailable.', 404);
      await supabase.from('friend_requests').update({ status, responded_at: new Date().toISOString() }).eq('id', requestId);
      if (status === 'ACCEPTED') {
        const low = Math.min(user.id, request.sender_id);
        const high = Math.max(user.id, request.sender_id);
        await supabase.from('friends').upsert({ user_id: low, friend_id: high }, { onConflict: 'user_id,friend_id' });
      }
      return NextResponse.json({ ok: true });
    }

    if (action === 'publicMessages' || action === 'privateMessages') {
      let query = supabase.from('messages').select('*').eq('is_deleted', false).order('created_at', { ascending: true }).limit(200);
      if (action === 'publicMessages') query = query.eq('message_type', 'PUBLIC');
      else {
        const otherId = Number(body.otherUserId);
        query = query.eq('message_type', 'PRIVATE').or(`and(sender_id.eq.${user.id},receiver_id.eq.${otherId}),and(sender_id.eq.${otherId},receiver_id.eq.${user.id})`);
      }
      const { data, error } = await query;
      if (error) return jsonError('Messages could not be loaded.', 500);
      const names = await findUsers([...(data ?? []).map((message) => message.sender_id), ...(data ?? []).map((message) => message.receiver_id).filter(Boolean)]);
      return NextResponse.json({ messages: (data ?? []).map((message) => ({ ...message, sender_username: names.get(message.sender_id) ?? 'Unknown', receiver_username: names.get(message.receiver_id) ?? null })) });
    }

    if (action === 'sendMessage') {
      const privateMessage = Boolean(body.receiverId);
      const content = String(body.content ?? '').trim();
      if (!content || content.length > 2000) return jsonError('Message content is invalid.');
      const row = { sender_id: user.id, receiver_id: privateMessage ? Number(body.receiverId) : null, message: content, message_type: privateMessage ? 'PRIVATE' : 'PUBLIC', reply_to_message_id: body.replyToMessageId ? Number(body.replyToMessageId) : null };
      const { data, error } = await supabase.from('messages').insert(row).select('*').single();
      if (error) return jsonError('Message could not be sent.', 500);
      return NextResponse.json({ message: data });
    }

    if (action === 'toggleReaction') {
      const messageId = Number(body.messageId);
      const reaction = String(body.reaction ?? '').trim();
      const { data: existing } = await supabase.from('message_reactions').select('id').eq('message_id', messageId).eq('user_id', user.id).eq('reaction', reaction).maybeSingle();
      if (existing) await supabase.from('message_reactions').delete().eq('id', existing.id);
      else await supabase.from('message_reactions').insert({ message_id: messageId, user_id: user.id, reaction });
      return NextResponse.json({ ok: true });
    }

    if (action === 'markRead') {
      const { error } = await supabase.from('message_reads').upsert({ message_id: Number(body.messageId), user_id: user.id, read_at: new Date().toISOString() }, { onConflict: 'message_id,user_id' });
      if (error) return jsonError('Read receipt could not be saved.', 500);
      return NextResponse.json({ ok: true });
    }

    if (action === 'togglePin') {
      if (user.role !== 'MODERATOR') return jsonError('Moderator access required.', 403);
      const messageId = Number(body.messageId);
      const { data: existing } = await supabase.from('pinned_messages').select('message_id').eq('message_id', messageId).maybeSingle();
      if (existing) await supabase.from('pinned_messages').delete().eq('message_id', messageId);
      else await supabase.from('pinned_messages').insert({ message_id: messageId, pinned_by: user.id });
      return NextResponse.json({ ok: true });
    }

    if (action === 'pinnedMessages') {
      const { data, error } = await supabase.from('pinned_messages').select('message_id').order('pinned_at', { ascending: false });
      if (error) return jsonError('Pinned messages could not be loaded.', 500);
      return NextResponse.json({ ids: (data ?? []).map((row) => row.message_id) });
    }

    if (action === 'notifications') {
      const { data, error } = await supabase.from('notifications').select('*').eq('user_id', user.id).eq('is_read', false).order('created_at', { ascending: false }).limit(50);
      if (error) return jsonError('Notifications could not be loaded.', 500);
      return NextResponse.json({ notifications: data ?? [] });
    }

    if (action === 'markNotificationsRead') {
      await supabase.from('notifications').update({ is_read: true }).eq('user_id', user.id).eq('is_read', false);
      return NextResponse.json({ ok: true });
    }

    if (action === 'editMessage' || action === 'deleteMessage') {
      const messageId = Number(body.messageId);
      const { data: message } = await supabase.from('messages').select('sender_id').eq('id', messageId).single();
      if (!message || (message.sender_id !== user.id && user.role !== 'MODERATOR')) return jsonError('Message permission denied.', 403);
      if (action === 'deleteMessage') await supabase.from('messages').update({ is_deleted: true }).eq('id', messageId);
      else await supabase.from('messages').update({ message: String(body.content ?? '').trim(), is_edited: true }).eq('id', messageId);
      return NextResponse.json({ ok: true });
    }

    if (action === 'muteUser' || action === 'unmuteUser') {
      if (user.role !== 'MODERATOR') return jsonError('Moderator access required.', 403);
      const targetId = Number(body.targetUserId);
      if (action === 'unmuteUser') await supabase.from('mutes').delete().eq('user_id', targetId);
      else await supabase.from('mutes').upsert({ user_id: targetId, muted_by: user.id, expires_at: new Date(Date.now() + Number(body.durationSeconds ?? 300) * 1000).toISOString(), reason: String(body.reason ?? 'Moderator mute') }, { onConflict: 'user_id' });
      return NextResponse.json({ ok: true });
    }

    return jsonError('Unknown action.');
  } catch (error) {
    console.error('SyncSphere API error', error);
    return jsonError('Unexpected server error.', 500);
  }
}
