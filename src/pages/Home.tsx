import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Avatar, Box, Button, Card, CardActionArea, CardContent, Chip, IconButton, Skeleton, Stack, Typography, Alert } from "@mui/material";
import RefreshIcon from "@mui/icons-material/RefreshRounded";
import MailIcon from "@mui/icons-material/MarkEmailUnreadRounded";
import { api, useApi } from "../lib/api";
import type { Invitation, Organization, Paginated, Transaction, User } from "../lib/types";
import { money } from "../lib/format";
import { ErrorBox, ListSkeleton, OrgAvatar, TopBar, Empty } from "../components/common";

function OrgCard({ org }: { org: Organization }) {
  const nav = useNavigate();
  const { data } = useApi<Organization>(`organizations/${org.id}?expand=balance_cents`);
  return (
    <Card sx={{ bgcolor: "background.paper", border: 1, borderColor: "divider" }}>
      <CardActionArea onClick={() => nav(`/org/${org.id}`)}>
        <CardContent sx={{ display: "flex", gap: 2, alignItems: "center" }}>
          <OrgAvatar org={org} />
          <Box sx={{ flex: 1, minWidth: 0 }}>
            <Typography fontWeight={600} noWrap>{org.name}</Typography>
            <Stack direction="row" spacing={0.5} sx={{ mt: 0.25 }}>
              {org.playground_mode && <Chip size="small" label="Playground" sx={{ height: 20 }} />}
              {org.financially_frozen && <Chip size="small" color="info" label="Frozen" sx={{ height: 20 }} />}
              {org.transparent && <Chip size="small" variant="outlined" label="Transparent" sx={{ height: 20 }} />}
            </Stack>
          </Box>
          <Box sx={{ textAlign: "right" }}>
            {data?.balance_cents !== undefined ? (
              <Typography fontWeight={700} variant="h6">{money(data.balance_cents)}</Typography>
            ) : <Skeleton width={80} height={32} />}
            <Typography variant="caption" color="text.secondary">balance</Typography>
          </Box>
        </CardContent>
      </CardActionArea>
    </Card>
  );
}

export default function Home() {
  const nav = useNavigate();
  const user = useApi<User>("user");
  const orgs = useApi<Organization[]>("user/organizations");
  const invites = useApi<Invitation[]>("user/invitations");
  const [missing, setMissing] = useState<number | null>(null);
  useEffect(() => {
    api<Paginated<Transaction>>("user/transactions/missing_receipt?limit=1").then((r) => setMissing(r.total_count)).catch(() => {});
  }, []);
  const pending = (invites.data || []).filter((i) => !i.accepted);
  const first = user.data?.name?.split(" ")[0];
  return (
    <>
      <TopBar large title={first ? `Hey ${first}` : "HCB"} actions={
        <>
          <IconButton onClick={() => { orgs.reload(); invites.reload(); user.reload(); }} aria-label="Refresh"><RefreshIcon /></IconButton>
          <Avatar src={user.data?.avatar} onClick={() => nav("/settings")} sx={{ width: 36, height: 36, cursor: "pointer" }}>{first?.[0]}</Avatar>
        </>
      } />
      <Stack spacing={1.5} sx={{ px: 2, pb: 2 }}>
        {missing !== null && missing > 0 && (
          <Alert severity="warning" action={<Button color="inherit" size="small" onClick={() => nav("/receipts")}>Upload</Button>}>
            {missing} transaction{missing > 1 ? "s" : ""} missing a receipt
          </Alert>
        )}
        {pending.map((inv) => (
          <Card key={inv.id} sx={{ bgcolor: "secondary.main", color: "#fff" }}>
            <CardActionArea onClick={() => nav(`/invitation/${inv.id}`)}>
              <CardContent sx={{ display: "flex", gap: 2, alignItems: "center" }}>
                <MailIcon />
                <Box sx={{ flex: 1 }}>
                  <Typography fontWeight={600}>Invitation to {inv.organization?.name || "an organization"}</Typography>
                  {inv.sender && <Typography variant="body2" sx={{ opacity: 0.85 }}>from {inv.sender.name}</Typography>}
                </Box>
              </CardContent>
            </CardActionArea>
          </Card>
        ))}
        <Typography variant="overline" color="text.secondary" sx={{ pt: 1 }}>Organizations</Typography>
      </Stack>
      {orgs.error && <ErrorBox error={orgs.error} retry={orgs.reload} />}
      {!orgs.data && orgs.loading && <ListSkeleton n={4} />}
      {orgs.data && orgs.data.length === 0 && <Empty title="No organizations yet" subtitle="Join or create an organization on HCB to see it here." />}
      <Stack spacing={1.5} sx={{ px: 2 }}>
        {orgs.data?.map((o) => <OrgCard key={o.id} org={o} />)}
      </Stack>
    </>
  );
}
