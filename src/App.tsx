import { useEffect, useMemo, useState } from "react";
import { HashRouter, Navigate, Route, Routes, useLocation, useNavigate } from "react-router-dom";
import { CssBaseline, ThemeProvider, useMediaQuery, Box, Paper, BottomNavigation, BottomNavigationAction, CircularProgress } from "@mui/material";
import HomeIcon from "@mui/icons-material/HomeRounded";
import CardIcon from "@mui/icons-material/CreditCardRounded";
import ReceiptIcon from "@mui/icons-material/ReceiptLongRounded";
import SettingsIcon from "@mui/icons-material/SettingsRounded";
import { App as CapApp } from "@capacitor/app";
import { Capacitor } from "@capacitor/core";
import { makeTheme } from "./theme";
import { finishLogin, getTokens } from "./lib/auth";
import { clearCache } from "./lib/api";
import { Toaster, toast } from "./components/toast";
import Login from "./pages/Login";
import Home from "./pages/Home";
import OrgPage from "./pages/Org";
import TransactionPage from "./pages/Transaction";
import Cards from "./pages/Cards";
import CardPage from "./pages/Card";
import Receipts from "./pages/Receipts";
import Settings from "./pages/Settings";
import InvitationPage from "./pages/Invitation";

const TABS = ["/", "/cards", "/receipts", "/settings"];

function Shell() {
  const loc = useLocation();
  const nav = useNavigate();
  const tab = TABS.indexOf(loc.pathname);
  // Android hardware back button
  useEffect(() => {
    if (!Capacitor.isNativePlatform()) return;
    const h = CapApp.addListener("backButton", () => {
      if (TABS.includes(window.location.hash.slice(1) || "/")) CapApp.minimizeApp(); else nav(-1);
    });
    return () => { h.then((x) => x.remove()); };
  }, [nav]);
  return (
    <Box sx={{ minHeight: "100dvh", pb: tab >= 0 ? "calc(80px + env(safe-area-inset-bottom))" : 0 }}>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/org/:id" element={<OrgPage />} />
        <Route path="/org/:orgId/tx/:txId" element={<TransactionPage />} />
        <Route path="/tx/:txId" element={<TransactionPage />} />
        <Route path="/cards" element={<Cards />} />
        <Route path="/cards/:id" element={<CardPage />} />
        <Route path="/receipts" element={<Receipts />} />
        <Route path="/settings" element={<Settings />} />
        <Route path="/invitation/:id" element={<InvitationPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
      {tab >= 0 && (
        <Paper elevation={3} sx={{ position: "fixed", bottom: 0, left: 0, right: 0, pb: "env(safe-area-inset-bottom)", zIndex: 10, borderRadius: 0 }}>
          <BottomNavigation showLabels value={tab} onChange={(_, v) => nav(TABS[v], { replace: true })} sx={{ height: 72 }}>
            <BottomNavigationAction label="Home" icon={<HomeIcon />} />
            <BottomNavigationAction label="Cards" icon={<CardIcon />} />
            <BottomNavigationAction label="Receipts" icon={<ReceiptIcon />} />
            <BottomNavigationAction label="Settings" icon={<SettingsIcon />} />
          </BottomNavigation>
        </Paper>
      )}
    </Box>
  );
}

export default function App() {
  const dark = useMediaQuery("(prefers-color-scheme: dark)");
  const [pref, setPref] = useState<string>(() => localStorage.getItem("theme") || "system");
  const mode = pref === "system" ? (dark ? "dark" : "light") : (pref as "light" | "dark");
  const theme = useMemo(() => makeTheme(mode), [mode]);
  const [authed, setAuthed] = useState<boolean | null>(null);

  useEffect(() => {
    const check = () => getTokens().then((t) => { setAuthed(!!t); if (!t) clearCache(); });
    check();
    window.addEventListener("hcb-auth", check);
    const onTheme = () => setPref(localStorage.getItem("theme") || "system");
    window.addEventListener("hcb-theme", onTheme);
    let sub: Promise<{ remove: () => void }> | undefined;
    if (Capacitor.isNativePlatform()) {
      sub = CapApp.addListener("appUrlOpen", ({ url }) => {
        if (url.startsWith("hcb:")) finishLogin(url).catch((e) => toast(e.message, "error"));
      });
      // cold start: app was killed while the browser was open
      CapApp.getLaunchUrl().then((r) => {
        if (r?.url?.startsWith("hcb:")) finishLogin(r.url).catch(() => {});
      });
    }
    return () => { window.removeEventListener("hcb-auth", check); window.removeEventListener("hcb-theme", onTheme); sub?.then((s) => s.remove()); };
  }, []);

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      {authed === null ? (
        <Box sx={{ display: "grid", placeItems: "center", height: "100dvh" }}><CircularProgress /></Box>
      ) : authed ? (
        <HashRouter><Shell /></HashRouter>
      ) : (
        <Login />
      )}
      <Toaster />
    </ThemeProvider>
  );
}
