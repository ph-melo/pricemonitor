const BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

function getToken() {
  return localStorage.getItem("pm_token");
}

function authHeaders() {
  const token = getToken();
  return {
    "Content-Type": "application/json",
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };
}

async function handleResponse(res) {
  if (res.status === 204) return null;

  const text = await res.text().catch(() => "");

  if (!res.ok) {
    let message = `Erro ${res.status}`;
    if (text) {
      try {
        const json = JSON.parse(text);
        message = json.message || json.error || text;
      } catch {
        message = text;
      }
    }
    const err = new Error(message);
    err.status = res.status;
    throw err;
  }

  if (!text) return null;
  try { return JSON.parse(text); } catch { return text; }
}

// ─── Auth ─────────────────────────────────────────────────────────────────────

export async function login(email, password) {
  const res = await fetch(`${BASE_URL}/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password }),
  });
  return handleResponse(res);
}

export async function register(name, email, password) {
  const res = await fetch(`${BASE_URL}/auth/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ name, email, password }),
  });
  return handleResponse(res);
}

// ─── Produtos Free/Pro ────────────────────────────────────────────────────────

export async function fetchProducts() {
  const res = await fetch(`${BASE_URL}/products`, { headers: authHeaders() });
  return handleResponse(res);
}

export async function addProduct(productUrl) {
  const res = await fetch(`${BASE_URL}/products`, {
    method: "POST",
    headers: authHeaders(),
    body: JSON.stringify({ productUrl }),
  });
  return handleResponse(res);
}

export async function deleteProduct(productId) {
  const res = await fetch(`${BASE_URL}/products/${productId}`, {
    method: "DELETE",
    headers: authHeaders(),
  });
  return handleResponse(res);
}

export async function checkProductNow(productId) {
  const res = await fetch(`${BASE_URL}/monitor/check/${productId}`, {
    method: "POST",
    headers: authHeaders(),
  });
  return handleResponse(res);
}

// ─── Enterprise ───────────────────────────────────────────────────────────────

export async function fetchEnterpriseProducts() {
  const res = await fetch(`${BASE_URL}/enterprise/products`, {
    headers: authHeaders(),
  });
  return handleResponse(res);
}

export async function addEnterpriseProduct({ ean, productName, mapPrice, tolerancePercent }) {
  const res = await fetch(`${BASE_URL}/enterprise/products`, {
    method: "POST",
    headers: authHeaders(),
    body: JSON.stringify({ ean, productName, mapPrice, tolerancePercent }),
  });
  return handleResponse(res);
}

export async function deleteEnterpriseProduct(productId) {
  const res = await fetch(`${BASE_URL}/enterprise/products/${productId}`, {
    method: "DELETE",
    headers: authHeaders(),
  });
  return handleResponse(res);
}

export async function checkEnterpriseProductNow(productId) {
  const res = await fetch(`${BASE_URL}/enterprise/products/${productId}/check`, {
    method: "POST",
    headers: authHeaders(),
  });
  return handleResponse(res);
}

export async function fetchViolations() {
  const res = await fetch(`${BASE_URL}/enterprise/violations`, {
    headers: authHeaders(),
  });
  return handleResponse(res);
}

export async function fetchUnseenViolations() {
  const res = await fetch(`${BASE_URL}/enterprise/violations/unseen`, {
    headers: authHeaders(),
  });
  return handleResponse(res);
}

export async function markViolationsAsSeen() {
  const res = await fetch(`${BASE_URL}/enterprise/violations/mark-seen`, {
    method: "POST",
    headers: authHeaders(),
  });
  return handleResponse(res);
}
