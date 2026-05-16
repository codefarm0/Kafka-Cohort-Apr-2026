import { useEffect, useState } from 'react';
import { listDeliveries, patchDeliveryStatus } from '../api/ecomApi';
import type { DeliveryResponse } from '../api/types';

type Tab = 'AWAITING_SHIPMENT' | 'SHIPPED' | 'DELIVERED' | 'ALL';

export function DeliveryPage() {
  const [tab, setTab] = useState<Tab>('AWAITING_SHIPMENT');
  const [rows, setRows] = useState<DeliveryResponse[]>([]);
  const [err, setErr] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const refresh = () => {
    setLoading(true);
    setErr(null);
    const p =
      tab === 'ALL'
        ? listDeliveries()
        : listDeliveries(tab);
    p.then(setRows)
      .catch((e) => setErr(e instanceof Error ? e.message : 'Failed'))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    setLoading(true);
    setErr(null);
    const promise =
      tab === 'ALL' ? listDeliveries() : listDeliveries(tab);
    promise
      .then(setRows)
      .catch((e) => setErr(e instanceof Error ? e.message : 'Failed'))
      .finally(() => setLoading(false));
  }, [tab]);

  return (
    <div>
      <h1>Delivery (simulated)</h1>
      <p className="muted">
        Lists <code>GET /api/deliveries?status=…</code> and advances workflow
        with <code>PATCH /api/deliveries/:orderId/status</code> → publishes to
        Kafka <code>deliveries</code> topic.
      </p>
      <div className="tabs">
        {(
          [
            'AWAITING_SHIPMENT',
            'SHIPPED',
            'DELIVERED',
            'ALL',
          ] as const
        ).map((t) => (
          <button
            key={t}
            type="button"
            className={tab === t ? 'active' : ''}
            onClick={() => setTab(t)}
          >
            {t.replace(/_/g, ' ')}
          </button>
        ))}
        <button type="button" onClick={refresh} disabled={loading}>
          Refresh
        </button>
      </div>
      {err && <div className="error-banner">{err}</div>}
      {loading && <p className="muted">Loading…</p>}
      <table className="data">
        <thead>
          <tr>
            <th>Order</th>
            <th>Delivery</th>
            <th>Status</th>
            <th>Customer</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((r) => (
            <tr key={`${r.orderId}-${r.deliveryId}`}>
              <td>
                <code>{r.orderId}</code>
              </td>
              <td className="muted">{r.deliveryId}</td>
              <td>
                <span className="badge">{r.status}</span>
              </td>
              <td>{r.customerId}</td>
              <td>
                {r.status === 'AWAITING_SHIPMENT' && (
                  <button
                    type="button"
                    className="primary"
                    onClick={async () => {
                      try {
                        await patchDeliveryStatus(r.orderId, 'SHIPPED');
                        refresh();
                      } catch (e) {
                        alert(
                          e instanceof Error ? e.message : 'Mark shipped failed'
                        );
                      }
                    }}
                  >
                    Mark SHIPPED
                  </button>
                )}
                {r.status === 'SHIPPED' && (
                  <button
                    type="button"
                    className="primary"
                    onClick={async () => {
                      try {
                        await patchDeliveryStatus(r.orderId, 'DELIVERED');
                        refresh();
                      } catch (e) {
                        alert(
                          e instanceof Error
                            ? e.message
                            : 'Mark delivered failed'
                        );
                      }
                    }}
                  >
                    Mark DELIVERED
                  </button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {!loading && rows.length === 0 && (
        <p className="muted">No rows — pay an order first so a shipment is created.</p>
      )}
    </div>
  );
}
