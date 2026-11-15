import { useCallback, useEffect, useRef, useState } from "react";
import { api } from "./api";
import type { Paginated } from "./types";

/** Cursor pagination over HCB v4 endpoints (limit / after / has_more). */
export function useInfinite<T extends { id: string }>(base: string | null, pageSize = 30) {
  const [items, setItems] = useState<T[]>([]);
  const [hasMore, setHasMore] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const [total, setTotal] = useState<number | null>(null);
  const busy = useRef(false);
  const gen = useRef(0);

  const load = useCallback(async (reset = false) => {
    if (!base || busy.current) return;
    busy.current = true; setLoading(true);
    const g = gen.current;
    try {
      const sep = base.includes("?") ? "&" : "?";
      const last = reset ? undefined : items[items.length - 1];
      const r = await api<Paginated<T>>(`${base}${sep}limit=${pageSize}${last ? `&after=${encodeURIComponent(last.id)}` : ""}`);
      if (g !== gen.current) return;
      setItems((prev) => {
        const merged = reset ? r.data : [...prev, ...r.data];
        const seen = new Set<string>();
        return merged.filter((x) => (seen.has(x.id) ? false : (seen.add(x.id), true)));
      });
      setHasMore(!!r.has_more); setTotal(r.total_count ?? null); setError(null);
    } catch (e) { setError(e as Error); setHasMore(false); }
    finally { busy.current = false; setLoading(false); }
  }, [base, items, pageSize]);

  const reset = useCallback(() => { gen.current++; busy.current = false; setItems([]); setHasMore(true); setError(null); }, []);
  useEffect(() => { reset(); }, [base, reset]);
  useEffect(() => { if (base && items.length === 0 && hasMore && !busy.current) load(true); }, [base, items.length, hasMore, load]);

  return { items, hasMore, loading, error, total, loadMore: () => load(false), refresh: reset, setItems };
}

export function useSentinel(cb: () => void, enabled: boolean) {
  const ref = useRef<HTMLDivElement | null>(null);
  useEffect(() => {
    if (!enabled || !ref.current) return;
    const io = new IntersectionObserver((e) => { if (e[0].isIntersecting) cb(); }, { rootMargin: "400px" });
    io.observe(ref.current);
    return () => io.disconnect();
  }, [cb, enabled]);
  return ref;
}
