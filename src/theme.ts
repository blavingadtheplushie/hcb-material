import { createTheme } from "@mui/material/styles";
export const makeTheme = (mode: "light" | "dark") =>
  createTheme({
    palette: {
      mode,
      primary: { main: "#EC3750" },
      secondary: { main: "#338EDA" },
      success: { main: "#33D6A6" },
      background: mode === "dark" ? { default: "#17171D", paper: "#252429" } : { default: "#F9FAFC", paper: "#FFFFFF" },
    },
    shape: { borderRadius: 16 },
    typography: { fontFamily: "'Roboto Flex', Roboto, system-ui, sans-serif", button: { textTransform: "none", fontWeight: 600 } },
    components: {
      MuiButton: { defaultProps: { disableElevation: true }, styleOverrides: { root: { borderRadius: 999 } } },
      MuiFab: { styleOverrides: { root: { borderRadius: 16 } } },
      MuiCard: { defaultProps: { elevation: 0 } },
      MuiAppBar: { defaultProps: { elevation: 0, color: "inherit" } },
      MuiChip: { styleOverrides: { root: { borderRadius: 8 } } },
    },
  });
