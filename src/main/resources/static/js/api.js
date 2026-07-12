/* ===========================================================
   FarmFresh — API layer
   Talks to the Spring Boot backend running on localhost:8080.
   Every other JS file calls the `api` object below — nothing
   else in this app builds a fetch() URL by hand.
=========================================================== */

const API_BASE = "http://localhost:8080";

function authHeaders(extra = {}) {
  const token = localStorage.getItem("ff_token");
  return token
      ? { ...extra, Authorization: `Bearer ${token}` }
      : { ...extra };
}

/**
 * Core request helper.
 * - Prefixes API_BASE
 * - Adds Authorization header automatically if a token is stored
 * - Parses JSON responses
 * - On non-2xx, throws an Error whose message is the backend's
 *   `message` field (the backend's GlobalExceptionHandler always
 *   returns { timestamp, status, error, message }).
 * - NOTE: because of a backend quirk, @Valid field-validation errors
 *   (e.g. missing required field) are NOT returned with per-field
 *   detail — they currently fall through to a generic 500 message.
 *   This wrapper surfaces whatever message the backend sends either way.
 */
async function request(path, { method = "GET", body, isForm = false, auth = true } = {}) {
  const opts = { method, headers: {} };

  if (auth) opts.headers = authHeaders(opts.headers);

  if (body !== undefined) {
    if (isForm) {
      opts.body = body; // FormData — browser sets multipart boundary itself
    } else {
      opts.headers["Content-Type"] = "application/json";
      opts.body = JSON.stringify(body);
    }
  }

  let res;
  try {
    res = await fetch(`${API_BASE}${path}`, opts);
  } catch (networkErr) {
    throw new Error(
        "Can't reach the backend at " + API_BASE +
        ". Make sure the Spring Boot app is running locally on port 8080."
    );
  }

  const contentType = res.headers.get("content-type") || "";
  const data = contentType.includes("application/json")
      ? await res.json().catch(() => null)
      : null;

  if (!res.ok) {
    const msg = (data && (data.message || data.error)) || `Request failed (${res.status})`;
    const err = new Error(msg);
    err.status = res.status;
    throw err;
  }
  return data;
}

/** Turns a relative "/uploads/xxx.jpg" path from the backend into a full URL. */
function assetUrl(path) {
  if (!path) return "";
  return path.startsWith("http") ? path : `${API_BASE}${path}`;
}

/**
 * Builds the FormData for POST /api/products, matching the backend's
 * @RequestPart("product") ProductRequest + @RequestPart("images") List<MultipartFile>.
 * The "product" part MUST be sent as an application/json blob, not a plain field.
 */
function buildProductFormData(productRequest, files) {
  const fd = new FormData();
  fd.append("product", new Blob([JSON.stringify(productRequest)], { type: "application/json" }));
  (files || []).forEach(f => fd.append("images", f));
  return fd;
}

const api = {
  // ---- Auth ----
  registerBuyer: (payload) => request("/api/auth/register/buyer", { method: "POST", body: payload, auth: false }),
  registerFarmer: (payload) => request("/api/auth/register/farmer", { method: "POST", body: payload, auth: false }),
  login: (payload) => request("/api/auth/login", { method: "POST", body: payload, auth: false }),
  agentRegister: (payload) => request("/api/auth/agent/register", { method: "POST", body: payload, auth: false }),
  agentLogin: (payload) => request("/api/auth/agent/login", { method: "POST", body: payload, auth: false }),
  agentProfile: () => request("/api/auth/agent/profile"),

  // ---- Registration OTP (must verify BEFORE calling registerBuyer/Farmer/agentRegister) ----
  // channel must be "EMAIL" or "PHONE" (not "SMS") — that's the exact string the backend's OtpChannel enum expects.
  // For PHONE, recipient must be in "+91XXXXXXXXXX" format (backend requires the country code for sending).
  registerSendOtp: (payload) => request("/api/auth/register/send-otp", { method: "POST", body: payload, auth: false }),
  registerVerifyOtp: (payload) => request("/api/auth/register/verify-otp", { method: "POST", body: payload, auth: false }),

  // ---- Forgot password ----
  forgotPasswordSendOtp: (payload) => request("/api/auth/forgot-password/send-otp", { method: "POST", body: payload, auth: false }),
  resetPassword: (payload) => request("/api/auth/forgot-password/reset", { method: "POST", body: payload, auth: false }),

  // ---- Categories ----
  listCategories: () => request("/api/categories", { auth: false }),

  // ---- Products (public GETs, farmer-only writes) ----
  listProducts: (params = "") => request(`/api/products${params}`, { auth: false }),
  getProduct: (id) => request(`/api/products/${id}`, { auth: false }),
  searchProducts: (q) => request(`/api/products/search?keyword=${encodeURIComponent(q)}`, { auth: false }),
  productsByCategory: (categoryId) => request(`/api/products/category/${categoryId}`, { auth: false }),
  myProducts: () => request("/api/products/my-products"),
  // NOTE: backend expects a multipart request with a part named "product"
  // (JSON) and 0+ parts named "images" (files) — see buildProductFormData().
  createProduct: (formData) => request("/api/products", { method: "POST", body: formData, isForm: true }),
  updateProduct: (id, payload) => request(`/api/products/${id}`, { method: "PUT", body: payload }),
  deleteProduct: (id) => request(`/api/products/${id}`, { method: "DELETE" }),

  // ---- Farmer ----
  farmerDashboard: () => request("/api/farmer/dashboard"),
  farmerProducts: () => request("/api/farmer/products"),

  // ---- Cart ----
  getCart: () => request("/api/cart"),
  addToCart: (payload) => request("/api/cart/add", { method: "POST", body: payload }),
  // NOTE: CartController takes quantity as a @RequestParam, not a JSON body.
  updateCartItem: (cartItemId, quantity) => request(`/api/cart/items/${cartItemId}?quantity=${quantity}`, { method: "PUT" }),
  removeCartItem: (cartItemId) => request(`/api/cart/items/${cartItemId}`, { method: "DELETE" }),
  clearCart: () => request("/api/cart/clear", { method: "DELETE" }),

  // ---- Addresses ----
  // Added alongside a new backend AddressController that fills a gap
  // SecurityConfig had reserved ("/api/addresses/**") but nothing implemented.
  createAddress: (payload) => request("/api/addresses", { method: "POST", body: payload }),
  myAddresses: () => request("/api/addresses/my-addresses"),
  getAddress: (id) => request(`/api/addresses/${id}`),
  updateAddress: (id, payload) => request(`/api/addresses/${id}`, { method: "PUT", body: payload }),
  deleteAddress: (id) => request(`/api/addresses/${id}`, { method: "DELETE" }),

  // ---- Orders ----
  checkout: (payload) => request("/api/orders/checkout", { method: "POST", body: payload }),
  myOrders: () => request("/api/orders/my-orders"),
  getOrder: (id) => request(`/api/orders/${id}`),
  cancelOrder: (id) => request(`/api/orders/${id}/cancel`, { method: "PUT" }),
  farmerOrders: () => request("/api/orders/farmer/my-orders"),
  allOrdersAdmin: () => request("/api/orders/admin/all"),

  // ---- Payments ----
  createPaymentOrder: (payload) => request("/api/payments/create-order", { method: "POST", body: payload }),
  verifyPayment: (payload) => request("/api/payments/verify", { method: "POST", body: payload }),
  getPayment: (orderId) => request(`/api/payments/${orderId}`),
  // NOTE: refund takes orderId as a @RequestParam, not a JSON body.
  refundPayment: (orderId) => request(`/api/payments/refund?orderId=${orderId}`, { method: "POST" }),

  // ---- Delivery ----
  assignDelivery: (payload) => request("/api/delivery/assign", { method: "POST", body: payload }),
  // Each action now targets a specific delivery by ID, so an agent with
  // multiple assigned deliveries can act on any of them independently.
  pickupDelivery: (deliveryId) => request(`/api/delivery/${deliveryId}/pickup`, { method: "PUT" }),
  outForDelivery: (deliveryId) => request(`/api/delivery/${deliveryId}/out-for-delivery`, { method: "PUT" }),
  completeDelivery: (deliveryId, otp) => request(`/api/delivery/${deliveryId}/complete?otp=${encodeURIComponent(otp)}`, { method: "PUT" }),
  myDeliveries: () => request("/api/delivery/my-deliveries"),
  trackDelivery: (orderId) => request(`/api/delivery/track/${orderId}`),

  // ---- Notifications ----
  myNotifications: () => request("/api/notifications"),
  unreadNotificationCount: () => request("/api/notifications/unread-count"),
  markNotificationRead: (id) => request(`/api/notifications/${id}/read`, { method: "PUT" }),
  markAllNotificationsRead: () => request("/api/notifications/read-all", { method: "PUT" }),

  // ---- Reviews ----
  createReview: (payload) => request("/api/reviews", { method: "POST", body: payload }),
  productReviews: (productId) => request(`/api/reviews/product/${productId}`, { auth: false }),
  productReviewSummary: (productId) => request(`/api/reviews/product/${productId}/summary`, { auth: false }),
  farmerReviews: (farmerId) => request(`/api/reviews/farmer/${farmerId}`, { auth: false }),
  myReviews: () => request("/api/reviews/my-reviews"),

  // ---- Subscriptions ----
  createSubscription: (payload) => request("/api/subscriptions", { method: "POST", body: payload }),
  pauseSubscription: (id) => request(`/api/subscriptions/${id}/pause`, { method: "PUT" }),
  resumeSubscription: (id) => request(`/api/subscriptions/${id}/resume`, { method: "PUT" }),
  deleteSubscription: (id) => request(`/api/subscriptions/${id}`, { method: "DELETE" }),
  mySubscriptions: () => request("/api/subscriptions/my-subscriptions"),

  // ---- Admin ----
  adminDashboard: () => request("/api/admin/dashboard"),
  adminStatistics: () => request("/api/admin/statistics"),
  pendingFarmers: () => request("/api/admin/farmers/pending"),
  approveFarmer: (id) => request(`/api/admin/farmers/${id}/approve`, { method: "PUT" }),
  rejectFarmer: (id) => request(`/api/admin/farmers/${id}/reject`, { method: "PUT" }),
  adminOrders: () => request("/api/admin/orders"),
  adminConfirmOrder: (id) => request(`/api/admin/orders/${id}/confirm`, { method: "PUT" }),
};