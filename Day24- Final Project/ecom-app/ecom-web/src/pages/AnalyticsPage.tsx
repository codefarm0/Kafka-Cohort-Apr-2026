import { useCallback, useEffect, useState } from 'react';
import { getDashboardMetrics } from '../api/ecomApi';
import type { DashboardMetrics } from '../api/types';

const POLL_MS = 5000;

function MetricCard({
  title,
  value,
  hint,
}: {
  title: string;
  value: string | number;
  hint?: string;
}) {
  return (
    <div className="card">
      <h3>{title}</h3>
      <p style={{ fontSize: '1.75rem', fontWeight: 700, margin: '0.25rem 0' }}>
        {value}
      </p>
      {hint ? <p className="muted">{hint}</p> : null}
    </div>
  );
}

export function AnalyticsPage() {
  const [metrics, setMetrics] = useState<DashboardMetrics | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    try {
      setError(null);
      const m = await getDashboardMetrics();
      setMetrics(m);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
    const id = window.setInterval(() => void load(), POLL_MS);
    return () => window.clearInterval(id);
  }, [load]);

  return (
    <div>
      <h1>Analytics (Kafka Streams)</h1>
      <p className="muted">
        Cumulative counters from topics <code>orders</code>, <code>payments</code>, and{' '}
        <code>deliveries</code>, refreshed every {POLL_MS / 1000}s. Values depend on{' '}
        <code>auto.offset.reset</code> and app restarts — see service README.
      </p>

      {loading && !metrics ? (
        <p className="muted">Loading metrics…</p>
      ) : null}
      {error ? (
        <p style={{ color: 'var(--danger, #c00)' }}>
          {error} — is <strong>streams-dashboard-service</strong> running on port 8087?
        </p>
      ) : null}

      {metrics ? (
        <>
          <p className="muted" style={{ marginBottom: '1rem' }}>
            Kafka Streams state: <code>{metrics.kafkaStreamsState}</code>
          </p>
          <div className="card-grid">
            <MetricCard title="Orders placed" value={metrics.ordersPlaced} />
            <MetricCard title="Payments succeeded" value={metrics.paymentsSucceeded} />
            <MetricCard title="Payments failed" value={metrics.paymentsFailed} />
            <MetricCard title="Deliveries shipped" value={metrics.deliveriesShipped} />
            <MetricCard title="Deliveries delivered" value={metrics.deliveriesDelivered} />
            <MetricCard
              title="Revenue (successful payments)"
              value={metrics.revenueTotal.toLocaleString(undefined, {
                style: 'currency',
                currency: 'USD',
              })}
              hint="Sum of data.amount on com.ecommerce.payment.processed"
            />
          </div>
        </>
      ) : null}
    </div>
  );
}
