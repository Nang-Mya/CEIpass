"use strict";

const CART_STORAGE_KEY = "cei-pass-demo-cart";
const LAST_QR_ACCOUNT_KEY = "cei-pass-last-qr-account";
const QR_DATABASE_NAME = "cei-pass-private-cache";
const QR_STORE_NAME = "issued-qr";
const QR_ENDPOINT = "/api/v1/me/qr";
const HISTORY_ENDPOINT = "/api/v1/me/usage-history";
const PASS_SUMMARY_ENDPOINT = "/api/v1/me/pass-summary";
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
let resetEmail = "", resetToken = "";
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
    signal: options.signal || AbortSignal.timeout(30000)
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

//keep cart data as a JSON array of objects in localStorage, each object containing passId, startDate, endDate, and quantity.
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
  return ["home", "buy", "qr", "history", "profile","reset"].includes(page) ? page : "home";
}

function pageHeading(eyebrow, title, description = "") {
  return `<div class="page-heading"><p class="eyebrow">${eyebrow}</p><h1>${title}</h1>${description ? `<p class="page-description">${description}</p>` : ""}</div>`;
}

function emptyCatalogState() {
  return `<div class="empty-state"><span class="empty-icon" aria-hidden="true">📡</span><h2>Catalog is unavailable</h2><p>Check backend API and Database.</p><button type="button" class="button button-outline" onclick="location.reload()">Retry Connection</button></div>`;
}

function attractionCard(attraction, compact = false) {
  const imageHtml = attraction.imageUrl 
    ? `<img src="${escapeHtml(attraction.imageUrl)}" alt="${escapeHtml(attraction.name)}" style="width: 100%; height: 100%; object-fit: cover; border-radius: inherit;">`
    : `<span>${attraction.initials}</span>`; 
    // if don't have img it'll show short name instead of blank space

  return `<article class="attraction-card${compact ? " attraction-card-compact" : ""}">
    <div class="attraction-art ${attraction.tone}" aria-hidden="true">
      ${imageHtml}
    </div>
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
  await requestJson("/api/v1/auth/csrf", { cache: "no-store" });
  const cookie = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
  if (!cookie) throw new Error("Security token missing.");
  return { "X-XSRF-TOKEN": decodeURIComponent(cookie[1]) };
  // return {};
}

// keep the QR caching functions as they are, since they are already well-defined and handle IndexedDB operations for caching QR codes.
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

function historyListMarkup(history) {
  if (history.length === 0) return `<div class="empty-state"><h2>No visits yet</h2></div>`;
  const rows = history.map((item) => {
    const success = item.status === "VALID_SUCCESS";
    const quotaFailure = item.status === "QUOTA_EXCEEDED" || item.status === "QUOTA_USED";
    const label = success ? "Success" : quotaFailure ? "Quota Used" : escapeHtml(item.status);
    return `<article class="history-item">
      <div class="history-item-copy"><h3>${escapeHtml(item.attractionName)}</h3><p>${escapeHtml(item.passName)}</p><time>${escapeHtml(formatDate(item.usedAt))}</time></div>
      <span class="me-badge ${success ? "is-available" : "is-used"}">${label}</span>
    </article>`;
  }).join("");
  return `<div class="history-list">${rows}</div>`;
}

function historyPassRemaining(pass) {
  if (pass.status === "EXPIRED") return "Expired";
  if (pass.status !== "ACTIVE" || !pass.endsAt) return "-";
  const hoursLeft = Math.floor((new Date(pass.endsAt).getTime() - Date.now()) / 3600000);
  if (!(hoursLeft > 0)) return "Expired";
  const days = Math.floor(hoursLeft / 24);
  const hours = hoursLeft % 24;
  return `${days} ${days === 1 ? "Day" : "Days"} ${hours} ${hours === 1 ? "Hour" : "Hours"}`;
}

function historyOverviewMarkup(account, summary) {
  const pass = summary.pass;
  const passCard = pass
    ? `<article class="history-pass-card">
        <div class="history-pass-head">
          <h2>${escapeHtml(pass.passName)}</h2>
          <span class="me-status ${pass.status === "ACTIVE" ? "is-active" : ""}">${escapeHtml(pass.status)}</span>
        </div>
        <div class="history-pass-detail">
          <p class="history-remaining-label">Remaining time</p>
          <strong class="history-remaining">${escapeHtml(historyPassRemaining(pass))}</strong>
          <p class="history-validity">Valid: ${escapeHtml(formatPassDate(pass.startDate))} - ${escapeHtml(formatPassDate(pass.endDate))}</p>
        </div>
      </article>`
    : `<div class="empty-state"><h2>No active pass yet</h2></div>`;
  const benefits = summary.benefits.length
    ? `<ul class="me-benefits">${summary.benefits.map((benefit) => {
        const used = benefit.used >= benefit.limit;
        return `<li class="me-benefit"><span><strong>${escapeHtml(benefit.attractionName)}</strong><small>${escapeHtml(benefit.detail)}</small></span>
          <span class="me-badge ${used ? "is-used" : "is-available"}">${used ? "Used" : "Available"} (${escapeHtml(benefit.used)}/${escapeHtml(benefit.limit)})</span></li>`;
      }).join("")}</ul>`
    : `<div class="empty-state"><h2>No active pass yet</h2></div>`;

  return `<section class="history-page">
    ${pageHeading("Your visits", "Pass history")}
    <article class="me-card me-identity history-profile"><strong>${escapeHtml(account.displayName)}</strong></article>
    ${passCard}
    <section class="me-card history-section"><h2 class="me-subtitle">Pass Benefits &amp; Quota</h2>${benefits}</section>
    <section class="me-card history-section">
      <h2 class="me-subtitle">Check-in History</h2>
      ${historyListMarkup(summary.recentUsage)}
      <a class="me-link history-full-link" href="#history" data-action="history-full">View Full Usage History →</a>
    </section>
  </section>`;
}

async function renderHistoryPage(showFullHistory = false) {
  const heading = showFullHistory ? "Full Usage History" : "Pass history";
  appContent.innerHTML = `<section>${pageHeading("Your visits", heading, "Loading...")}</section>`;
  try {
    if (showFullHistory) {
      const history = await requestJson(HISTORY_ENDPOINT, { cache: "no-store" });
      appContent.innerHTML = `<section class="history-page">
        ${pageHeading("Your visits", heading)}
        <button type="button" class="button button-outline history-back" data-action="history-recent">Back</button>
        ${historyListMarkup(history)}
      </section>`;
      return;
    }

    const [account, summary] = await Promise.all([
      requestJson("/api/v1/auth/me", { cache: "no-store" }),
      requestJson(PASS_SUMMARY_ENDPOINT, { cache: "no-store" })
    ]);
    appContent.innerHTML = historyOverviewMarkup(account, summary);
  } catch (error) {
    if (error.status === 401) appContent.innerHTML = `<section>${pageHeading("Your visits", "Pass history")}<div class="empty-state"><h2>Sign in required</h2></div></section>`;
    else appContent.innerHTML = `<section>${pageHeading("Your visits", "Pass history")}<div class="empty-state"><h2>History unavailable</h2></div></section>`;
  }
}

function formatPassDate(value) {
  const d = new Date(`${value}T00:00:00`);
  return value && Number.isFinite(d.getTime())
    ? new Intl.DateTimeFormat("en", { day: "numeric", month: "short", year: "numeric" }).format(d) : "-";
}

function passRemaining(pass) {
  if (pass.status !== "ACTIVE" || !pass.endDate) return "-";
  const ms = new Date(`${pass.endDate}T23:59:59`).getTime() - Date.now();
  if (!(ms > 0)) return "Expired";
  const hours = Math.floor(ms / 3600000);
  return `${Math.floor(hours / 24)} days ${hours % 24} hours`;
}

function myPassCard(authState) {
  if (authState.passError) return `<div class="me-card"><p class="form-message">Could not load your pass. Please try again later.</p></div>`;
  const pass = authState.pass;
  if (!pass) return `<div class="me-card me-empty"><strong>No pass yet</strong><small>Buy a pass to see its benefits here.</small><a class="button button-dark" href="#buy">Buy a pass</a></div>`;
  const active = pass.status === "ACTIVE";
  const benefits = pass.benefits.length === 0
    ? `<p class="me-muted">This pass has no listed benefits.</p>`
    : pass.benefits.map((b) => {
        const used = b.used >= b.limit;
        return `<li class="me-benefit"><span><strong>${escapeHtml(b.placeName)}</strong><small>${escapeHtml(b.detail)}</small></span>
          <span class="me-badge ${used ? "is-used" : "is-available"}">${used ? "Used" : "Available"} (${b.used}/${b.limit})</span></li>`;
      }).join("");
  return `<div class="me-card">
    <div class="me-pass-head"><span class="me-pass-info">
      <strong>${escapeHtml(pass.passName)}</strong>
      <small>Remaining: ${escapeHtml(passRemaining(pass))}</small>
      <small>Start: ${formatPassDate(pass.startDate)}</small>
      <small>End: ${formatPassDate(pass.endDate)}</small></span>
      <span class="me-status ${active ? "is-active" : ""}">${active ? "Active" : "Not active"}</span></div>
    <h2 class="me-subtitle">Pass benefits</h2><ul class="me-benefits">${benefits}</ul></div>`;
}

const PRIVACY_LABELS = {
  MARKETING_EMAILS: ["Marketing emails", "Offers and news about Chiang Rai Pass"],
  USAGE_ANALYTICS: ["Usage analytics", "Help improve the app"]
};

function privacyCard(authState) {
  const p = authState.privacy;
  const failed = authState.privacyError || !p;
  const canChange = !failed && p.available;
  const rows = Object.entries(PRIVACY_LABELS).map(([purpose, [label, description]]) => {
    const item = failed ? null : p.preferences.find((x) => x.purpose === purpose);
    // Only an explicit "granted" from the server is ever shown as consented.
    const granted = Boolean(item && item.granted);
    const status = failed
      ? `<span class="me-badge is-used">Unavailable</span>`
      : `<span class="me-badge ${granted ? "is-available" : "is-used"}">${granted ? "Consented" : "Not consented"}</span>`;
    const button = canChange
      ? `<button type="button" class="text-button" data-privacy-purpose="${purpose}" data-privacy-granted="${granted}">${granted ? "Withdraw consent" : "Give consent"}</button>`
      : "";
    const updated = item && item.recordedAt ? `<small>Updated ${escapeHtml(formatDate(item.recordedAt))}</small>` : "";
    return `<li class="me-benefit"><span><strong>${escapeHtml(label)}</strong><small>${escapeHtml(description)}</small><small class="me-optional">Optional</small>${updated}</span>
      <span class="me-privacy-actions">${status}${button}</span></li>`;
  }).join("");
  const note = failed
    ? `<p class="form-message">Could not load your privacy preferences. Please try again later.</p>`
    : (!p.available ? `<p class="me-muted">Changing preferences will be available once the Privacy Notice is published.</p>` : "");
  return `<div class="me-card"><h2 class="me-subtitle">Privacy Notice</h2>
      <p class="me-muted">Learn more about how your personal information is collected, used, and managed.</p>
      <a class="me-link" href="privacy.html">Read Privacy Notice</a></div>
    <div class="me-card" id="privacy-card" data-version="${escapeHtml(canChange ? p.noticeVersion : "")}">
      <h2 class="me-subtitle">Privacy preferences</h2>
      <p class="me-muted">Manage optional preferences</p>${note}
      <ul class="me-benefits">${rows}</ul><p id="privacy-message" class="form-message"></p></div>`;
}

document.addEventListener("click", async (event) => {
  const button = event.target.closest("[data-privacy-purpose]");
  if (!button) return;
  const card = document.querySelector("#privacy-card"), msg = document.querySelector("#privacy-message");
  button.disabled = true; msg.textContent = "Saving...";
  try {
    const csrf = await csrfHeaders();
    await requestJson("/api/v1/me/privacy-preferences", {
      method: "POST",
      headers: { ...csrf, "Content-Type": "application/json" },
      body: JSON.stringify({
        noticeVersion: card.dataset.version,
        choices: [{ purpose: button.dataset.privacyPurpose, granted: button.dataset.privacyGranted !== "true" }]
      })
    });
    profileFeedback = "Privacy choice saved."; await renderProfilePage();
  } catch (e) { msg.textContent = e.message; button.disabled = false; }
});

function profilePage(authState) {
    if (authState.account) {
    const account = authState.account;
    return `<section>${pageHeading("Your account", "Profile")}
      ${authState.notice ? `<p class="form-message">${escapeHtml(authState.notice)}</p>` : ""}
      <div class="me-card me-identity">
        <span class="me-avatar" aria-hidden="true"><svg viewBox="0 0 24 24" width="44" height="44" fill="none" stroke="currentColor" stroke-width="1.6"><circle cx="12" cy="12" r="10.2"/><circle cx="12" cy="9.6" r="3.4"/><path d="M5.6 18.6c1.5-2.8 3.6-4.1 6.4-4.1s4.9 1.3 6.4 4.1"/></svg></span>
        <span class="me-identity-text"><strong>${escapeHtml(account.displayName)}</strong><small>${escapeHtml(account.email)}</small></span>
      </div>
      ${myPassCard(authState)}
      ${privacyCard(authState)}
      <button type="button" class="button button-outline button-full" data-action="logout">Sign out</button></section>`;
  }
  const reg = profileMode === "register";

  if (profileMode === "forgot") {
    return `<section>${pageHeading("Your account", "Reset your password")}
      <div class="profile-card"><form id="forgot-form" class="account-form" novalidate>
        <label class="field-label">Email</label><input name="email" type="email" required autocomplete="email">
        <button type="submit" class="button button-dark button-full">Send code →</button><p class="form-message">${escapeHtml(authState.notice || "")}</p>
        <button type="button" class="button button-outline button-full" data-action="profile-mode" data-mode="login">Back to sign in</button>
      </form></div></section>`;
  }
  if (profileMode === "otp") {
    return `<section>${pageHeading("Your account", "Enter the code")}
      <div class="profile-card"><form id="otp-form" class="account-form" novalidate>
        <p>We sent a 6-digit code to <strong>${escapeHtml(resetEmail)}</strong>. It expires in 10 minutes.</p>
        <label class="field-label">6-digit code</label><input name="otp" inputmode="numeric" autocomplete="one-time-code" maxlength="6" required>
        <button type="submit" class="button button-dark button-full">Verify code →</button><p class="form-message">${escapeHtml(authState.notice || "")}</p>
        <small>You can request a new code once a minute.</small>
        <button type="button" class="button button-outline button-full" data-action="resend-otp">Send a new code</button>
      </form></div></section>`;
  }
  if (profileMode === "newpass") {
    return `<section>${pageHeading("Your account", "Choose a new password")}
      <div class="profile-card"><form id="newpass-form" class="account-form" novalidate>
        <label class="field-label">New password</label><input name="password" type="password" required autocomplete="new-password">
        <small>At least 8 characters, with at least 1 number and 1 special character (! @ # $ %). English letters only.</small>
        <label class="field-label">Confirm new password</label><input name="confirm" type="password" required autocomplete="new-password">
        <button type="submit" class="button button-dark button-full">Save new password →</button><p class="form-message">${escapeHtml(authState.notice || "")}</p>
        <button type="button" class="button button-outline button-full" data-action="profile-mode" data-mode="login">Back to sign in</button>
      </form></div></section>`;
  }
  return `<section>${pageHeading("Your account", "Make it yours.")}
    <div class="profile-card"><div class="profile-tabs"><button type="button" class="${!reg ? "is-active" : ""}" data-action="profile-mode" data-mode="login">Sign in</button><button type="button" class="${reg ? "is-active" : ""}" data-action="profile-mode" data-mode="register"${authState.config.registrationEnabled ? "" : " disabled"}>Create account</button></div>
    <form id="account-form" class="account-form" novalidate>
      ${reg ? `<label class="field-label">Full name</label><input name="fullName" required>` : ""}
      <label class="field-label">Email</label><input name="email" type="email" required>
      <label class="field-label">Password</label><input name="password" type="password" required>
      ${reg ? "" : `<button type="button" class="password-reset-link" data-action="profile-mode" data-mode="forgot">Forgot password?</button>`}
      ${reg ? `<small>At least 8 characters, with at least 1 number and 1 special character (! @ # $ %). English letters only.</small>` : ""}
      ${reg ? `<div class="consent-box"><p><a class="me-link" href="privacy.html">Read Privacy Notice</a></p><label class="check-row"><input type="checkbox" name="privacyNoticeAcknowledged" required><span>I acknowledge the Privacy Notice.</span></label></div>` : ""}
      <button type="submit" class="button button-dark button-full">${reg ? "Create account" : "Sign in"} →</button><p id="account-message" class="form-message">${escapeHtml(authState.notice || authState.error)}</p>
    </form></div></section>`;
}

async function renderProfilePage(loggedInAccount = null) {
  const authState = {
    account: loggedInAccount,
    config: { registrationEnabled: true },
    pass: null,
    passError: false,
    privacy: null,
    privacyError: false,
    notice: "",
    error: ""
  };
  appContent.innerHTML = `<section>${pageHeading("Your account", "Loading...")}</section>`;

  // 1. If there is no account information sent directly from the login page, then request /api/v1/auth/me
  if (!authState.account) {
    try {
      authState.account = await requestJson("/api/v1/auth/me", { cache: "no-store" });
      authState.notice = await clearQrForAccountSwitch(authState.account.accountId);
    } catch (e) {
      // ยังไม่ได้ล็อกอิน (401)
    }
  }

  // 2. If logged in, fetch the pass (My Pass) and privacy settings to display on the Profile page
  if (authState.account) {
    const [passResult, privacyResult] = await Promise.allSettled([
      requestJson("/api/v1/me/pass", { cache: "no-store" }),
      requestJson("/api/v1/me/privacy-preferences", { cache: "no-store" })
    ]);

    if (passResult.status === "fulfilled") {
      authState.pass = passResult.value?.pass || null;
    } else {
      authState.passError = true;
    }

    if (privacyResult.status === "fulfilled") {
      authState.privacy = privacyResult.value;
    } else {
      authState.privacyError = true;
    }
  } else {
    // 3. If not logged in, fetch the config for the login form
    try {
      authState.config = await requestJson("/api/v1/auth/config", { cache: "no-store" });
    } catch (e) {
      authState.config = { registrationEnabled: true };
    }
  }

  if (profileFeedback) {
    authState.notice = profileFeedback;
    profileFeedback = "";
  }
  if (currentPage() === "profile") {
    appContent.innerHTML = profilePage(authState);
  }
}

document.addEventListener("submit", async (event) => {
  const id = event.target.id;
  if (!["forgot-form", "otp-form", "newpass-form"].includes(id)) return;
  event.preventDefault();
  const form = event.target, msg = form.querySelector(".form-message"), btn = form.querySelector('[type="submit"]');
  let password = "";
  if (id === "otp-form" && !/^[0-9]{6}$/.test(form.elements.otp.value.trim())) { msg.textContent = "Enter the 6-digit code from your email."; return; }
  if (id === "newpass-form") {
    password = form.elements.password.value;
    const problem = passwordProblem(password);
    if (problem) { msg.textContent = problem; return; }
    if (password !== form.elements.confirm.value) { msg.textContent = "The two passwords do not match."; return; }
  }
  btn.disabled = true; msg.textContent = "Processing...";
  try {
    const csrf = await csrfHeaders();
    const post = (path, payload) => requestJson(path, { method: "POST", headers: { ...csrf, "Content-Type": "application/json" }, body: JSON.stringify(payload) });
    if (id === "forgot-form") {
      resetEmail = form.elements.email.value.trim();
      const reply = await post("/api/v1/auth/password/forgot", { email: resetEmail });
      profileMode = "otp"; profileFeedback = reply.message; await renderProfilePage(); return;
    }
    if (id === "otp-form") {
      const reply = await post("/api/v1/auth/password/verify-otp", { email: resetEmail, otp: form.elements.otp.value.trim() });
      resetToken = reply.token; profileMode = "newpass"; profileFeedback = reply.message; await renderProfilePage(); return;
    }
    const reply = await post("/api/v1/auth/password/reset", { token: resetToken, newPassword: password });
    resetEmail = ""; resetToken = ""; profileMode = "login"; profileFeedback = reply.message; await renderProfilePage();
  } catch (e) { msg.textContent = e.message; } finally { btn.disabled = false; }
});

document.addEventListener("click", async (event) => {
  const button = event.target.closest('[data-action="resend-otp"]');
  if (!button) return;
  const msg = button.closest("form").querySelector(".form-message");
  button.disabled = true; msg.textContent = "Sending...";
  try {
    const csrf = await csrfHeaders();
    const reply = await requestJson("/api/v1/auth/password/forgot", { method: "POST", headers: { ...csrf, "Content-Type": "application/json" }, body: JSON.stringify({ email: resetEmail }) });
    msg.textContent = reply.message;
  } catch (e) { msg.textContent = e.message; } finally { button.disabled = false; }
});

function setProfileMode(mode) { profileMode = mode === "register" ? "register" : mode === "forgot" ? "forgot" : "login"; if (profileMode !== "forgot") { resetEmail = ""; resetToken = ""; } profileFeedback = ""; render(); }

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
    case "reset": renderResetPage(); break;
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
    case "history-full": event.preventDefault(); await renderHistoryPage(true); break;
    case "history-recent": await renderHistoryPage(); break;
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
      await requestJson("/api/v1/auth/register", { method: "POST", headers: { ...csrf, "Content-Type": "application/json" }, body: JSON.stringify({ fullName: form.elements.fullName.value.trim(), email: form.elements.email.value.trim(), password: form.elements.password.value, privacyNoticeAcknowledged: form.elements.privacyNoticeAcknowledged.checked }) });
      profileMode = "login"; profileFeedback = "Account created. Sign in."; await renderProfilePage(); return;
    }
    const res = await fetch(apiUrl("/api/v1/auth/login"), { method: "POST", headers: { ...csrf, Accept: "application/json", "Content-Type": "application/x-www-form-urlencoded" }, body: new URLSearchParams({ email: form.elements.email.value.trim(), password: form.elements.password.value }), credentials: "include" });
    if (!res.ok) {
      let reason = "Sign in failed. Please try again.";
      try { const body = await res.json(); if (typeof body.message === "string") reason = body.message; } catch (e) {}
      throw new Error(reason);
    }

    const account = await res.json();
    profileFeedback = await clearQrForAccountSwitch(account.accountId) || "Signed in.";
    profileMode = "login";
    if (currentPage() !== "profile") {
      window.location.hash = "#profile";
    }
    await renderProfilePage(account);
  } catch (e) { msg.textContent = e.message; } finally { btn.disabled = false; }
});

window.addEventListener("hashchange", render);
render();