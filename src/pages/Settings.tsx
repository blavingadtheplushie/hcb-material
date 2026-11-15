import { Avatar, Box, Button, Card, CardContent, List, ListItemButton, ListItemIcon, ListItemText, ToggleButton, ToggleButtonGroup, Typography, Stack } from "@mui/material";
import OpenIcon from "@mui/icons-material/OpenInNewRounded";
import CodeIcon from "@mui/icons-material/CodeRounded";
import HelpIcon from "@mui/icons-material/HelpOutlineRounded";
import { useState } from "react";
import { Browser } from "@capacitor/browser";
import { useApi } from "../lib/api";
import { logout } from "../lib/auth";
import type { User } from "../lib/types";
import { TopBar } from "../components/common";

export default function Settings() {
  const user = useApi<User>("user");
  const [theme, setTheme] = useState(localStorage.getItem("theme") || "system");
  const u = user.data;
  return (
    <>
      <TopBar large title="Settings" />
      <Stack spacing={2} sx={{ px: 2 }}>
        <Card sx={{ border: 1, borderColor: "divider" }}>
          <CardContent sx={{ display: "flex", gap: 2, alignItems: "center" }}>
            <Avatar src={u?.avatar} sx={{ width: 56, height: 56 }}>{u?.name?.[0]}</Avatar>
            <Box sx={{ minWidth: 0 }}>
              <Typography variant="h6" noWrap>{u?.name}</Typography>
              <Typography color="text.secondary" noWrap>{u?.email}</Typography>
            </Box>
          </CardContent>
        </Card>
        <Box>
          <Typography variant="overline" color="text.secondary">Theme</Typography>
          <ToggleButtonGroup exclusive fullWidth value={theme} onChange={(_, v) => { if (!v) return; setTheme(v); localStorage.setItem("theme", v); window.dispatchEvent(new Event("hcb-theme")); }}>
            <ToggleButton value="system">System</ToggleButton>
            <ToggleButton value="light">Light</ToggleButton>
            <ToggleButton value="dark">Dark</ToggleButton>
          </ToggleButtonGroup>
        </Box>
        <List sx={{ bgcolor: "background.paper", borderRadius: "20px", border: 1, borderColor: "divider" }}>
          <ListItemButton onClick={() => Browser.open({ url: "https://hcb.hackclub.com/my/settings" })}><ListItemIcon><OpenIcon /></ListItemIcon><ListItemText primary="Account settings on HCB" /></ListItemButton>
          <ListItemButton onClick={() => Browser.open({ url: "https://help.hcb.hackclub.com" })}><ListItemIcon><HelpIcon /></ListItemIcon><ListItemText primary="HCB help center" /></ListItemButton>
          <ListItemButton onClick={() => Browser.open({ url: "https://github.com/blavingadtheplushie/hcb-material" })}><ListItemIcon><CodeIcon /></ListItemIcon><ListItemText primary="Source code" secondary="Unofficial client, not affiliated with Hack Club" /></ListItemButton>
        </List>
        <Button color="error" variant="outlined" size="large" onClick={() => { if (confirm("Log out?")) logout(); }}>Log out</Button>
        <Typography variant="caption" color="text.secondary" textAlign="center">HCB Material v0.1.0</Typography>
      </Stack>
    </>
  );
}
