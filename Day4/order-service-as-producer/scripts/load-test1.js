import http from "k6/http";
import { check, sleep } from "k6";

// ==============================
// Load Test Configuration
// ==============================
export const options = {
  stages: [
    { duration: "10s", target: 10 }, // Ramp-up to 10 users
    { duration: "30s", target: 10 }, // Stay at 10 users
    { duration: "10s", target: 50 }, // Spike to 50 users
    { duration: "20s", target: 50 }, // Sustain 50 users
    { duration: "10s", target: 0 },  // Ramp-down
  ],

  thresholds: {
    http_req_duration: ["p(95)<500"], // 95% requests < 500ms
    http_req_failed: ["rate<0.01"],   // <1% failures allowed
  },
};

// ==============================
// Base URL (Spring Boot App)
// ==============================
const BASE_URL = "http://localhost:8080/api/orders";

// ==============================
// Test Function
// ==============================
export default function () {
  // Sample OrderRequest Payload
  const payload = JSON.stringify({
    customerId: "cust-101",
    productId: "prod-5001",
    quantity: 2,
    price: 499.99,
  });

  const params = {
    headers: {
      "Content-Type": "application/json",
    },
  };

  // POST Request
  const res = http.post(BASE_URL, payload, params);

  // Validate Response
  check(res, {
    "Status is 201 Created": (r) => r.status === 201,
    "Response has orderId": (r) => JSON.parse(r.body).orderId !== undefined,
  });

  sleep(1); // simulate user think-time
}
