import { Capacitor } from "@capacitor/core";
import { Preferences } from "@capacitor/preferences";
import { Browser } from "@capacitor/browser";

export const API_BASE = "https://hcb.hackclub.com/api/v4";
// Public OAuth client of the official HCB mobile app (hackclub/hcb-mobile .env.example).
export const CLIENT_ID = "yt8JHmPDmmYYLUmoEiGtocYwg5fSOGCrcIY3G-vkMRs";
// Same redirect as the official app: expo makeRedirectUri({ scheme: "hcb" }) => "hcb://"
export const REDIRECT_URI = "hcb://";
const SCOPES = "read write";

export interface Tokens { access_token: string; refresh_token?: string; expires_in?: number; created_at: number }

const b64url = (buf: ArrayBuffer | Uint8Array) =>
  btoa(String.fromCharCode(...new Uint8Array(buf as ArrayBuffer))).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
const random = (n = 32) => b64url(crypto.getRandomValues(new Uint8Array(n)));

let cached: Tokens | null | undefined;

export async function getTokens(): Promise<Tokens | null> {
  if (cached !== undefined) return cached;
  const { value } = await Preferences.get({ key: "tokens" });
  cached = value ? JSON.parse(value) : null;
  return cached!;
}
export async function setTokens(t: Tokens | null) {
  cached = t;
  if (t) await Preferences.set({ key: "tokens", value: JSON.stringify(t) });
  else await Preferences.remove({ key: "tokens" });
  window.dispatchEvent(new Event("hcb-auth"));
}

export async function startLogin(signup = false) {
  const verifier = random(48);
  const challenge = b64url(await crypto.subtle.digest("SHA-256", new TextEncoder().encode(verifier)));
  const state = random(16);
  await Preferences.set({ key: "pkce", value: JSON.stringify({ verifier, state }) });
  const url = new URL(API_BASE + "/oauth/authorize");
  Object.entries({
    client_id: CLIENT_ID, redirect_uri: REDIRECT_URI, response_type: "code", scope: SCOPES,
    code_challenge: challenge, code_challenge_method: "S256", state,
    no_app_shell: "true", signup: String(signup),
    theme: matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light",
  }).forEach(([k, v]) => url.searchParams.set(k, v));
  if (Capacitor.isNativePlatform()) await Browser.open({ url: url.toString() });
  else window.open(url.toString(), "_blank");
}

/** Handles hcb://?code=…&state=… (deep link) or a pasted redirect URL. */
export async function finishLogin(redirect: string) {
  const q = redirect.includes("?") ? redirect.slice(redirect.indexOf("?") + 1) : redirect.includes("=") ? redirect : `code=${redirect}`;
  const p = new URLSearchParams(q.split("#")[0]);
  if (p.get("error")) throw new Error(p.get("error_description") || p.get("error")!);
  const code = p.get("code");
  if (!code) throw new Error("No authorization code in the redirect.");
  const { value } = await Preferences.get({ key: "pkce" });
  if (!value) throw new Error("Login session expired, please try again.");
  const { verifier, state } = JSON.parse(value);
  if (p.get("state") && p.get("state") !== state) throw new Error("State mismatch, please try again.");
  const res = await fetch(API_BASE + "/oauth/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ grant_type: "authorization_code", code, client_id: CLIENT_ID, redirect_uri: REDIRECT_URI, code_verifier: verifier }),
  });
  const json = await res.json();
  if (!res.ok) throw new Error(json.error_description || json.error || "Token exchange failed");
  await Preferences.remove({ key: "pkce" });
  if (Capacitor.isNativePlatform()) Browser.close().catch(() => {});
  await setTokens({ ...json, created_at: json.created_at ?? Math.floor(Date.now() / 1000) });
}

let refreshing: Promise<Tokens | null> | null = null;
export function refresh(): Promise<Tokens | null> {
  // Refresh tokens rotate: make sure only one refresh runs at a time.
  if (refreshing) return refreshing;
  refreshing = (async () => {
    const t = await getTokens();
    if (!t?.refresh_token) { await setTokens(null); return null; }
    const res = await fetch(API_BASE + "/oauth/token", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({ grant_type: "refresh_token", refresh_token: t.refresh_token, client_id: CLIENT_ID }),
    });
    if (!res.ok) { if (res.status === 400 || res.status === 401) await setTokens(null); return null; }
    const json = await res.json();
    const nt = { ...json, created_at: json.created_at ?? Math.floor(Date.now() / 1000) };
    await setTokens(nt);
    return nt;
  })().finally(() => { refreshing = null; });
  return refreshing;
}

export async function logout() {
  const t = await getTokens();
  if (t) fetch(API_BASE + "/oauth/revoke", {
    method: "POST", headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ token: t.access_token, client_id: CLIENT_ID }),
  }).catch(() => {});
  await setTokens(null);
}
