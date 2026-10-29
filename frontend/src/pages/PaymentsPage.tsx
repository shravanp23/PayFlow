import { useEffect, useState } from "react";
import { api, Payment } from "../lib/api";

const STATUS_COLORS: Record<string, string> = {
  SUCCESS: "bg-green-100 text-green-700",
  FAILED: "bg-red-100 text-red-700",
  PROCESSING: "bg-blue-100 text-blue-700",
  CREATED: "bg-gray-100 text-gray-700",
  TIMEOUT: "bg-amber-100 text-amber-700",
  RETRYING: "bg-amber-100 text-amber-700",
  REFUNDED: "bg-purple-100 text-purple-700",
};

const DEMO_MERCHANT_ID = "00000000-0000-0000-0000-000000000001";

export default function PaymentsPage() {
  const [payments, setPayments] = useState<Payment[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);

  async function load() {
    setLoading(true);
    setError(null);
    try {
      const res = await api.listPayments(DEMO_MERCHANT_ID);
      setPayments(res.content);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  async function createDemoPayment() {
    setCreating(true);
    try {
      await api.createPayment(
        {
          amount: 1499,
          currency: "INR",
          paymentMethod: "UPI",
          merchantId: DEMO_MERCHANT_ID,
          description: "Demo payment",
        },
        crypto.randomUUID()
      );
      await load();
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setCreating(false);
    }
  }

  return (
    <div className="p-6 max-w-5xl mx-auto">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-xl font-semibold text-gray-900">Payments</h1>
        <button
          onClick={createDemoPayment}
          disabled={creating}
          className="rounded-md bg-gray-900 text-white text-sm px-4 py-2 disabled:opacity-50"
        >
          {creating ? "Creating..." : "Create Payment"}
        </button>
      </div>

      {error && <div className="mb-4 text-sm text-red-600">{error}</div>}

      <div className="rounded-lg border border-gray-200 overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 text-gray-500 text-left">
            <tr>
              <th className="px-4 py-2 font-medium">Payment ID</th>
              <th className="px-4 py-2 font-medium">Amount</th>
              <th className="px-4 py-2 font-medium">Method</th>
              <th className="px-4 py-2 font-medium">Status</th>
              <th className="px-4 py-2 font-medium">Created</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td className="px-4 py-6 text-gray-400" colSpan={5}>Loading...</td></tr>
            ) : payments.length === 0 ? (
              <tr><td className="px-4 py-6 text-gray-400" colSpan={5}>No payments yet.</td></tr>
            ) : (
              payments.map((p) => (
                <tr key={p.paymentId} className="border-t border-gray-100">
                  <td className="px-4 py-2 font-mono text-xs">{p.paymentId}</td>
                  <td className="px-4 py-2">{p.currency} {p.amount}</td>
                  <td className="px-4 py-2">{p.paymentMethod}</td>
                  <td className="px-4 py-2">
                    <span className={`px-2 py-0.5 rounded text-xs font-medium ${STATUS_COLORS[p.status] ?? "bg-gray-100 text-gray-700"}`}>
                      {p.status}
                    </span>
                  </td>
                  <td className="px-4 py-2 text-gray-500">{new Date(p.createdAt).toLocaleString()}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
