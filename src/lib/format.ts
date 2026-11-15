import type { Transaction } from "./types";
export const money = (cents: number) =>
  (cents < 0 ? "-" : "") + "$" + (Math.abs(cents) / 100).toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
export const shortDate = (d: string) => new Date(d).toLocaleDateString(undefined, { month: "short", day: "numeric", year: "numeric" });
export const dateTime = (d: string) => new Date(d).toLocaleString(undefined, { dateStyle: "medium", timeStyle: "short" });

// Same labels as hcb-mobile lib/transactionTitle.ts
export function txTitle(t: Transaction): string {
  const a = money(Math.abs(t.amount_cents));
  if (t.card_charge) return `Card charge of ${a}`;
  if (t.check) return `Check of ${a}`;
  if (t.transfer) return `Transfer of ${a}`;
  if (t.donation) return `Donation of ${a}`;
  if (t.wise_transfer) return `Wise transfer of ${a}`;
  if (t.ach_transfer) return `ACH transfer of ${a}`;
  if (t.check_deposit) return `Check deposit of ${a}`;
  if (t.invoice) return `Invoice of ${a}`;
  if (t.expense_payout) return `Expense payout of ${a}`;
  if (t.code === "700") return `Bank fee of ${a}`;
  return "Transaction";
}
export function txKind(t: Transaction): string {
  if (t.card_charge) return "card";
  if (t.donation) return "donation";
  if (t.transfer) return "transfer";
  if (t.invoice) return "invoice";
  if (t.check || t.check_deposit) return "check";
  if (t.ach_transfer || t.wise_transfer) return "bank";
  if (t.code === "700" || t.code === "701" || t.code === "702") return "fee";
  return "other";
}
