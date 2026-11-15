import { useCallback, useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { Avatar, AvatarGroup, Box, Card, CardContent, Chip, CircularProgress, InputAdornment, List, Stack, Tab, Tabs, TextField, Typography, IconButton, Tooltip } from "@mui/material";
import SearchIcon from "@mui/icons-material/SearchRounded";
import OpenIcon from "@mui/icons-material/OpenInNewRounded";
import { Browser } from "@capacitor/browser";
import { useApi } from "../lib/api";
import { useInfinite, useSentinel } from "../lib/useInfinite";
import type { Card as HcbCard, Organization, Transaction } from "../lib/types";
import { money } from "../lib/format";
import { Empty, ErrorBox, ListSkeleton, OrgAvatar, TopBar } from "../components/common";
import TxRow from "../components/TxRow";
import CardRow from "../components/CardRow";

type OrgX = Organization & { users?: { id: string; name: string; avatar?: string; role?: string }[] };

export default function OrgPage() {
  const { id } = useParams();
  const nav = useNavigate();
  const org = useApi<OrgX>(`organizations/${id}?expand=balance_cents,users`);
  const [tab, setTab] = useState(0);
  const [q, setQ] = useState("");
  const [search, setSearch] = useState("");
  const [filter, setFilter] = useState<"" | "expenses" | "revenue" | "missing_receipts">("");
  useEffect(() => { const t = setTimeout(() => setSearch(q), 400); return () => clearTimeout(t); }, [q]);

  const params = new URLSearchParams();
  if (search) params.set("filters[search]", search);
  if (filter) params.set(`filters[${filter}]`, "true");
  const qs = params.toString();
  const txs = useInfinite<Transaction>(id ? `organizations/${id}/transactions${qs ? `?${qs}` : ""}` : null);
  const more = useCallback(() => { if (txs.hasMore && !txs.loading) txs.loadMore(); }, [txs]);
  const sentinel = useSentinel(more, tab === 0 && txs.hasMore && !txs.error);
  const cards = useApi<HcbCard[]>(tab === 1 ? `organizations/${id}/cards?expand=user` : null);

  const o = org.data;
  return (
    <>
      <TopBar back title={o?.name || "Organization"} actions={
        o && <Tooltip title="Open on HCB"><IconButton onClick={() => Browser.open({ url: `https://hcb.hackclub.com/${o.slug}` })}><OpenIcon /></IconButton></Tooltip>
      } />
      {org.error && <ErrorBox error={org.error} retry={org.reload} />}
      <Box sx={{ px: 2 }}>
        <Card sx={{ background: (t) => `linear-gradient(135deg, ${t.palette.primary.main}, #FF8C37)`, color: "#fff", borderRadius: "28px" }}>
          <CardContent sx={{ p: 3 }}>
            <Stack direction="row" spacing={2} alignItems="center">
              <OrgAvatar org={o} size={52} />
              <Box sx={{ flex: 1, minWidth: 0 }}>
                <Typography variant="body2" sx={{ opacity: 0.85 }}>Available balance</Typography>
                <Typography variant="h4" fontWeight={800}>{o?.balance_cents !== undefined ? money(o.balance_cents) : <CircularProgress size={28} color="inherit" />}</Typography>
              </Box>
            </Stack>
            <Stack direction="row" spacing={1} sx={{ mt: 2 }} alignItems="center">
              {o?.playground_mode && <Chip size="small" label="Playground mode" sx={{ bgcolor: "#fff3", color: "#fff" }} />}
              {o?.financially_frozen && <Chip size="small" label="Frozen" sx={{ bgcolor: "#fff3", color: "#fff" }} />}
              <Box sx={{ flex: 1 }} />
              {o?.users && (
                <AvatarGroup max={5} sx={{ "& .MuiAvatar-root": { width: 28, height: 28, fontSize: 13, borderColor: "transparent" } }}>
                  {o.users.map((u) => <Avatar key={u.id} src={u.avatar} alt={u.name}>{u.name?.[0]}</Avatar>)}
                </AvatarGroup>
              )}
            </Stack>
          </CardContent>
        </Card>
      </Box>
      <Tabs value={tab} onChange={(_, v) => setTab(v)} variant="fullWidth" sx={{ mt: 1 }}>
        <Tab label="Transactions" /><Tab label="Cards" /><Tab label="Team" />
      </Tabs>

      {tab === 0 && (
        <>
          <Box sx={{ px: 2, pt: 2 }}>
            <TextField fullWidth size="small" placeholder="Search transactions" value={q} onChange={(e) => setQ(e.target.value)}
              slotProps={{ input: { startAdornment: <InputAdornment position="start"><SearchIcon /></InputAdornment>, sx: { borderRadius: 999 } } }} />
            <Stack direction="row" spacing={1} sx={{ mt: 1.5, overflowX: "auto", pb: 0.5 }}>
              {([["", "All"], ["expenses", "Expenses"], ["revenue", "Revenue"], ["missing_receipts", "Missing receipt"]] as const).map(([k, l]) => (
                <Chip key={k} label={l} color={filter === k ? "primary" : "default"} variant={filter === k ? "filled" : "outlined"} onClick={() => setFilter(k)} />
              ))}
            </Stack>
          </Box>
          {txs.total !== null && <Typography variant="caption" color="text.secondary" sx={{ px: 3 }}>{txs.total} transactions</Typography>}
          <List>
            {txs.items.map((t) => <TxRow key={t.id} tx={t} onClick={() => nav(`/org/${id}/tx/${t.id}`)} />)}
          </List>
          {txs.error && <ErrorBox error={txs.error} retry={txs.refresh} />}
          {txs.loading && txs.items.length === 0 && <ListSkeleton />}
          {!txs.loading && !txs.error && txs.items.length === 0 && <Empty title="No transactions" />}
          <div ref={sentinel} style={{ height: 1 }} />
          {txs.loading && txs.items.length > 0 && <Box sx={{ textAlign: "center", py: 2 }}><CircularProgress size={24} /></Box>}
        </>
      )}

      {tab === 1 && (
        <Box sx={{ pt: 1 }}>
          {cards.error && <ErrorBox error={cards.error} retry={cards.reload} />}
          {!cards.data && cards.loading && <ListSkeleton n={3} />}
          {cards.data?.length === 0 && <Empty title="No cards" />}
          <List>{cards.data?.map((c) => <CardRow key={c.id} card={c} showUser />)}</List>
        </Box>
      )}

      {tab === 2 && (
        <List sx={{ pt: 1 }}>
          {o?.users?.map((u) => (
            <Box key={u.id} sx={{ display: "flex", alignItems: "center", gap: 2, px: 3, py: 1.25 }}>
              <Avatar src={u.avatar}>{u.name?.[0]}</Avatar>
              <Typography sx={{ flex: 1 }}>{u.name}</Typography>
              {u.role && <Chip size="small" label={u.role} variant="outlined" />}
            </Box>
          ))}
          {o && !o.users?.length && <Empty title="No team info" />}
        </List>
      )}
    </>
  );
}
