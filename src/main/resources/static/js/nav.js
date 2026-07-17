/* ===========================================================
   FarmFresh — nav bar + toast helper, shared across all pages.
   Each page needs a <div id="nav"></div> and <div id="toast-root"></div>.
=========================================================== */

function toast(message, kind = "ok") {
  const root = document.getElementById("toast-root");
  if (!root) { alert(message); return; }
  const el = document.createElement("div");
  el.className = `toast ${kind === "err" ? "err" : "ok"}`;
  el.textContent = message;
  root.appendChild(el);
  setTimeout(() => el.remove(), 3800);
}

function money(n) {
  const num = Number(n || 0);
  return "₹" + num.toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

/* Formats a cart/order line-item quantity, e.g.
   formatQty(2, "kg")   -> "2 kg"
   formatQty(1.5, "L")  -> "1.5 L"
   formatQty(3)         -> "3"          (no unit passed) */
function formatQty(qty, unit = "") {
  const num = Number(qty || 0);
  const display = Number.isInteger(num)
      ? num.toString()
      : num.toString().replace(/0+$/, "").replace(/\.$/, "");
  return unit ? `${display} ${unit}` : display;
}

function renderNav() {
  const mount = document.getElementById("nav");
  if (!mount) return;
  const root = rootPath();
  const role = session.role();
  const loggedIn = session.isLoggedIn();

  let roleLinks = "";
  if (role === "BUYER") {
    roleLinks = `
      <a href="${root}cart.html">Cart</a>
      <a href="${root}orders.html">My Orders</a>
      <a href="${root}subscriptions.html">Subscriptions</a>`;
  } else if (role === "SHOP") {
    roleLinks = `
      <a href="${root}shop/dashboard.html">Dashboard</a>
      <a href="${root}shop/products.html">My Products</a>`;
  } else if (role === "ADMIN") {
    roleLinks = `
      <a href="${root}admin/dashboard.html">Dashboard</a>
      <a href="${root}admin/shops.html">Shops</a>
      <a href="${root}admin/orders.html">Orders</a>`;
  } else if (role === "AGENT") {
    roleLinks = `<a href="${root}agent/dashboard.html">My Deliveries</a>`;
  }

  mount.innerHTML = `
    <div class="brand-strip">
      <a href="${root}index.html">
        <img src="${root}img/logo.png" alt="Fresh Farming — Empowering Shops, Enriching Lives" class="brand-logo">
      </a>
    </div>
    <nav class="nav">
      <div class="container">
        <div class="nav-links" style="margin-left:auto;">
          <a href="${root}shop.html">Shop</a>
          ${roleLinks}
          ${loggedIn ? `
            <div class="notif-wrap" id="notifWrap">
              <button class="notif-bell" id="notifBell" title="Notifications" aria-label="Notifications">
                🔔<span class="notif-count" id="notifCount" style="display:none;">0</span>
              </button>
            </div>
            <span class="nav-tag">${role}</span><a href="#" id="logoutLink">Logout (${session.name() || ""})</a>`
      : `<a href="${root}login.html">Login</a><a href="${root}register.html">Register</a>`
  }
        </div>
      </div>
    </nav>`;

  const logoutLink = document.getElementById("logoutLink");
  if (logoutLink) {
    logoutLink.addEventListener("click", (e) => {
      e.preventDefault();
      session.logout(root + "index.html");
    });
  }

  if (loggedIn) initNotifications();
}

/* ===========================================================
   Notification bell — polls unread count, opens a dropdown with
   the full feed on click, marks items read on click / "mark all".
   Used by every logged-in page (buyer/shop/admin/agent) so
   things like "delivery agent assigned", "order out for delivery",
   and "order delivered" actually surface in the UI, not just email/SMS.
=========================================================== */
let _notifPollTimer = null;

function initNotifications() {
  refreshNotifCount();
  if (_notifPollTimer) clearInterval(_notifPollTimer);
  _notifPollTimer = setInterval(refreshNotifCount, 25000);

  const bell = document.getElementById("notifBell");
  if (!bell) return;
  bell.addEventListener("click", (e) => {
    e.stopPropagation();
    const wrap = document.getElementById("notifWrap");
    const existing = document.getElementById("notifDropdown");
    if (existing) { existing.remove(); return; }
    openNotifDropdown(wrap);
  });

  document.addEventListener("click", (e) => {
    const dd = document.getElementById("notifDropdown");
    const wrap = document.getElementById("notifWrap");
    if (dd && wrap && !wrap.contains(e.target)) dd.remove();
  });
}

async function refreshNotifCount() {
  const countEl = document.getElementById("notifCount");
  if (!countEl) return;
  try {
    const { unread } = await api.unreadNotificationCount();
    if (unread > 0) {
      countEl.textContent = unread > 99 ? "99+" : unread;
      countEl.style.display = "inline-block";
    } else {
      countEl.style.display = "none";
    }
  } catch (e) { /* not logged in / expired token — ignore silently */ }
}

async function openNotifDropdown(wrap) {
  const dd = document.createElement("div");
  dd.className = "notif-dropdown";
  dd.id = "notifDropdown";
  dd.innerHTML = `
    <div class="notif-dropdown-header">
      <strong>Notifications</strong>
      <button class="notif-mark-all" id="notifMarkAll">Mark all read</button>
    </div>
    <div id="notifList"><div class="notif-empty">Loading…</div></div>`;
  wrap.appendChild(dd);

  document.getElementById("notifMarkAll").addEventListener("click", async (e) => {
    e.stopPropagation();
    try {
      await api.markAllNotificationsRead();
      await refreshNotifCount();
      renderNotifList(await api.myNotifications());
    } catch (err) { toast(err.message, "err"); }
  });

  try {
    const items = await api.myNotifications();
    renderNotifList(items);
  } catch (e) {
    document.getElementById("notifList").innerHTML =
        `<div class="notif-empty">Couldn't load notifications.</div>`;
  }
}

function renderNotifList(items) {
  const list = document.getElementById("notifList");
  if (!list) return;
  if (!items.length) {
    list.innerHTML = `<div class="notif-empty">No notifications yet.</div>`;
    return;
  }
  list.innerHTML = items.map(n => `
    <div class="notif-item ${n.isRead ? "" : "unread"}" data-id="${n.id}">
      <div class="notif-title">${n.title}</div>
      <div class="notif-msg">${n.message}</div>
      <div class="notif-time">${n.createdAt ? new Date(n.createdAt).toLocaleString() : ""}</div>
    </div>`).join("");

  list.querySelectorAll(".notif-item").forEach(el => {
    el.addEventListener("click", async () => {
      const id = el.dataset.id;
      if (el.classList.contains("unread")) {
        try {
          await api.markNotificationRead(id);
          el.classList.remove("unread");
          refreshNotifCount();
        } catch (e) { /* ignore */ }
      }
    });
  });
}

document.addEventListener("DOMContentLoaded", renderNav);