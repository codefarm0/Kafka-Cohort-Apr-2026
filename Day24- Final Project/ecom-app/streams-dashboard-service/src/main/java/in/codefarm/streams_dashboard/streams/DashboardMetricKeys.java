package in.codefarm.streams_dashboard.streams;

/**
 * Keys in the {@code dashboard-metrics} KeyValueStore (String → Long counts; revenue in cents).
 */
public final class DashboardMetricKeys {

    public static final String ORDERS_PLACED = "orders_placed";
    public static final String PAYMENTS_SUCCEEDED = "payments_succeeded";
    public static final String PAYMENTS_FAILED = "payments_failed";
    public static final String DELIVERIES_SHIPPED = "deliveries_shipped";
    public static final String DELIVERIES_DELIVERED = "deliveries_delivered";
    /** Cumulative successful payment amounts × 100 (integer cents). */
    public static final String REVENUE_CENTS = "revenue_cents";

    private DashboardMetricKeys() {}
}
