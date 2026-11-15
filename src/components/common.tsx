import { ReactNode } from "react";
import { AppBar, Avatar, Box, Button, CircularProgress, IconButton, Toolbar, Typography, Alert, Skeleton, Stack } from "@mui/material";
import BackIcon from "@mui/icons-material/ArrowBackRounded";
import { useNavigate } from "react-router-dom";
import type { Organization } from "../lib/types";

export function TopBar({ title, back, actions, large }: { title: ReactNode; back?: boolean; actions?: ReactNode; large?: boolean }) {
  const nav = useNavigate();
  return (
    <AppBar position="sticky" sx={{ pt: "env(safe-area-inset-top)", bgcolor: "background.default" }}>
      <Toolbar sx={{ gap: 1, minHeight: large ? 72 : 64 }}>
        {back && <IconButton edge="start" onClick={() => nav(-1)} aria-label="Back"><BackIcon /></IconButton>}
        <Typography variant={large ? "h5" : "h6"} fontWeight={large ? 700 : 600} noWrap sx={{ flex: 1 }}>{title}</Typography>
        {actions}
      </Toolbar>
    </AppBar>
  );
}

export function OrgAvatar({ org, size = 44 }: { org?: Organization; size?: number }) {
  return (
    <Avatar src={org?.icon || undefined} variant="rounded" sx={{ width: size, height: size, bgcolor: "primary.main", fontWeight: 700, borderRadius: "12px" }}>
      {org?.name?.[0]?.toUpperCase()}
    </Avatar>
  );
}

export function Loading() {
  return <Box sx={{ display: "grid", placeItems: "center", py: 6 }}><CircularProgress /></Box>;
}

export function ListSkeleton({ n = 6 }: { n?: number }) {
  return (
    <Stack spacing={1} sx={{ px: 2 }}>
      {Array.from({ length: n }).map((_, i) => <Skeleton key={i} variant="rounded" height={64} />)}
    </Stack>
  );
}

export function ErrorBox({ error, retry }: { error: Error; retry?: () => void }) {
  return (
    <Box sx={{ p: 2 }}>
      <Alert severity="error" action={retry && <Button color="inherit" size="small" onClick={retry}>Retry</Button>}>{error.message}</Alert>
    </Box>
  );
}

export function Empty({ icon, title, subtitle }: { icon?: ReactNode; title: string; subtitle?: string }) {
  return (
    <Box sx={{ textAlign: "center", py: 8, px: 3, color: "text.secondary" }}>
      <Box sx={{ fontSize: 56, mb: 1, opacity: 0.6 }}>{icon}</Box>
      <Typography variant="h6" color="text.primary">{title}</Typography>
      {subtitle && <Typography variant="body2">{subtitle}</Typography>}
    </Box>
  );
}
