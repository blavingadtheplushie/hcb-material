export interface User { id: string; name: string; email?: string; avatar?: string; admin?: boolean }
export interface Organization {
  id: string; name: string; slug: string; icon?: string | null; country?: string;
  playground_mode?: boolean; transparent?: boolean; financially_frozen?: boolean;
  balance_cents?: number; fee_balance_cents?: number; parent_id?: string | null;
}
export interface Card {
  id: string; type: "virtual" | "physical"; status: string; name?: string;
  last4?: string | null; exp_month?: number; exp_year?: number;
  organization?: Organization; user?: User; total_spent_cents?: number;
}
export interface Tag { id: string; label: string; color: string; emoji?: string | null }
export interface Transaction {
  id: string; date: string; amount_cents: number; memo: string; has_custom_memo: boolean;
  pending: boolean; declined: boolean; reversed: boolean; code: string;
  missing_receipt: boolean; lost_receipt: boolean; tags?: Tag[];
  organization?: Organization;
  card_charge?: { merchant: { name: string; smart_name?: string; country: string }; spent_at: string; card: Card & { user?: User }; decline_reason?: string; wallet?: string };
  donation?: { donor: { name: string; email?: string }; message?: string; recurring?: boolean; refunded?: boolean };
  transfer?: { from: Organization; to: Organization; memo: string; status: string; sender?: User };
  check?: { recipient_name: string; memo: string; status?: string; sender?: User };
  ach_transfer?: { recipient_name: string; bank_name: string; payment_for: string; sender?: User };
  invoice?: { sponsor: { name: string }; description: string; due_date: string };
  wise_transfer?: { recipient_name: string; payment_for: string; currency: string; amount_cents: number };
  check_deposit?: { status: string };
  expense_payout?: { report_id: string };
}
export interface Receipt { id: string; url: string; preview_url?: string; filename: string; created_at: string; uploader?: User | null }
export interface Comment { id: string; content: string; user: User; created_at: string; file?: string; admin_only?: boolean }
export interface Invitation { id: string; accepted: boolean; role?: string; sender?: User; organization?: Organization; created_at: string }
export interface Paginated<T> { total_count: number; has_more: boolean; data: T[] }
