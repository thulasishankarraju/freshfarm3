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
  } else if (role === "FARMER") {
    roleLinks = `
      <a href="${root}farmer/dashboard.html">Dashboard</a>
      <a href="${root}farmer/products.html">My Products</a>`;
  } else if (role === "ADMIN") {
    roleLinks = `
      <a href="${root}admin/dashboard.html">Dashboard</a>
      <a href="${root}admin/farmers.html">Farmers</a>
      <a href="${root}admin/orders.html">Orders</a>`;
  } else if (role === "AGENT") {
    roleLinks = `<a href="${root}agent/dashboard.html">My Deliveries</a>`;
  }

  mount.innerHTML = `
    <div class="brand-strip">
      <a href="${root}index.html">
        <img src="${root}img/logo.png" alt="Fresh Farming — Empowering Farmers, Enriching Lives" class="brand-logo">
      </a>
    </div>
    <nav class="nav">
      <div class="container">
        <div class="nav-links" style="margin-left:auto;">
          <a href="${root}shop.html">Shop</a>
          ${roleLinks}
          ${loggedIn
            ? `<span class="nav-tag">${role}</span><a href="#" id="logoutLink">Logout (${session.name() || ""})</a>`
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
}

document.addEventListener("DOMContentLoaded", renderNav);
