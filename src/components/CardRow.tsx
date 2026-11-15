import { Avatar, Box, Chip, ListItemAvatar, ListItemButton, ListItemText, Typography } from "@mui/material";
import CardIcon from "@mui/icons-material/CreditCardRounded";
import { useNavigate } from "react-router-dom";
import type { Card } from "../lib/types";

export const statusColor = (s: string) => (s === "active" ? "success" : s === "frozen" ? "info" : s === "inactive" ? "warning" : "default") as any;

export default function CardRow({ card, showUser }: { card: Card; showUser?: boolean }) {
  const nav = useNavigate();
  return (
    <ListItemButton onClick={() => nav(`/cards/${card.id}`)} sx={{ borderRadius: "12px", mx: 1, opacity: card.status === "canceled" ? 0.5 : 1 }}>
      <ListItemAvatar><Avatar sx={{ bgcolor: card.type === "physical" ? "#1f1f24" : "primary.main", color: "#fff" }}><CardIcon /></Avatar></ListItemAvatar>
      <ListItemText
        primary={<Typography fontWeight={500} noWrap>{card.name || (card.type === "physical" ? "Physical card" : "Virtual card")}{card.last4 ? ` ·•• ${card.last4}` : ""}</Typography>}
        secondary={showUser ? card.user?.name : card.organization?.name}
      />
      <Box><Chip size="small" label={card.status} color={statusColor(card.status)} variant="outlined" /></Box>
    </ListItemButton>
  );
}
