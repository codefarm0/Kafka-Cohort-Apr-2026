import http from "k6/http";
import { check, sleep } from "k6";

// ==============================
// Load Test Config
// ==============================
export const options = {
  vus: 20,              // 20 concurrent users
  duration: "30s",      // run for 30 seconds

  thresholds: {
    http_req_failed: ["rate<0.01"],        // <1% failures
    http_req_duration: ["p(95)<1000"],     // 95% < 1s
  },
};

// ==============================
// Target Endpoint
// ==============================
const BASE_URL = "http://localhost:8080/api/orders";

// ==============================
// Generate Large Payload (~200KB)
// ==============================
function generateLargeString(sizeKB) {
  return "X".repeat(sizeKB * 1024);
}

export default function () {
  // Heavy payload field (200KB description)
  const largeDescription = generateLargeString(200);

  // OrderRequest JSON Payload
  const payload = JSON.stringify({
    customerId: `cust-${__VU}-${__ITER}`,
    productId: `prod-${Math.floor(Math.random() * 1000)}`,
    quantity: 5,
    price: 999.99,

    // Heavy field to increase Kafka message size
    description: largeDescription,
  });

  const params = {
    headers: {
      "Content-Type": "application/json",
    },
  };

  // Send POST request
  const res = http.post(BASE_URL, payload, params);

  // Validate response
  check(res, {
    "Status is 201 Created": (r) => r.status === 201,
  });

  sleep(0.2); // small think time for high throughput
}
