const API_URL = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

export type PaymentStatus =
  | "CREATED" | "PROCESSING" | "SUCCESS" | "FAILED" | "TIMEOUT"
  | "RETRYING" | "CANCELLED" | "REFUND_REQUESTED" | "REFUND_PROCESSING"
  | "REFUNDED" | "REFUND_FAILED";

export interface Payment {
  paymentId: string;
  status: PaymentStatus;
  amount: number;
  currency: string;
  merchantId: string;
  paymentMethod: string;
  provider: string | null;
  description: string | null;
  retryCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePaymentInput {
  amount: number;
  currency: string;
  paymentMethod: string;
  merchantId: string;
  customerReference?: string;
  description?: string;
  provider?: string;
}

async function handle<T>(res: Response): Promise<T> {
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error(body.message ?? `Request failed with status ${res.status}`);
  }
  return res.json() as Promise<T>;
}

export const api = {
  createPayment(input: CreatePaymentInput, idempotencyKey: string): Promise<Payment> {
    return fetch(`${API_URL}/api/v1/payments`, {
      method: "POST",
      headers: { "Content-Type": "application/json", "Idempotency-Key": idempotencyKey },
      body: JSON.stringify(input),
    }).then((r) => handle<Payment>(r));
  },

  getPayment(paymentId: string): Promise<Payment> {
    return fetch(`${API_URL}/api/v1/payments/${paymentId}`).then((r) => handle<Payment>(r));
  },

  listPayments(merchantId: string): Promise<{ content: Payment[] }> {
    return fetch(`${API_URL}/api/v1/payments?merchantId=${merchantId}`).then((r) =>
      handle<{ content: Payment[] }>(r)
    );
  },
};
