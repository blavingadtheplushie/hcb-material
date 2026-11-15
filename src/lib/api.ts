import { useCallback, useEffect, useRef, useState } from "react";
import { API_BASE, getTokens, refresh } from "./auth";

export class ApiError extends Error { constructor(public status: number, message: string, public body?: unknown) { super(message); } }

async function validToken() {
  let t = await getTokens();
  if (t?.expires_in && (t.created_at + t.expires_in - 60) * 1000 < Date.now()) t = await refresh();
  return t?.access_token;
}

export async function api<T = any>(path: string, init: RequestInit & { json?: unknown } = {}, retry = true): Promise<T> {
  const token = await validToken();
  const headers = new Headers(init.headers);
  if (token) headers.set("Authorization", `Bearer ${token}`);
  let body = init.body;
  if (init.json !== undefined) { headers.set("Content-Type", "application/json"); body = JSON.stringify(init.json); }
  headers.set("Accept", "application/json");
  const res = await fetch(`${API_BASE}/${path.replace(/^\//, "")}`, { ...init, headers, body });
  if (res.status === 401 && retry && token) {
    const nt = await refresh();
    if (nt) return api<T>(path, init, false);
  }
  const text = await res.text();
  let data: any = null;
  try { data = text ? JSON.parse(text) : null; } catch { data = text; }
  if (!res.ok) {
    const msg = (data && (data.error_description || data.message || (Array.isArray(data.messages) && data.messages.join(", ")) || data.error)) || `HTTP ${res.status}`;
    throw new ApiError(res.status, String(msg), data);
  }
  return data as T;
}

// tiny SWR: in-memory cache + revalidate
const cache = new Map<string, unknown>();
const listeners = new Map<string, Set<() => void>>();
export function mutate(key: string, data?: unknown) {
  if (data !== undefined) cache.set(key, data); else cache.delete(key);
  listeners.get(key)?.forEach((l) => l());
}
export function clearCache() { cache.clear(); }

export function useApi<T>(key: string | null) {
  const [data, setData] = useState<T | undefined>(key ? (cache.get(key) as T) : undefined);
  const [error, setError] = useState<Error | null>(null);
  const [loading, setLoading] = useState(!!key && !cache.has(key));
  const keyRef = useRef(key);
  keyRef.current = key;
  const load = useCallback(async () => {
    if (!key) return;
    setLoading(true);
    try {
      const d = await api<T>(key);
      cache.set(key, d);
      if (keyRef.current === key) { setData(d); setError(null); }
    } catch (e) { if (keyRef.current === key) setError(e as Error); }
    finally { if (keyRef.current === key) setLoading(false); }
  }, [key]);
  useEffect(() => {
    if (!key) return;
    setData(cache.get(key) as T);
    load();
    const l = () => { if (cache.has(key)) setData(cache.get(key) as T); else load(); };
    if (!listeners.has(key)) listeners.set(key, new Set());
    listeners.get(key)!.add(l);
    return () => { listeners.get(key)?.delete(l); };
  }, [key, load]);
  return { data, error, loading, reload: load, setData: (d: T) => { if (key) mutate(key, d); } };
}
