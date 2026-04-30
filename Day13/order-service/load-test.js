import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    orders_load: {
      executor: 'constant-arrival-rate',
      rate: 10,              // 10 new orders per second
      timeUnit: '1s',
      duration: '20m',       // ~12,000 orders over 20 minutes
      preAllocatedVUs: 50,
      maxVUs: 200,
    },
  },
};
export default function () {
  const payload = JSON.stringify({
    userId: Math.floor(Math.random() * 1000),
    courseId: Math.floor(Math.random() * 200),
    amount: 99.99,
  });
  const res = http.post('http://localhost:8080/api/orders', payload, {
    headers: { 'Content-Type': 'application/json' },
  });
  check(res, {
    'status is 200': (r) => r.status === 200,
  });
  // Small pause so VUs don't hammer in a tight loop
  sleep(1);
}