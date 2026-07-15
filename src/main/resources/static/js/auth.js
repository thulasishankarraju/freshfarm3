/* ===========================================================
   FarmFresh — session helpers
   Stores exactly what AuthResponse gives back after login/register.
=========================================================== */

const session = {
  save(auth) {
    localStorage.setItem("ff_token", auth.token);
    localStorage.setItem("ff_role", auth.role);
    localStorage.setItem("ff_name", auth.fullName || "");
    localStorage.setItem("ff_email", auth.email || "");
    localStorage.setItem("ff_userId", auth.userId ?? "");
  },
  clear() {
    ["ff_token", "ff_role", "ff_name", "ff_email", "ff_userId"].forEach(k => localStorage.removeItem(k));
  },
  isLoggedIn() {
    return !!localStorage.getItem("ff_token");
  },
  role() {
    return localStorage.getItem("ff_role");
  },
  name() {
    return localStorage.getItem("ff_name");
  },
  logout(redirectTo = "index.html") {
    this.clear();
    window.location.href = redirectTo;
  }
};

/**
 * Call at the top of any page that requires login.
 * `allowedRoles` — array of roles allowed on this page, e.g. ["BUYER"].
 * Pass [] to only require "logged in, any role".
 * Redirects to login.html if not authenticated / wrong role.
 */
function requireAuth(allowedRoles = []) {
  if (!session.isLoggedIn()) {
    window.location.href = rootPath() + "login.html";
    return false;
  }
  if (allowedRoles.length && !allowedRoles.includes(session.role())) {
    toast("You don't have access to that page.", "err");
    window.location.href = rootPath() + "index.html";
    return false;
  }
  return true;
}

/** Returns "" at site root, "../" when inside /shop, /admin, /agent subfolders. */
function rootPath() {
  const p = window.location.pathname;
  return /\/(shop|admin|agent)\//.test(p) ? "../" : "";
}
