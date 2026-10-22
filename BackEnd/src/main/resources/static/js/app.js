"use strict";

const CART_STORAGE_KEY = "cei-pass-demo-cart";
const LAST_QR_ACCOUNT_KEY = "cei-pass-last-qr-account";
const QR_DATABASE_NAME = "cei-pass-private-cache";
const QR_STORE_NAME = "issued-qr";
const QR_ENDPOINT = "/api/v1/me/qr";
const HISTORY_ENDPOINT = "/api/v1/me/usage-history";
const PASSES_ENDPOINT = "/api/v1/catalog/passes";
const ATTRACTIONS_ENDPOINT = "/api/v1/catalog/attractions";
const CHECKOUT_ENDPOINT = "/api/v1/me/orders";

let passes = [];
let attractions = [];
let isCatalogLoaded = false;
let catalogLoadError = false;

const appContent = document.querySelector("#app-content");
const cartCount = document.querySelector("#cart-count");
const announcer = document.querySelector("#app-announcer");
const apiBaseUrl = window.CEIPASS_CONFIG?.apiBaseUrl;
let profileMode = "login";
let qrRefreshTimer;
let profileFeedback = "";

class ApiError extends Error {
  constructor(status, message = `The service returned HTTP ${status}.`) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

function escapeHtml(value) {
  return String(value).replace(/[&<>"']/g, (character) => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': "&quot;",
    "'": "&#39;"
  })[character]);
}

function formatPrice(amount) {
  return new Intl.NumberFormat("en-US", { style: "currency", currency: "THB", maximumFractionDigits: 0 }).format(amount);
}

function apiUrl(path) {
  if (!apiBaseUrl) throw new Error("The API address is not configured.");
  return `${apiBaseUrl.replace(/\/+$/, "")}${path}`;
}

async function requestJson(path, options = {}) {
  const response = await fetch(apiUrl(path), {
    ...options,
    headers: { Accept: "application/json", ...options.headers },
    credentials: "include",
    signal: options.signal || AbortSignal.timeout(8000)
  });
  if (!response.ok) {
    let message = `The service returned HTTP ${response.status}.`;
    try {
      const body = await response.json();
      if (typeof body.message === "string") message = body.message;
    } catch (e) {}
    throw new ApiError(response.status, message);
  }
  return response.json();
}

async function fetchCatalogData() {
  if (isCatalogLoaded) return;
  try {
    const [fetchedPasses, fetchedAttractions] = await Promise.all([
      requestJson(PASSES_ENDPOINT),
      requestJson(ATTRACTIONS_ENDPOINT)
    ]);
    passes = fetchedPasses.map((pass, index) => {
      const tags = ["Standard", "Explorer", "Family"];
      return { ...pass, name: pass.displayName, detail: pass.description, tag: tags[index % tags.length] };
    });
    const tones = ["rose", "ink", "leaf"];
    attractions = fetchedAttractions.map((attr, index) => {
      const words = attr.name.split(" ");
      const initials = words.length > 1 ? words[0][0] + words[1][0] : words[0].slice(0, 2).toUpperCase();
      return { ...attr, category: `${attr.category} · ${attr.location}`, initials, tone: tones[index % tones.length], description: attr.description || "Explore Chiang Rai." };
    });
    isCatalogLoaded = true;
    catalogLoadError = false;
  } catch (error) {
    catalogLoadError = true;
  }
}

// ตะกร้าแบบใหม่เก็บเป็น Array of Objects
function readCart() {
  try {
    const value = JSON.parse(localStorage.getItem(CART_STORAGE_KEY) || "[]");
    return Array.isArray(value) ? value : [];
  } catch (error) { return []; }
}

let cart = readCart();

function persistCart() {
  localStorage.setItem(CART_STORAGE_KEY, JSON.stringify(cart));
  updateCartCount();
}

function updateCartCount() {
  const count = cart.reduce((total, item) => total + item.quantity, 0);
  cartCount.textContent = String(count);
  cartCount.hidden = count === 0;
}

function announce(message) { announcer.textContent = message; }
function currentPage() {
  const page = window.location.hash.slice(1).split("?")[0];
  return ["home", "buy", "qr", "history", "profile","verify"].includes(page) ? page : "home";
}

function pageHeading(eyebrow, title, description = "") {
  return `<div class="page-heading"><p class="eyebrow">${eyebrow}</p><h1>${title}</h1>${description ? `<p class="page-description">${description}</p>` : ""}</div>`;
}

function emptyCatalogState() {
  return `<div class="empty-state"><span class="empty-icon" aria-hidden="true">📡</span><h2>Catalog is unavailable</h2><p>Check backend API and Database.</p><button type="button" class="button button-outline" onclick="location.reload()">Retry Connection</button></div>`;
}

function attractionCard(attraction, compact = false) {
  return `<article class="attraction-card${compact ? " attraction-card-compact" : ""}">
    <div class="attraction-art ${attraction.tone}" aria-hidden="true"><span>${attraction.initials}</span></div>
    <div class="attraction-info"><p class="card-kicker">${escapeHtml(attraction.category)}</p><h3>${escapeHtml(attraction.name)}</h3>
    ${compact ? "" : `<p>${escapeHtml(attraction.description)}</p>`}<span class="partner-label"><span class="partner-dot"></span>Pass partner</span></div></article>`;
}

function homePage() {
  if (catalogLoadError || (isCatalogLoaded && attractions.length === 0)) return `<section class="home-page">${emptyCatalogState()}</section>`;
  const featured = attractions.slice(0, 3).map((attr) => attractionCard(attr, true)).join("");
  return `<section class="home-page">
    <div class="hero-card"><div class="hero-copy"><p class="eyebrow eyebrow-light">Your Chiang Rai adventure</p><h1>See more.<br>Worry less.</h1>
    <a class="button button-light" href="#buy">Explore passes <span aria-hidden="true">→</span></a></div></div>
    <section class="section-block quick-actions" aria-label="Quick access">
      <a class="quick-card" href="#buy"><span class="quick-icon quick-icon-green" aria-hidden="true">🎟️</span><span><strong>Buy a pass</strong><small>Find your way to more</small></span></a>
      <a class="quick-card" href="#qr"><span class="quick-icon quick-icon-amber" aria-hidden="true">📱</span><span><strong>My QR pass</strong><small>Ready when you are</small></span></a>
    </section>
    <section class="section-block"><div class="section-title-row"><div><p class="eyebrow">Hand-picked for you</p><h2>Explore Chiang Rai</h2></div></div>
    <div class="attraction-list">${featured}</div></section></section>`;
}

function cartDetails() {
  if (cart.length === 0) return `<div class="cart-empty"><span class="cart-empty-icon" aria-hidden="true">+</span><strong>Your cart is waiting</strong></div>`;
  
  let total = 0;
  const lines = cart.map((item, index) => {
    const pass = passes.find(p => p.id === item.passId);
    if (!pass) return "";
    const start = new Date(item.startDate);
    const end = new Date(item.endDate);
    const days = Math.round((end - start) / 86400000) + 1;
    const itemTotal = pass.price * days * item.quantity;
    total += itemTotal;
    
    return `<div class="cart-line">
      <div class="cart-line-main"><strong>${escapeHtml(pass.name)}</strong>
      <span style="font-size: 0.85rem; color: #5c5c5c;">${item.startDate} to ${item.endDate} (${days} days)</span>
      <span>${formatPrice(pass.price)} / day</span></div>
      <div class="quantity-control">
        <button type="button" data-action="update-cart" data-index="${index}" data-amount="-1">−</button>
        <span>${item.quantity}</span>
        <button type="button" data-action="update-cart" data-index="${index}" data-amount="1">+</button>
      </div></div>`;
  }).join("");

  return `<div class="cart-lines">${lines}<div class="cart-total"><span>Total</span><strong>${formatPrice(total)}</strong></div>
    <button type="button" class="button button-dark button-full" data-action="checkout">Confirm & Checkout <span aria-hidden="true">→</span></button></div>`;
}

function buyPage() {
  if (catalogLoadError || (isCatalogLoaded && passes.length === 0)) return `<section>${pageHeading("Find your pass", "Choose your way to explore.")}${emptyCatalogState()}</section>`;
  
  const todayDate = new Date();
  const todayStr = new Date(todayDate.getTime() - (todayDate.getTimezoneOffset() * 60000)).toISOString().split('T')[0];

  return `<section>
    ${pageHeading("Find your pass", "Choose your way to explore.", "Pick a pass and dates for your trip.")}
    
    <div class="notice-card" style="margin-bottom:1.5rem; background:#fff; border:1px solid #e1e3db;">
       <p style="font-weight:600; margin-bottom:0.5rem;">1. Select your travel dates</p>
       <div style="display:flex; gap:1rem; align-items:center;">
          <label style="flex:1; font-size:0.875rem;">Start: <input type="date" id="trip-start" style="width:100%; padding:0.5rem; border:1px solid #ccc; border-radius:4px;" min="${todayStr}"></label>
          <label style="flex:1; font-size:0.875rem;">End: <input type="date" id="trip-end" style="width:100%; padding:0.5rem; border:1px solid #ccc; border-radius:4px;"></label>
       </div>
    </div>

    <div class="pass-list">
      ${passes.map((pass, index) => `<article class="pass-card">
        <div class="pass-art pass-art-${index % 3 + 1}" aria-hidden="true"><span>${pass.name.substring(0,3).toUpperCase()}</span></div>
        <div class="pass-details"><span class="pass-tag">${escapeHtml(pass.tag)}</span><h2>${escapeHtml(pass.name)}</h2><p>${escapeHtml(pass.detail)}</p>
          <div class="pass-buy-row"><strong class="pass-price">${formatPrice(pass.price)}<small> / day</small></strong>
            <button type="button" class="button button-outline" data-action="add-to-cart" data-id="${pass.id}">Add to cart</button>
          </div>
        </div>
      </article>`).join("")}
    </div>
    <section class="cart-panel"><div class="section-title-row cart-heading"><div><p class="eyebrow">Ready to pay</p><h2 id="cart-title">Your cart</h2></div></div>${cartDetails()}</section>
  </section>`;
}

async function csrfHeaders() {
  // await requestJson("/api/v1/auth/csrf", { cache: "no-store" });
  // const cookie = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
  // if (!cookie) throw new Error("Security token missing.");
  // return { "X-XSRF-TOKEN": decodeURIComponent(cookie[1]) };
  return {};
}

// ... [คงฟังก์ชัน QR และ Storage อื่นๆ ไว้เหมือนเดิม] ...
function openQrDatabase() {
  return new Promise((resolve, reject) => {
    if (!("indexedDB" in window)) { reject(new Error("No IDB support.")); return; }
    const request = indexedDB.open(QR_DATABASE_NAME, 1);
    request.onupgradeneeded = () => { if (!request.result.objectStoreNames.contains(QR_STORE_NAME)) request.result.createObjectStore(QR_STORE_NAME, { keyPath: "accountId" }); };
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}
async function saveCachedQr(record) { const db = await openQrDatabase(); return new Promise((res, rej) => { const tx = db.transaction(QR_STORE_NAME, "readwrite"); tx.objectStore(QR_STORE_NAME).put(record); tx.oncomplete = () => { db.close(); res(); }; tx.onerror = () => { db.close(); rej(tx.error); }; }); }
async function getCachedQr(accountId) { const db = await openQrDatabase(); return new Promise((res, rej) => { const tx = db.transaction(QR_STORE_NAME, "readonly"); const req = tx.objectStore(QR_STORE_NAME).get(accountId); req.onsuccess = () => { db.close(); res(req.result || null); }; req.onerror = () => { db.close(); rej(req.error); }; }); }
async function clearCachedQr(accountId) { if (!accountId) return; const db = await openQrDatabase(); return new Promise((res) => { const tx = db.transaction(QR_STORE_NAME, "readwrite"); tx.objectStore(QR_STORE_NAME).delete(accountId); tx.oncomplete = () => { db.close(); res(); }; }); }
async function clearAllCachedQr() { const db = await openQrDatabase(); return new Promise((res) => { const tx = db.transaction(QR_STORE_NAME, "readwrite"); tx.objectStore(QR_STORE_NAME).clear(); tx.oncomplete = () => { db.close(); res(); }; }); }

async function clearQrForAccountSwitch(accountId) {
  const previous = localStorage.getItem(LAST_QR_ACCOUNT_KEY);
  if (!previous || previous === accountId) return "";
  try { localStorage.removeItem(LAST_QR_ACCOUNT_KEY); await clearCachedQr(previous); } catch (e) { return "Could not delete previous QR."; }
  return "";
}

function renderQrCard(record, isCached) {
  const expiry = new Date(record.expiresAt);
  if (!Number.isFinite(expiry.getTime()) || (expiry.getTime() - Date.now()) <= 0) return `<div class="empty-state"><h2>QR Expired</h2><button type="button" class="button button-outline" data-action="refresh-qr">Try again</button></div>`;
  const offlineBanner = isCached ? `<div class="offline-banner"><strong>Saved code</strong></div>` : "";
  return `<div class="qr-card">${offlineBanner}<div class="qr-card-heading"><span>Ready to scan</span></div>
    <img class="qr-image" src="${record.qrImageDataUrl}" alt="QR"><h2>${escapeHtml(record.passName || "Pass")}</h2>
    <p class="qr-expiry" data-expiry="${escapeHtml(record.expiresAt)}">Expires in <strong>--:--</strong></p>
    <button type="button" class="button button-outline button-full" data-action="refresh-qr"${isCached ? " disabled" : ""}>Refresh</button></div>`;
}

async function renderQrPage() {
  clearInterval(qrRefreshTimer);
  appContent.innerHTML = `<section>${pageHeading("Your visits", "My QR pass", "Loading...")}</section>`;
  let lastAccount = localStorage.getItem(LAST_QR_ACCOUNT_KEY) || "";
  try {
    const record = await requestJson(QR_ENDPOINT);
    if (lastAccount && lastAccount !== record.accountId) await clearCachedQr(lastAccount);
    try { await saveCachedQr(record); localStorage.setItem(LAST_QR_ACCOUNT_KEY, record.accountId); } catch (e) {}
    if (currentPage() === "qr") {
      appContent.innerHTML = `<section>${pageHeading("Your visits", "My QR pass")}${renderQrCard(record, false)}<button class="text-button cache-clear-button" data-action="clear-qr-cache">Remove saved QR</button></section>`;
      startExpiryCountdown();
    }
  } catch (error) {
    if (currentPage() !== "qr") return;
    if (error.status === 401) {
      if (lastAccount) await clearCachedQr(lastAccount);
      localStorage.removeItem(LAST_QR_ACCOUNT_KEY);
      appContent.innerHTML = `<section>${pageHeading("Your visits", "My QR pass")}<div class="empty-state"><h2>Sign in required</h2><a class="button button-dark" href="#profile">Go to profile</a></div></section>`;
      return;
    }
    if (lastAccount && !navigator.onLine) {
      const cached = await getCachedQr(lastAccount);
      if (cached && new Date(cached.expiresAt).getTime() > Date.now()) {
        appContent.innerHTML = `<section>${pageHeading("Your visits", "My QR pass")}${renderQrCard(cached, true)}<button class="text-button cache-clear-button" data-action="clear-qr-cache">Remove saved QR</button></section>`;
        startExpiryCountdown();
        return;
      }
    }
    appContent.innerHTML = `<section>${pageHeading("Your visits", "My QR pass")}<div class="empty-state"><h2>Not available</h2><p>${error.message}</p></div></section>`;
  }
}

function startExpiryCountdown() {
  clearInterval(qrRefreshTimer);
  const el = document.querySelector("[data-expiry]");
  if (!el) return;
  const update = () => {
    const s = Math.max(0, Math.floor((new Date(el.dataset.expiry).getTime() - Date.now()) / 1000));
    el.innerHTML = s > 0 ? `Expires in <strong>${Math.floor(s/60).toString().padStart(2,"0")}:${(s%60).toString().padStart(2,"0")}</strong>` : "<strong>Expired.</strong>";
    if (s <= 0) { document.querySelector(".qr-image")?.remove(); clearInterval(qrRefreshTimer); }
  };
  update();
  qrRefreshTimer = setInterval(update, 1000);
}

function formatDate(value) {
  const d = new Date(value); return Number.isFinite(d.getTime()) ? new Intl.DateTimeFormat("en", { dateStyle: "medium", timeStyle: "short" }).format(d) : "N/A";
}

async function renderHistoryPage() {
  appContent.innerHTML = `<section>${pageHeading("Your visits", "Pass history", "Loading...")}</section>`;
  try {
    const history = await requestJson(HISTORY_ENDPOINT);
    if (history.length === 0) appContent.innerHTML = `<section>${pageHeading("Your visits", "Pass history")}<div class="empty-state"><h2>No visits yet</h2></div></section>`;
    else appContent.innerHTML = `<section>${pageHeading("Your visits", "Pass history")}<div class="history-list">${history.map(e => `<article class="history-item"><div><h2>${escapeHtml(e.attractionName)}</h2><p>${escapeHtml(e.passName)}</p><time>${formatDate(e.usedAt)}</time></div></article>`).join("")}</div></section>`;
  } catch (error) {
    if (error.status === 401) appContent.innerHTML = `<section>${pageHeading("Your visits", "Pass history")}<div class="empty-state"><h2>Sign in required</h2></div></section>`;
    else appContent.innerHTML = `<section>${pageHeading("Your visits", "Pass history")}<div class="empty-state"><h2>History unavailable</h2></div></section>`;
  }
}

function profilePage(authState) {
  if (authState.account) {
    return `<section>${pageHeading("Your account", `Welcome, ${escapeHtml(authState.account.displayName)}.`)}
      <div class="profile-card"><div class="profile-option-row"><span><strong>Email address</strong><small>${escapeHtml(authState.account.email)}</small></span></div>
      <button type="button" class="button button-outline button-full" data-action="logout">Sign out</button><p class="form-message">${escapeHtml(authState.notice || "")}</p></div></section>`;
  }
  const reg = profileMode === "register";
  return `<section>${pageHeading("Your account", "Make it yours.")}
    <div class="profile-card"><div class="profile-tabs"><button type="button" class="${!reg ? "is-active" : ""}" data-action="profile-mode" data-mode="login">Sign in</button><button type="button" class="${reg ? "is-active" : ""}" data-action="profile-mode" data-mode="register"${authState.config.registrationEnabled ? "" : " disabled"}>Create account</button></div>
    <form id="account-form" class="account-form" novalidate>
      ${reg ? `<label class="field-label">Full name</label><input name="fullName" required>` : ""}
      <label class="field-label">Email</label><input name="email" type="email" required>
      <label class="field-label">Password</label><input name="password" type="password" required>
      ${reg ? `<small>At least 8 characters, with at least 1 number and 1 special character (! @ # $ %). English letters only.</small>` : ""}
      ${reg ? `<div class="consent-box"><label class="check-row"><input type="checkbox" name="privacyNoticeAcknowledged" required><span>I acknowledge the Privacy Notice.</span></label></div>` : ""}
      <button type="submit" class="button button-dark button-full">${reg ? "Create account" : "Sign in"} →</button><p id="account-message" class="form-message">${escapeHtml(authState.notice || authState.error)}</p>
    </form></div></section>`;
}

async function renderProfilePage() {
  const authState = { account: null, config: { registrationEnabled: false }, notice: "", error: "" };
  appContent.innerHTML = `<section>${pageHeading("Your account", "Loading...")}</section>`;
  try {
    authState.config = await requestJson("/api/v1/auth/config", { cache: "no-store" });
    try {
      authState.account = await requestJson("/api/v1/auth/me", { cache: "no-store" });
      authState.notice = await clearQrForAccountSwitch(authState.account.accountId);
    } catch (e) {}
  } catch (e) { authState.error = "Auth service unavailable."; }
  if (profileFeedback) { authState.notice = profileFeedback; profileFeedback = ""; }
  if (currentPage() === "profile") appContent.innerHTML = profilePage(authState);
}

let verifyState = null;

function verifyHtml(state) {
  return `<section>${pageHeading("Your account", escapeHtml(state.title))}<div class="empty-state"><p>${escapeHtml(state.message)}</p><a class="button button-dark" href="#profile">Go to sign in</a></div></section>`;
}

async function renderVerifyPage() {
  const token = new URLSearchParams(window.location.hash.split("?")[1] || "").get("token");
  if (!token) {
    appContent.innerHTML = verifyHtml(verifyState || { title: "Could not verify email", message: "This verification link is incomplete. Open the link from your email again." });
    return;
  }
  if (verifyState && verifyState.token === token && !verifyState.pending) { appContent.innerHTML = verifyHtml(verifyState); return; }
  if (verifyState && verifyState.pending === token) return;
  verifyState = { pending: token };
  appContent.innerHTML = `<section>${pageHeading("Your account", "Verifying your email...")}</section>`;
  let result;
  try {
    const csrf = await csrfHeaders();
    const reply = await requestJson("/api/v1/auth/verify-email", { method: "POST", headers: { ...csrf, "Content-Type": "application/json" }, body: JSON.stringify({ token }) });
    result = { token, title: "Email verified", message: reply.message };
  } catch (e) { result = { token, title: "Could not verify email", message: e.message }; }
  verifyState = result;
  if (currentPage() === "verify") appContent.innerHTML = verifyHtml(result);
}

function addResendButton(form, msg) {
  if (form.querySelector("#resend-verification")) return;
  const button = document.createElement("button");
  button.type = "button"; button.id = "resend-verification"; button.className = "button button-outline button-full"; button.textContent = "Resend verification email";
  button.addEventListener("click", async () => {
    button.disabled = true;
    try {
      const csrf = await csrfHeaders();
      const reply = await requestJson("/api/v1/auth/resend-verification", { method: "POST", headers: { ...csrf, "Content-Type": "application/json" }, body: JSON.stringify({ email: form.elements.email.value.trim() }) });
      msg.textContent = reply.message;
    } catch (e) { msg.textContent = e.message; } finally { button.disabled = false; }
  });
  msg.after(button);
}

function setProfileMode(mode) { profileMode = mode === "register" ? "register" : "login"; profileFeedback = ""; render(); }

async function render() {
  const page = currentPage();
  document.querySelectorAll(".bottom-nav [data-page]").forEach(l => l.dataset.page === page ? l.setAttribute("aria-current", "page") : l.removeAttribute("aria-current"));
  updateCartCount();
  if (!isCatalogLoaded && (page === "home" || page === "buy")) { appContent.innerHTML = `<div class="loading-panel">Loading...</div>`; await fetchCatalogData(); }
  switch (page) {
    case "buy": appContent.innerHTML = buyPage(); break;
    case "qr": void renderQrPage(); break;
    case "history": void renderHistoryPage(); break;
    case "profile": void renderProfilePage(); break;
    case "verify": void renderVerifyPage(); break;
    default: appContent.innerHTML = homePage();
  }
}

function showNotice(title, message) {
  document.querySelector("#notice-dialog")?.remove();
  const d = document.createElement("dialog"); d.id = "notice-dialog"; d.className = "notice-dialog";
  d.innerHTML = `<div class="dialog-content"><button class="dialog-close" data-action="close-dialog">×</button><h2>${escapeHtml(title)}</h2><p>${escapeHtml(message)}</p><button class="button button-dark" data-action="close-dialog">Got it</button></div>`;
  document.body.append(d); d.showModal();
}

document.addEventListener("click", async (event) => {
  if (event.target.closest("[data-navigate]")) { window.location.hash = event.target.closest("[data-navigate]").dataset.navigate; return; }
  const control = event.target.closest("[data-action]"); if (!control) return;

  switch (control.dataset.action) {
    case "add-to-cart":
      const id = control.dataset.id;
      const startInp = document.getElementById("trip-start");
      const endInp = document.getElementById("trip-end");
      if (!startInp.value || !endInp.value) { showNotice("Dates Required", "Please select your Start and End dates at the top before adding a pass."); return; }
      if (endInp.value < startInp.value) { showNotice("Invalid Dates", "The End Date cannot be before the Start Date."); return; }
      const exist = cart.find(i => i.passId === id && i.startDate === startInp.value && i.endDate === endInp.value);
      if (exist) { exist.quantity += 1; if (exist.quantity > 10) exist.quantity = 10; } 
      else cart.push({ passId: id, startDate: startInp.value, endDate: endInp.value, quantity: 1 });
      persistCart(); render(); announce("Cart updated.");
      break;
    case "update-cart":
      const idx = parseInt(control.dataset.index);
      cart[idx].quantity += parseInt(control.dataset.amount);
      if (cart[idx].quantity <= 0) cart.splice(idx, 1);
      else if (cart[idx].quantity > 10) cart[idx].quantity = 10;
      persistCart(); render();
      break;
    case "checkout":
      if (cart.length === 0) return;
      control.disabled = true; control.textContent = "Processing...";
      try {
        const csrf = await csrfHeaders();
        for (const item of cart) {
          await requestJson(CHECKOUT_ENDPOINT, { method: "POST", headers: { ...csrf, "Content-Type": "application/json" }, body: JSON.stringify(item), cache: "no-store" });
        }
        cart = []; persistCart();
        showNotice("Success!", "Your payment was processed and passes have been added to your account. Go to 'My QR' to view them.");
        render();
      } catch (e) { showNotice("Checkout Failed", e.message || "Please sign in to complete checkout."); control.disabled = false; control.innerHTML = `Confirm & Checkout <span aria-hidden="true">→</span>`; }
      break;
    case "profile-mode": setProfileMode(control.dataset.mode); break;
    case "logout":
      try { const csrf = await csrfHeaders(); await fetch(apiUrl("/api/v1/auth/logout"), { method: "POST", headers: csrf, credentials: "include" }); try { await clearAllCachedQr(); localStorage.removeItem(LAST_QR_ACCOUNT_KEY); } catch (e) {} profileMode = "login"; profileFeedback = "Signed out."; render(); } catch (e) {} break;
    case "refresh-qr": if (!control.disabled) await renderQrPage(); break;
    case "clear-qr-cache": let acc = localStorage.getItem(LAST_QR_ACCOUNT_KEY); if (acc) await clearCachedQr(acc); localStorage.removeItem(LAST_QR_ACCOUNT_KEY); if (currentPage() === "qr") await renderQrPage(); break;
    case "close-dialog": control.closest("dialog")?.close(); break;
  }
});

function passwordProblem(password) {
  const unmet = [];
  if (password.length < 8) unmet.push("at least 8 characters");
  if (password.length > 72) unmet.push("at most 72 characters");
  if (!/[0-9]/.test(password)) unmet.push("at least 1 number");
  if (!/[!-\/:-@\[-`{-~]/.test(password)) unmet.push("at least 1 special character (for example ! @ # $ %)");
  if (/[^\x20-\x7E]/.test(password)) unmet.push("only English letters, numbers and symbols (no Thai or other languages)");
  return unmet.length ? `Password must have: ${unmet.join("; ")}.` : "";
}

document.addEventListener("submit", async (event) => {
  if (event.target.id !== "account-form") return;
  event.preventDefault(); const form = event.target; const msg = form.querySelector("#account-message");
  if (!form.reportValidity()) return;
  if (profileMode === "register" && !form.elements.privacyNoticeAcknowledged.checked) { msg.textContent = "Please acknowledge the Privacy Notice."; return; }
  if (profileMode === "register") { const problem = passwordProblem(form.elements.password.value); if (problem) { msg.textContent = problem; return; } }
  const btn = form.querySelector('[type="submit"]'); btn.disabled = true; msg.textContent = "Processing...";
  try {
    const csrf = await csrfHeaders();
    if (profileMode === "register") {
      const created = await requestJson("/api/v1/auth/register", { method: "POST", headers: { ...csrf, "Content-Type": "application/json" }, body: JSON.stringify({ fullName: form.elements.fullName.value.trim(), email: form.elements.email.value.trim(), password: form.elements.password.value, privacyNoticeAcknowledged: form.elements.privacyNoticeAcknowledged.checked }) });
      profileMode = "login"; profileFeedback = created.message;; await renderProfilePage(); return;
    }
    const res = await fetch(apiUrl("/api/v1/auth/login"), { method: "POST", headers: { ...csrf, Accept: "application/json", "Content-Type": "application/x-www-form-urlencoded" }, body: new URLSearchParams({ email: form.elements.email.value.trim(), password: form.elements.password.value }), credentials: "include" });
      if (!res.ok) {
        let reason = "Sign in failed. Please try again.", notVerified = false;
        try { const body = await res.json(); if (typeof body.message === "string") reason = body.message; notVerified = body.code === "EMAIL_NOT_VERIFIED"; } catch (e) {}
        const failure = new Error(reason); failure.notVerified = notVerified; throw failure;
    }

    const account = await res.json(); profileFeedback = await clearQrForAccountSwitch(account.accountId) || "Signed in."; profileMode = "login"; window.location.hash = "#profile"; await renderProfilePage();
  } catch (e) { msg.textContent = e.message; if (e.notVerified) addResendButton(form, msg); } finally { btn.disabled = false; }
});

window.addEventListener("hashchange", render);
render();