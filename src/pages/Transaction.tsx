import { useState } from "react";
import { useParams } from "react-router-dom";
import { Avatar, Box, Button, Card, CardContent, Chip, Dialog, DialogActions, DialogContent, DialogTitle, Divider, IconButton, List, ListItem, ListItemAvatar, ListItemText, Menu, MenuItem, Stack, TextField, Typography, ImageList, ImageListItem, ImageListItemBar, CircularProgress } from "@mui/material";
import MoreIcon from "@mui/icons-material/MoreVertRounded";
import CameraIcon from "@mui/icons-material/PhotoCameraRounded";
import UploadIcon from "@mui/icons-material/UploadFileRounded";
import DeleteIcon from "@mui/icons-material/DeleteOutlineRounded";
import SendIcon from "@mui/icons-material/SendRounded";
import FileIcon from "@mui/icons-material/InsertDriveFileRounded";
import { Browser } from "@capacitor/browser";
import { api, useApi } from "../lib/api";
import type { Comment, Receipt, Transaction } from "../lib/types";
import { dateTime, money, shortDate, txTitle } from "../lib/format";
import { ErrorBox, Loading, TopBar } from "../components/common";
import { toast } from "../components/toast";
import { deleteReceipt, pickFiles, uploadReceipt } from "../components/receipts";

function Row({ k, v }: { k: string; v?: React.ReactNode }) {
  if (v === undefined || v === null || v === "") return null;
  return (
    <Box sx={{ display: "flex", justifyContent: "space-between", gap: 2, py: 1 }}>
      <Typography color="text.secondary">{k}</Typography>
      <Typography fontWeight={500} textAlign="right" sx={{ wordBreak: "break-word" }}>{v}</Typography>
    </Box>
  );
}

function details(t: Transaction): [string, React.ReactNode][] {
  const r: [string, React.ReactNode][] = [];
  if (t.card_charge) {
    const c = t.card_charge;
    r.push(["Merchant", c.merchant.smart_name || c.merchant.name], ["Spent at", dateTime(c.spent_at)]);
    if (c.card?.user) r.push(["Cardholder", c.card.user.name]);
    if (c.card?.last4) r.push(["Card", `•••• ${c.card.last4}`]);
    if (c.wallet) r.push(["Wallet", c.wallet.replace("_", " ")]);
    if (c.decline_reason) r.push(["Decline reason", c.decline_reason.replace(/_/g, " ")]);
  }
  if (t.donation) r.push(["Donor", t.donation.donor.name], ["Message", t.donation.message], ["Recurring", t.donation.recurring ? "Yes" : undefined]);
  if (t.transfer) r.push(["From", t.transfer.from?.name], ["To", t.transfer.to?.name], ["Status", t.transfer.status], ["Sent by", t.transfer.sender?.name]);
  if (t.check) r.push(["Recipient", t.check.recipient_name], ["Memo", t.check.memo], ["Status", t.check.status?.replace(/_/g, " ")]);
  if (t.ach_transfer) r.push(["Recipient", t.ach_transfer.recipient_name], ["Bank", t.ach_transfer.bank_name], ["For", t.ach_transfer.payment_for], ["Sent by", t.ach_transfer.sender?.name]);
  if (t.wise_transfer) r.push(["Recipient", t.wise_transfer.recipient_name], ["For", t.wise_transfer.payment_for], ["Amount", `${(t.wise_transfer.amount_cents / 100).toFixed(2)} ${t.wise_transfer.currency}`]);
  if (t.invoice) r.push(["Sponsor", t.invoice.sponsor?.name], ["Description", t.invoice.description], ["Due", shortDate(t.invoice.due_date)]);
  if (t.check_deposit) r.push(["Status", t.check_deposit.status]);
  return r;
}

export default function TransactionPage() {
  const { orgId, txId } = useParams();
  const key = orgId ? `organizations/${orgId}/transactions/${txId}` : `transactions/${txId}?expand=organization`;
  const tx = useApi<Transaction>(key);
  const receipts = useApi<Receipt[]>(`receipts?transaction_id=${txId}`);
  const comments = useApi<Comment[]>(`comments?transaction_id=${txId}`);
  const [menu, setMenu] = useState<HTMLElement | null>(null);
  const [rename, setRename] = useState<string | null>(null);
  const [comment, setComment] = useState("");
  const [busy, setBusy] = useState(false);
  const [uploading, setUploading] = useState(0);
  const t = tx.data;
  const org = orgId || t?.organization?.id;

  const upload = async (camera: boolean) => {
    const files = await pickFiles({ camera, multiple: !camera });
    if (!files.length) return;
    setUploading(files.length);
    try {
      for (const f of files) { await uploadReceipt(f, txId); setUploading((n) => n - 1); }
      toast(files.length > 1 ? "Receipts uploaded" : "Receipt uploaded");
      receipts.reload(); tx.reload();
    } catch (e) { toast((e as Error).message, "error"); } finally { setUploading(0); }
  };

  const saveMemo = async () => {
    if (rename === null || !org) return;
    setBusy(true);
    try {
      const updated = await api<Transaction>(`organizations/${org}/transactions/${txId}`, { method: "PATCH", json: { memo: rename } });
      tx.setData({ ...t!, ...updated });
      setRename(null); toast("Renamed");
    } catch (e) { toast((e as Error).message, "error"); } finally { setBusy(false); }
  };

  const noReceipt = async () => {
    setMenu(null);
    try { await api(`transactions/${txId}/mark_no_receipt`, { method: "POST" }); toast("Marked as no/lost receipt"); tx.reload(); }
    catch (e) { toast((e as Error).message, "error"); }
  };

  const send = async () => {
    if (!comment.trim()) return;
    setBusy(true);
    try {
      const body = new FormData();
      body.append("content", comment.trim());
      body.append("transaction_id", txId!);
      const c = await api<Comment>("comments", { method: "POST", body });
      comments.setData([...(comments.data || []), c]);
      setComment("");
    } catch (e) { toast((e as Error).message, "error"); } finally { setBusy(false); }
  };

  const delReceipt = async (r: Receipt) => {
    if (!confirm(`Delete ${r.filename}?`)) return;
    try { await deleteReceipt(r.id); receipts.setData((receipts.data || []).filter((x) => x.id !== r.id)); tx.reload(); }
    catch (e) { toast((e as Error).message, "error"); }
  };

  if (tx.error) return (<><TopBar back title="Transaction" /><ErrorBox error={tx.error} retry={tx.reload} /></>);
  if (!t) return (<><TopBar back title="Transaction" /><Loading /></>);
  const positive = t.amount_cents > 0;

  return (
    <>
      <TopBar back title={txTitle(t)} actions={
        <>
          <IconButton onClick={(e) => setMenu(e.currentTarget)}><MoreIcon /></IconButton>
          <Menu anchorEl={menu} open={!!menu} onClose={() => setMenu(null)}>
            <MenuItem disabled={!org} onClick={() => { setMenu(null); setRename(t.memo); }}>Rename</MenuItem>
            {t.missing_receipt && <MenuItem onClick={noReceipt}>Mark as no/lost receipt</MenuItem>}
            {org && <MenuItem onClick={() => { setMenu(null); Browser.open({ url: `https://hcb.hackclub.com/hcb/${t.id.replace(/^txn_/, "")}` }); }}>Open on HCB</MenuItem>}
          </Menu>
        </>
      } />
      <Stack spacing={2} sx={{ px: 2, pb: 4 }}>
        <Box sx={{ textAlign: "center", py: 2 }}>
          <Typography variant="h3" fontWeight={800} color={positive ? "success.main" : "text.primary"} sx={{ textDecoration: t.declined || t.reversed ? "line-through" : "none" }}>
            {positive ? "+" : ""}{money(t.amount_cents)}
          </Typography>
          <Typography variant="h6" sx={{ mt: 1 }}>{t.memo}</Typography>
          <Stack direction="row" spacing={1} justifyContent="center" sx={{ mt: 1 }}>
            {t.pending && <Chip label="Pending" />}
            {t.declined && <Chip color="error" label="Declined" />}
            {t.reversed && <Chip label="Reversed" />}
            {t.lost_receipt && <Chip variant="outlined" label="No/lost receipt" />}
            {t.tags?.map((g) => <Chip key={g.id} variant="outlined" label={`${g.emoji ? g.emoji + " " : ""}${g.label}`} />)}
          </Stack>
        </Box>

        <Card sx={{ border: 1, borderColor: "divider" }}>
          <CardContent>
            <Row k="Date" v={shortDate(t.date)} />
            {t.organization && <Row k="Organization" v={t.organization.name} />}
            {details(t).map(([k, v]) => <Row key={k} k={k} v={v} />)}
          </CardContent>
        </Card>

        <Card sx={{ border: 1, borderColor: t.missing_receipt ? "warning.main" : "divider" }}>
          <CardContent>
            <Stack direction="row" alignItems="center" sx={{ mb: 1 }}>
              <Typography variant="h6" sx={{ flex: 1 }}>Receipts</Typography>
              {uploading > 0 && <CircularProgress size={20} sx={{ mr: 1 }} />}
              <IconButton onClick={() => upload(true)} disabled={uploading > 0} aria-label="Take photo"><CameraIcon /></IconButton>
              <IconButton onClick={() => upload(false)} disabled={uploading > 0} aria-label="Upload file"><UploadIcon /></IconButton>
            </Stack>
            {t.missing_receipt && !receipts.data?.length && <Typography color="warning.main" variant="body2">This transaction is missing a receipt.</Typography>}
            {receipts.data && receipts.data.length > 0 && (
              <ImageList cols={2} gap={8} sx={{ m: 0 }}>
                {receipts.data.map((r) => (
                  <ImageListItem key={r.id} sx={{ borderRadius: "12px", overflow: "hidden", bgcolor: "action.hover", cursor: "pointer", minHeight: 120 }}
                    onClick={() => Browser.open({ url: r.url })}>
                    {r.preview_url ? <img src={r.preview_url} alt={r.filename} loading="lazy" style={{ objectFit: "cover", height: 160 }} />
                      : <Box sx={{ display: "grid", placeItems: "center", height: 160 }}><FileIcon fontSize="large" /></Box>}
                    <ImageListItemBar title={r.filename} subtitle={r.uploader?.name}
                      actionIcon={<IconButton sx={{ color: "#fff" }} onClick={(e) => { e.stopPropagation(); delReceipt(r); }}><DeleteIcon /></IconButton>} />
                  </ImageListItem>
                ))}
              </ImageList>
            )}
          </CardContent>
        </Card>

        <Card sx={{ border: 1, borderColor: "divider" }}>
          <CardContent>
            <Typography variant="h6" sx={{ mb: 1 }}>Comments</Typography>
            {comments.error && <Typography color="error" variant="body2">{comments.error.message}</Typography>}
            <List disablePadding>
              {comments.data?.map((c, i) => (
                <Box key={c.id}>
                  {i > 0 && <Divider component="li" />}
                  <ListItem alignItems="flex-start" disableGutters>
                    <ListItemAvatar><Avatar src={c.user?.avatar}>{c.user?.name?.[0]}</Avatar></ListItemAvatar>
                    <ListItemText
                      primary={<>{c.user?.name} <Typography component="span" variant="caption" color="text.secondary">{dateTime(c.created_at)}{c.admin_only ? " · admin only" : ""}</Typography></>}
                      secondary={<>
                        <Typography component="span" variant="body2" color="text.primary" sx={{ whiteSpace: "pre-wrap" }}>{c.content}</Typography>
                        {c.file && <><br /><Button size="small" startIcon={<FileIcon />} onClick={() => Browser.open({ url: c.file! })}>Attachment</Button></>}
                      </>}
                    />
                  </ListItem>
                </Box>
              ))}
            </List>
            {comments.data?.length === 0 && <Typography variant="body2" color="text.secondary">No comments yet.</Typography>}
            <Stack direction="row" spacing={1} sx={{ mt: 1.5 }}>
              <TextField fullWidth size="small" multiline maxRows={4} placeholder="Add a comment" value={comment} onChange={(e) => setComment(e.target.value)} />
              <IconButton color="primary" disabled={!comment.trim() || busy} onClick={send}><SendIcon /></IconButton>
            </Stack>
          </CardContent>
        </Card>
      </Stack>

      <Dialog open={rename !== null} onClose={() => setRename(null)} fullWidth>
        <DialogTitle>Rename transaction</DialogTitle>
        <DialogContent><TextField autoFocus fullWidth margin="dense" label="Memo" value={rename || ""} onChange={(e) => setRename(e.target.value)} /></DialogContent>
        <DialogActions>
          <Button onClick={() => setRename(null)}>Cancel</Button>
          <Button variant="contained" disabled={busy} onClick={saveMemo}>Save</Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
