import { useEffect, useState } from "react";
import { Alert, Snackbar } from "@mui/material";
type Sev = "success" | "error" | "info";
export function toast(msg: string, sev: Sev = "success") { window.dispatchEvent(new CustomEvent("hcb-toast", { detail: { msg, sev } })); }
export function Toaster() {
  const [t, setT] = useState<{ msg: string; sev: Sev; k: number } | null>(null);
  useEffect(() => {
    const h = (e: Event) => setT({ ...(e as CustomEvent).detail, k: Date.now() });
    window.addEventListener("hcb-toast", h);
    return () => window.removeEventListener("hcb-toast", h);
  }, []);
  return (
    <Snackbar key={t?.k} open={!!t} autoHideDuration={3500} onClose={() => setT(null)} anchorOrigin={{ vertical: "bottom", horizontal: "center" }} sx={{ mb: 10 }}>
      <Alert severity={t?.sev || "info"} variant="filled" onClose={() => setT(null)} sx={{ width: "100%" }}>{t?.msg}</Alert>
    </Snackbar>
  );
}
