import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    room_lifecycle: {
      executor: 'ramping-vus',
      startVUs: 1,
      stages: [
        { duration: '30s', target: 20 },
        { duration: '60s', target: 50 },
        { duration: '30s', target: 0 }
      ]
    }
  }
};

const base = __ENV.API_BASE || 'http://localhost:8080';

function guest(displayName) {
  const res = http.post(`${base}/v1/auth/guest`, JSON.stringify({ displayName }), {
    headers: { 'Content-Type': 'application/json' }
  });
  check(res, { 'guest auth 200': r => r.status === 200 });
  return res.json();
}

export default function () {
  const host = guest(`load-host-${__VU}-${__ITER}`);
  const joiner = guest(`load-join-${__VU}-${__ITER}`);

  const roomRes = http.post(
    `${base}/v1/rooms`,
    JSON.stringify({ minPlayers: 2, maxPlayers: 8 }),
    {
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${host.sessionToken}`
      }
    }
  );

  check(roomRes, { 'create room 200': r => r.status === 200 });
  const roomId = roomRes.json('roomId');

  const join1 = http.post(`${base}/v1/rooms/${roomId}/join`, null, {
    headers: { Authorization: `Bearer ${host.sessionToken}` }
  });
  const join2 = http.post(`${base}/v1/rooms/${roomId}/join`, null, {
    headers: { Authorization: `Bearer ${joiner.sessionToken}` }
  });

  check(join1, { 'host join 200': r => r.status === 200 });
  check(join2, { 'joiner join 200': r => r.status === 200 });

  sleep(1);
}
