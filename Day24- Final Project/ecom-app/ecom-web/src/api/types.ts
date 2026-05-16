export type SearchHit = {
  productId: string;
  sku?: string;
  name: string;
  description?: string;
  categoryId?: string;
  price: number;
  availableQuantity?: number;
  snippet?: string;
};

export type Product = {
  id?: number;
  productId: string;
  sku?: string;
  productName: string;
  description?: string;
  categoryId?: string;
  price: number;
  availableQuantity: number;
  reservedQuantity?: number;
  createdAt?: string;
  updatedAt?: string;
};

export type OrderItemResponse = {
  id?: number;
  productId: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
};

export type OrderResponse = {
  id: string;
  customerId: string;
  totalAmount: number;
  status: string;
  shippingAddress?: string;
  items: OrderItemResponse[];
  createdAt?: string;
  updatedAt?: string;
};

export type DeliveryResponse = {
  deliveryId: string;
  orderId: string;
  customerId?: string;
  shippingAddress?: string;
  status: string;
  createdAt?: string;
  updatedAt?: string;
};

/** GET /api/dashboard/metrics (Kafka Streams aggregates). */
export type DashboardMetrics = {
  ordersPlaced: number;
  paymentsSucceeded: number;
  paymentsFailed: number;
  deliveriesShipped: number;
  deliveriesDelivered: number;
  revenueTotal: number;
  kafkaStreamsState: string;
};
