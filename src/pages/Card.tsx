import { useCallback, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { Box, Button, Chip, CircularProgress, List, Stack, Typography, Card as MCard, CardContent } from "@mui/material";
import SnowIcon from "@mui/icons-material/AcUnitRounded";
import SunIcon from "@mui/icons-material/WbSunnyRounded";
import { api, mutate, useApi } from "../lib/api";
import { useInfinite, useSentinel } from "../lib/useInfinite";
import type { Card, Transaction } from "../lib/types";
import { money } from "../lib/format";
import { Empty, ErrorBox, Loading, TopBar } from "../components/common";
import TxRow from "../components/TxRow";
import { statusColor } from "../components/CardRow";
import { toast } from "../components/toast";

export function CardVisual({ card }: { card: Card }) {
  const physical = card.type === "physical";
  return (
    <Box sx={{
      aspectRatio: "1.586", borderRadius: "20px", p: 2.5, color: "#fff", position: "relative", overflow: "hidden",
      background: physical ? "linear-gradient(135deg,#2b2b33,#0c0c10)" : "linear-gradient(135deg,#EC3750,#FF8C37)",
      filter: card.status === "frozen" ? "saturate(0.3) brightness(1.1)" : card.status === "canceled" ? "grayscale(1)" : "none",
      display: "flex", flexDirection: "column", justifyContent: "space-between", boxShadow: 6,
    }}>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Typography fontWeight={800} letterSpacing={1}>HCB</Typography>
        <Typography variant="caption" sx={{ opacity: 0.8 }}>{physical ? "PHYSICAL" : "VIRTUAL"}</Typography>
      </Stack>
      <Typography variant="h5" fontFamily="monospace" letterSpacing={3}>•••• •••• •••• {card.last4 || "••••"}</Typography>
      <Stack direction="row" justifyContent="space-between" alignItems="flex-end">
        <Box>
          <Typography variant="caption" sx={{ opacity: 0.75 }}>{card.organization?.name}</Typography>
          <Typography fontWeight={600}>{card.user?.name || card.name}</Typography>
        </Box>
        {card.exp_month && <Typography fontFamily="monospace">{String(card.exp_month).padStart(2, "0")}/{String(card.exp_year).slice(-2)}</Typography>}
      </Stack>
      {card.status === "frozen" && <SnowIcon sx={{ position: "absolute", right: -20, bottom: -20, fontSize: 140, opacity: 0.15 }} />}
    </Box>
  );
}

export default function CardPage() {
  const { id } = useParams();
  const nav = useNavigate();
  const card = useApi<Card>(`cards/${id}?expand=organization,user,total_spent_cents`);
  const txs = useInfinite<Transaction>(`cards/${id}/transactions`);
  const more = useCallback(() => { if (txs.hasMore && !txs.loading) txs.loadMore(); }, [txs]);
  const sentinel = useSentinel(more, txs.hasMore && !txs.error);
  const [busy, setBusy] = useState(false);
  const c = card.data;

  const toggle = async () => {
    if (!c) return;
    const freezing = c.status === "active";
    if (freezing && !confirm("Freeze this card? Purchases will be declined until you defrost it.")) return;
    setBusy(true);
    try {
      await api(`cards/${id}/${freezing ? "freeze" : "defrost"}`, { method: "POST" }).catch(async (e) => {
        // older API: PATCH cards/:id { status }
        if (e.status === 404) return api(`cards/${id}`, { method: "PATCH", json: { status: freezing ? "frozen" : "active" } });
        throw e;
      });
      card.setData({ ...c, status: freezing ? "frozen" : "active" });
      mutate("user/cards?expand=organization");
      toast(freezing ? "Card frozen" : "Card defrosted");
    } catch (e) { toast((e as Error).message, "error"); } finally { setBusy(false); }
  };

  if (card.error) return (<><TopBar back title="Card" /><ErrorBox error={card.error} retry={card.reload} /></>);
  if (!c) return (<><TopBar back title="Card" /><Loading /></>);
  const canToggle = c.status === "active" || c.status === "frozen";
  return (
    <>
      <TopBar back title={c.name || "Card"} />
      <Stack spacing={2} sx={{ px: 2 }}>
        <CardVisual card={c} />
        <Stack direction="row" spacing={1} alignItems="center">
          <Chip label={c.status} color={statusColor(c.status)} />
          <Box sx={{ flex: 1 }} />
          {canToggle && (
            <Button variant={c.status === "active" ? "outlined" : "contained"} disabled={busy} onClick={toggle}
              startIcon={busy ? <CircularProgress size={16} /> : c.status === "active" ? <SnowIcon /> : <SunIcon />}>
              {c.status === "active" ? "Freeze" : "Defrost"}
            </Button>
          )}
        </Stack>
        {c.total_spent_cents !== undefined && (
          <MCard sx={{ border: 1, borderColor: "divider" }}>
            <CardContent sx={{ display: "flex", justifyContent: "space-between" }}>
              <Typography color="text.secondary">Total spent</Typography>
              <Typography fontWeight={700}>{money(c.total_spent_cents)}</Typography>
            </CardContent>
          </MCard>
        )}
        <Typography variant="overline" color="text.secondary">Transactions</Typography>
      </Stack>
      <List>{txs.items.map((t) => <TxRow key={t.id} tx={t} onClick={() => nav(c.organization ? `/org/${c.organization.id}/tx/${t.id}` : `/tx/${t.id}`)} />)}</List>
      {txs.error && <ErrorBox error={txs.error} retry={txs.refresh} />}
      {!txs.loading && !txs.error && txs.items.length === 0 && <Empty title="No transactions yet" />}
      <div ref={sentinel} style={{ height: 1 }} />
      {txs.loading && <Box sx={{ textAlign: "center", py: 2 }}><CircularProgress size={24} /></Box>}
    </>
  );
}
