import type React from "react";
import { Avatar, Box, Chip, ListItemAvatar, ListItemButton, ListItemText, Typography } from "@mui/material";
import CardIcon from "@mui/icons-material/CreditCardRounded";
import GiftIcon from "@mui/icons-material/VolunteerActivismRounded";
import SwapIcon from "@mui/icons-material/SwapHorizRounded";
import InvoiceIcon from "@mui/icons-material/RequestQuoteRounded";
import CheckIcon from "@mui/icons-material/LocalPostOfficeRounded";
import BankIcon from "@mui/icons-material/AccountBalanceRounded";
import FeeIcon from "@mui/icons-material/PercentRounded";
import OtherIcon from "@mui/icons-material/ReceiptRounded";
import WarnIcon from "@mui/icons-material/ErrorOutlineRounded";
import type { Transaction } from "../lib/types";
import { money, shortDate, txKind } from "../lib/format";

const ICONS: Record<string, React.ReactElement> = {
  card: <CardIcon />, donation: <GiftIcon />, transfer: <SwapIcon />, invoice: <InvoiceIcon />,
  check: <CheckIcon />, bank: <BankIcon />, fee: <FeeIcon />, other: <OtherIcon />,
};

export default function TxRow({ tx, onClick, showOrg }: { tx: Transaction; onClick?: () => void; showOrg?: boolean }) {
  const positive = tx.amount_cents > 0;
  const struck = tx.declined || tx.reversed;
  return (
    <ListItemButton onClick={onClick} sx={{ borderRadius: "12px", mx: 1, opacity: struck ? 0.55 : 1 }}>
      <ListItemAvatar>
        <Avatar sx={{ bgcolor: positive ? "success.main" : "action.selected", color: positive ? "#fff" : "text.primary" }}>{ICONS[txKind(tx)]}</Avatar>
      </ListItemAvatar>
      <ListItemText
        primary={<Typography noWrap fontWeight={500} sx={{ textDecoration: struck ? "line-through" : "none" }}>{tx.memo}</Typography>}
        secondary={
          <Box component="span" sx={{ display: "flex", gap: 0.75, alignItems: "center", flexWrap: "wrap" }}>
            <span>{showOrg && tx.organization ? `${tx.organization.name} · ` : ""}{shortDate(tx.date)}</span>
            {tx.pending && <Chip size="small" label="Pending" sx={{ height: 20 }} />}
            {tx.declined && <Chip size="small" color="error" label="Declined" sx={{ height: 20 }} />}
            {tx.missing_receipt && !tx.declined && <Chip size="small" color="warning" variant="outlined" icon={<WarnIcon />} label="Receipt" sx={{ height: 20 }} />}
          </Box>
        }
        secondaryTypographyProps={{ component: "div" }}
      />
      <Typography fontWeight={600} color={positive ? "success.main" : "text.primary"} sx={{ ml: 1, whiteSpace: "nowrap" }}>
        {positive ? "+" : ""}{money(tx.amount_cents)}
      </Typography>
    </ListItemButton>
  );
}
