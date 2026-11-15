import { useState } from "react";
import { Box, Button, Collapse, Stack, TextField, Typography, Link } from "@mui/material";
import { Capacitor } from "@capacitor/core";
import { finishLogin, startLogin } from "../lib/auth";
import { toast } from "../components/toast";

export default function Login() {
  const [paste, setPaste] = useState("");
  const [busy, setBusy] = useState(false);
  const [showPaste, setShowPaste] = useState(!Capacitor.isNativePlatform());
  const go = async (signup: boolean) => { try { await startLogin(signup); } catch (e) { toast((e as Error).message, "error"); } };
  const finish = async () => {
    setBusy(true);
    try { await finishLogin(paste.trim()); } catch (e) { toast((e as Error).message, "error"); } finally { setBusy(false); }
  };
  return (
    <Box sx={{ minHeight: "100dvh", display: "flex", flexDirection: "column", justifyContent: "center", p: 3, pt: "calc(24px + env(safe-area-inset-top))",
      background: (t) => `radial-gradient(circle at 50% 0%, ${t.palette.primary.main}33, transparent 60%)` }}>
      <Stack spacing={3} sx={{ maxWidth: 420, mx: "auto", width: "100%" }}>
        <Box sx={{ textAlign: "center" }}>
          <Box component="img" src="https://assets.hackclub.com/hcb-light.svg" alt="" sx={{ width: 96, height: 96, borderRadius: 6, bgcolor: "primary.main", p: 1.5, mb: 2 }}
            onError={(e: any) => { e.currentTarget.style.display = "none"; }} />
          <Typography variant="h4" fontWeight={800}>HCB</Typography>
          <Typography color="text.secondary">An unofficial Material client for HCB by Hack Club.</Typography>
        </Box>
        <Button size="large" variant="contained" onClick={() => go(false)} sx={{ py: 1.5 }}>Log in</Button>
        <Button size="large" variant="outlined" onClick={() => go(true)} sx={{ py: 1.5 }}>Sign up</Button>
        {Capacitor.isNativePlatform() && (
          <Link component="button" variant="body2" onClick={() => setShowPaste((s) => !s)}>Login didn't come back to the app?</Link>
        )}
        <Collapse in={showPaste}>
          <Stack spacing={1.5}>
            <Typography variant="body2" color="text.secondary">
              After logging in, your browser tries to open a link starting with <code>hcb://</code>. Copy that link (or the code) and paste it here.
            </Typography>
            <TextField label="Redirect link or code" value={paste} onChange={(e) => setPaste(e.target.value)} placeholder="hcb://?code=…" fullWidth />
            <Button variant="contained" color="secondary" disabled={!paste || busy} onClick={finish}>Finish login</Button>
          </Stack>
        </Collapse>
        <Typography variant="caption" color="text.secondary" textAlign="center">Not affiliated with Hack Club. Uses the public HCB API v4.</Typography>
      </Stack>
    </Box>
  );
}
