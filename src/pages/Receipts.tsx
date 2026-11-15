import { useCallback, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Box, Button, CircularProgress, Fab, IconButton, ImageList, ImageListItem, ImageListItemBar, List, Stack, Tab, Tabs, Typography } from "@mui/material";
import CameraIcon from "@mui/icons-material/PhotoCameraRounded";
import DeleteIcon from "@mui/icons-material/DeleteOutlineRounded";
import FileIcon from "@mui/icons-material/InsertDriveFileRounded";
import DoneIcon from "@mui/icons-material/TaskAltRounded";
import { Browser } from "@capacitor/browser";
import { useApi } from "../lib/api";
import { useInfinite, useSentinel } from "../lib/useInfinite";
import type { Receipt, Transaction } from "../lib/types";
import { Empty, ErrorBox, ListSkeleton, TopBar } from "../components/common";
import TxRow from "../components/TxRow";
import { deleteReceipt, pickFiles, uploadReceipt } from "../components/receipts";
import { toast } from "../components/toast";

export default function Receipts() {
  const nav = useNavigate();
  const [tab, setTab] = useState(0);
  const missing = useInfinite<Transaction>("user/transactions/missing_receipt?expand=organization");
  const more = useCallback(() => { if (missing.hasMore && !missing.loading) missing.loadMore(); }, [missing]);
  const sentinel = useSentinel(more, tab === 0 && missing.hasMore && !missing.error);
  const bin = useApi<Receipt[]>("receipts");
  const [uploading, setUploading] = useState(false);

  const addToBin = async () => {
    const files = await pickFiles({ multiple: true });
    if (!files.length) return;
    setUploading(true);
    try { for (const f of files) await uploadReceipt(f); toast("Added to Receipt Bin"); bin.reload(); setTab(1); }
    catch (e) { toast((e as Error).message, "error"); } finally { setUploading(false); }
  };
  const del = async (r: Receipt) => {
    if (!confirm(`Delete ${r.filename}?`)) return;
    try { await deleteReceipt(r.id); bin.setData((bin.data || []).filter((x) => x.id !== r.id)); } catch (e) { toast((e as Error).message, "error"); }
  };

  return (
    <>
      <TopBar large title="Receipts" />
      <Tabs value={tab} onChange={(_, v) => setTab(v)} variant="fullWidth">
        <Tab label={`Missing${missing.total ? ` (${missing.total})` : ""}`} />
        <Tab label={`Receipt Bin${bin.data?.length ? ` (${bin.data.length})` : ""}`} />
      </Tabs>
      {tab === 0 && (
        <>
          <List>{missing.items.map((t) => <TxRow key={t.id} tx={t} showOrg onClick={() => nav(t.organization ? `/org/${t.organization.id}/tx/${t.id}` : `/tx/${t.id}`)} />)}</List>
          {missing.error && <ErrorBox error={missing.error} retry={missing.refresh} />}
          {missing.loading && missing.items.length === 0 && <ListSkeleton />}
          {!missing.loading && !missing.error && missing.items.length === 0 && <Empty icon={<DoneIcon fontSize="inherit" />} title="All caught up!" subtitle="No transactions are missing receipts." />}
          <div ref={sentinel} style={{ height: 1 }} />
        </>
      )}
      {tab === 1 && (
        <Box sx={{ p: 2 }}>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Receipts you upload here wait in your bin until HCB matches them to a transaction.
          </Typography>
          {bin.error && <ErrorBox error={bin.error} retry={bin.reload} />}
          {!bin.data && bin.loading && <ListSkeleton n={2} />}
          {bin.data?.length === 0 && <Empty title="Your Receipt Bin is empty" />}
          {!!bin.data?.length && (
            <ImageList cols={2} gap={8}>
              {bin.data.map((r) => (
                <ImageListItem key={r.id} sx={{ borderRadius: 3, overflow: "hidden", bgcolor: "action.hover", cursor: "pointer" }} onClick={() => Browser.open({ url: r.url })}>
                  {r.preview_url ? <img src={r.preview_url} alt={r.filename} loading="lazy" style={{ height: 180, objectFit: "cover" }} />
                    : <Box sx={{ display: "grid", placeItems: "center", height: 180 }}><FileIcon fontSize="large" /></Box>}
                  <ImageListItemBar title={r.filename} actionIcon={<IconButton sx={{ color: "#fff" }} onClick={(e) => { e.stopPropagation(); del(r); }}><DeleteIcon /></IconButton>} />
                </ImageListItem>
              ))}
            </ImageList>
          )}
        </Box>
      )}
      <Fab color="primary" variant="extended" onClick={addToBin} disabled={uploading}
        sx={{ position: "fixed", right: 16, bottom: "calc(96px + env(safe-area-inset-bottom))" }}>
        {uploading ? <CircularProgress size={20} sx={{ mr: 1 }} color="inherit" /> : <CameraIcon sx={{ mr: 1 }} />} Add receipt
      </Fab>
    </>
  );
}
