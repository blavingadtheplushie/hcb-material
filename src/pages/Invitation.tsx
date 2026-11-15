import { useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { Avatar, Box, Button, Chip, Stack, Typography } from "@mui/material";
import { api, mutate, useApi } from "../lib/api";
import type { Invitation } from "../lib/types";
import { ErrorBox, Loading, OrgAvatar, TopBar } from "../components/common";
import { toast } from "../components/toast";

export default function InvitationPage() {
  const { id } = useParams();
  const nav = useNavigate();
  const inv = useApi<Invitation>(`user/invitations/${id}?expand=organization,sender`);
  const [busy, setBusy] = useState(false);
  const act = async (a: "accept" | "reject") => {
    setBusy(true);
    try {
      await api(`user/invitations/${id}/${a}`, { method: "POST" });
      mutate("user/invitations"); mutate("user/organizations");
      toast(a === "accept" ? "Welcome aboard!" : "Invitation declined");
      nav(a === "accept" && inv.data?.organization ? `/org/${inv.data.organization.id}` : "/", { replace: true });
    } catch (e) { toast((e as Error).message, "error"); } finally { setBusy(false); }
  };
  const i = inv.data;
  return (
    <>
      <TopBar back title="Invitation" />
      {inv.error && <ErrorBox error={inv.error} retry={inv.reload} />}
      {!i && !inv.error && <Loading />}
      {i && (
        <Stack spacing={3} alignItems="center" sx={{ p: 3, textAlign: "center" }}>
          <OrgAvatar org={i.organization} size={96} />
          <Box>
            <Typography variant="h5" fontWeight={700}>{i.organization?.name}</Typography>
            {i.sender && (
              <Stack direction="row" spacing={1} alignItems="center" justifyContent="center" sx={{ mt: 1 }}>
                <Avatar src={i.sender.avatar} sx={{ width: 24, height: 24 }} />
                <Typography color="text.secondary">{i.sender.name} invited you</Typography>
              </Stack>
            )}
            {i.role && <Chip sx={{ mt: 1 }} label={`Role: ${i.role}`} />}
          </Box>
          <Stack direction="row" spacing={2} sx={{ width: "100%", maxWidth: 400 }}>
            <Button fullWidth size="large" variant="outlined" disabled={busy} onClick={() => act("reject")}>Decline</Button>
            <Button fullWidth size="large" variant="contained" disabled={busy} onClick={() => act("accept")}>Accept</Button>
          </Stack>
        </Stack>
      )}
    </>
  );
}
