# FarmFresh Frontend

A plain HTML/CSS/JS frontend built to match the `freshfarm3` Spring Boot backend **exactly as it actually behaves** — every request shape below was verified against the real controller code, not assumed from convention.

## 1. Run the backend first

```
cd freshfarm3-main
./mvnw spring-boot:run
```
- Needs a local MySQL 8 instance with a `farmfresh3_db` database (create it: `CREATE DATABASE farmfresh3_db;`) matching the credentials in `application.properties`.
- Backend runs on **http://localhost:8080**.
- An admin account is auto-seeded on first boot: `admin@farmfresh.com` / `Admin@123`.

## 2. Serve this frontend on an allowed port

The backend's `SecurityConfig` only allows CORS from these origins — **you must serve this folder from one of them, or every request will be blocked by the browser**:
```
http://localhost:5173
http://localhost:3000
http://127.0.0.1:5501
http://localhost:5501
```
Easiest option with VS Code's **Live Server** extension: open its settings and set `Live Server: Port` to `5501` before clicking "Go Live". Or with Node:
```
npx http-server . -p 5501
```
Then open `http://localhost:5501/index.html`.

## 3. Known backend gaps this frontend works around

These aren't frontend bugs — they're missing pieces in the backend itself:

| Gap | Where it shows up | Workaround used here |
|---|---|---|
| ~~No `AddressController`~~ — **fixed**: added `AddressController` + `AddressService` + `AddressRequest`/`AddressResponse` to the backend (`POST/GET/PUT/DELETE /api/addresses`), matching the `BUYER`-only rule `SecurityConfig` already reserved for this path. | `checkout.html` | Checkout now lists saved addresses and lets you add new ones right from the UI — no manual SQL needed. **You must copy the 3 new backend files into your project and rebuild** (see §3.1 below) for this to work. |
| No `CategoryController`, despite `SecurityConfig` permitting `GET /api/categories/**` | `shop.html`, `shop/products.html` | Category dropdown on the shop page is built by reading `categoryId`/`categoryName` off whatever products come back. Shops must type a numeric category ID directly when creating a product. |
| No `CouponController`, despite `CouponRequest`/`CouponValidateRequest` DTOs existing | `checkout.html` | The coupon code field is accepted and sent with checkout (the backend DTO allows it) but there's no way to validate/preview a discount before placing the order. |
| Razorpay `key-id` is never exposed by any endpoint (it's server-side only) | `checkout.html` | You must hardcode a matching **test** key in `checkout.html`'s `RAZORPAY_KEY_ID` constant, and set the same key in the backend's `application.properties` (`razorpay.key-id`). Cash-on-delivery works with zero setup. |
| `@Valid` validation errors (missing/invalid fields) aren't given their own exception handler, so they currently surface as a generic 500 "Something went wrong" instead of field-level messages | Every form | Forms use HTML5 `required`/`pattern`/`minlength` to catch obvious mistakes client-side before they ever hit that backend behavior. |
| `PUT /api/cart/items/{id}` takes `quantity` as a query param, not a JSON body | `cart.html` | Handled correctly in `js/api.js` — noted here so you don't "fix" it back to a body if you change the backend. |
| Agent `pickup` / `out-for-delivery` / `complete` act on the agent's single current delivery — no order ID is ever passed | `agent/dashboard.html` | The UI only shows action buttons on the first non-delivered delivery in the list, since that's the one the backend will actually update. |

### 3.1 Applying the AddressController backend fix

Three new files need to be dropped into your backend project (not included in this frontend zip — ask for the backend patch if you don't have them yet):
```
src/main/java/com/example/freshfarm3/dto/request/AddressRequest.java
src/main/java/com/example/freshfarm3/dto/response/AddressResponse.java
src/main/java/com/example/freshfarm3/service/AddressService.java
src/main/java/com/example/freshfarm3/controller/AddressController.java
```
They follow the exact same conventions already used elsewhere in the codebase (Lombok `@Builder`, `AppUserDetails`/email lookup pattern from `CartService`, `ResourceNotFoundException`/`UnauthorizedException`). No changes to `SecurityConfig` are needed — it already reserves `/api/addresses/**` for `BUYER`. Restart the backend after adding them.

## 3.5 New: OTP-gated registration & forgot password (backend patch required)

This required backend changes too, since the OTP building blocks (`OtpService`,
`SendOtpRequest`/`VerifyOtpRequest`/`ResetPasswordRequest`) existed in the code but
were never wired to any controller endpoint. Ask for the `AuthController`/`AuthService`
patch alongside this frontend if you don't already have it applied — 4 new endpoints:

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/auth/register/send-otp` | Step 1 of registration — send a code to email or phone |
| POST | `/api/auth/register/verify-otp` | Step 2 — must succeed before `/register/buyer`, `/register/shop`, or `/api/auth/agent/register` will accept that email/phone |
| POST | `/api/auth/forgot-password/send-otp` | Step 1 of reset — send a code to a registered email or phone |
| POST | `/api/auth/forgot-password/reset` | Step 2 — verify the code and set the new password in one call |

**Important format detail:** the backend's `OtpChannel` enum only accepts `"EMAIL"` or
`"PHONE"` (not `"SMS"`) as the `channel` value, and phone recipients must be sent as
`+91XXXXXXXXXX` (with country code) for the OTP to actually dispatch via Twilio — even
though the rest of the app stores/validates phone numbers as bare 10-digit strings
everywhere else. The frontend handles this conversion automatically (adds `+91` before
calling the OTP endpoints, strips it back off before filling the actual registration
form), but keep it in mind if you call these endpoints directly.

## 4. Folder structure

```
index.html              landing page
login.html              buyer/shop/admin login + agent login tab + forgot-password link
register.html           buyer/shop/agent registration — each gated behind OTP verification first
forgot-password.html    choose email/SMS → verify OTP → set new password
shop.html               public product listing, search, category filter
product-detail.html     single product, reviews, add-to-cart, subscribe
cart.html               cart view/edit
checkout.html           address + payment method + Razorpay/COD
order-success.html      confirmation after checkout
orders.html             buyer order history, cancel, review, track
subscriptions.html      buyer subscription management
shop/dashboard.html   shop product-count stats
shop/products.html    shop product CRUD (multipart image upload)
admin/dashboard.html    platform-wide stats + pending shop approvals
admin/shops.html      dedicated shop approval queue
admin/orders.html       all orders + delivery assignment + refunds
agent/dashboard.html    agent's assigned deliveries + status actions
css/style.css           shared design tokens & components
js/api.js               single source of truth for every API call + base URL
js/auth.js              session storage + route guarding
js/nav.js               shared nav bar + toast notifications
```

## 5. Roles at a glance

| Role | Entry point | Guarded pages |
|---|---|---|
| Buyer | `register.html` → Buyer tab | cart, checkout, orders, subscriptions |
| Shop | `register.html` → Shop tab (needs admin approval for listings) | shop/dashboard, shop/products |
| Delivery Agent | `register.html` → Delivery Agent tab | agent/dashboard |
| Admin | seeded automatically, log in via `login.html` | admin/dashboard, admin/shops, admin/orders |
