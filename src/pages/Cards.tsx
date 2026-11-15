import { useState } from "react";
import { FormControlLabel, List, Switch, Box } from "@mui/material";
import CardIcon from "@mui/icons-material/CreditCardOffRounded";
import { useApi } from "../lib/api";
import type { Card } from "../lib/types";
import { Empty, ErrorBox, ListSkeleton, TopBar } from "../components/common";
import CardRow from "../components/CardRow";

const ORDER: Record<string, number> = { active: 0, inactive: 1, frozen: 2, expired: 3, canceled: 4 };

export default function Cards() {
  const cards = useApi<Card[]>("user/cards?expand=organization");
  const [showCanceled, setShowCanceled] = useState(() => localStorage.getItem("showCanceled") === "1");
  const list = (cards.data || [])
    .filter((c) => showCanceled || (c.status !== "canceled" && c.status !== "expired"))
    .sort((a, b) => (ORDER[a.status] ?? 9) - (ORDER[b.status] ?? 9));
  return (
    <>
      <TopBar large title="Cards" />
      <Box sx={{ px: 3 }}>
        <FormControlLabel control={<Switch checked={showCanceled} onChange={(e) => { setShowCanceled(e.target.checked); localStorage.setItem("showCanceled", e.target.checked ? "1" : "0"); }} />} label="Show canceled & expired" />
      </Box>
      {cards.error && <ErrorBox error={cards.error} retry={cards.reload} />}
      {!cards.data && cards.loading && <ListSkeleton n={4} />}
      {cards.data && list.length === 0 && <Empty icon={<CardIcon fontSize="inherit" />} title="No cards" subtitle="Cards you're issued on HCB will show up here." />}
      <List>{list.map((c) => <CardRow key={c.id} card={c} />)}</List>
    </>
  );
}
