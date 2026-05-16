package in.codefarm.streams_dashboard.web;

/**
 * Stable JSON contract for the React analytics page.
 */
public record DashboardMetricsResponse(
    long ordersPlaced,
    long paymentsSucceeded,
    long paymentsFailed,
    long deliveriesShipped,
    long deliveriesDelivered,
    double revenueTotal,
    String kafkaStreamsState
) {
    public static DashboardMetricsResponse empty(String state) {
        return new DashboardMetricsResponse(0, 0, 0, 0, 0, 0.0, state);
    }
}
