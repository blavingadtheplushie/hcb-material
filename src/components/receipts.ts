import { api } from "../lib/api";
import type { Receipt } from "../lib/types";

export function pickFiles(opts: { camera?: boolean; multiple?: boolean } = {}): Promise<File[]> {
  return new Promise((resolve) => {
    const input = document.createElement("input");
    input.type = "file";
    input.accept = "image/*,application/pdf";
    if (opts.multiple) input.multiple = true;
    if (opts.camera) input.setAttribute("capture", "environment");
    input.onchange = () => resolve(Array.from(input.files || []));
    input.click();
  });
}

export async function uploadReceipt(file: File, transactionId?: string) {
  const body = new FormData();
  body.append("file", file, file.name || "receipt.jpg");
  if (transactionId) body.append("transaction_id", transactionId);
  return api<Receipt>("receipts", { method: "POST", body });
}

export const deleteReceipt = (id: string) => api(`receipts/${id}`, { method: "DELETE" });
