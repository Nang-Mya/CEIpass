"use strict";

const CART_STORAGE_KEY = "cei-pass-demo-cart";
const LAST_QR_ACCOUNT_KEY = "cei-pass-last-qr-account";
const QR_DATABASE_NAME = "cei-pass-private-cache";
const QR_STORE_NAME = "issued-qr";
const QR_ENDPOINT = "/api/v1/me/qr";
const HISTORY_ENDPOINT = "/api/v1/me/usage-history";
const MY_PASSES_ENDPOINT = "/api/v1/me/passes";
const PASSES_ENDPOINT = "/api/v1/catalog/passes";
const ATTRACTIONS_ENDPOINT = "/api/v1/catalog/attractions";
const CHECKOUT_ENDPOINT = "/api/v1/me/orders";

let passes = [];
let attractions = [];
let isCatalogLoaded = false;
let catalogLoadError = false;
let historyExpandedPassIds = new Set();
let historyPassCards = [];
let historyHasInitializedCards = false;
let historyAccount = null;
let historyPassFilter = "ALL";
let qrPassCards = [];
let qrAccountId = "";
let qrRecordsByPassId = new Map();
let qrSelectedIndex = 0;
let qrSwipeStartX = null;

const appContent = document.querySelector("#app-content");
const cartCount = document.querySelector("#cart-count");
const announcer = document.querySelector("#app-announcer");
const apiBaseUrl = window.CEIPASS_CONFIG?.apiBaseUrl;
let profileMode = "login";
let resetEmail = "", resetToken = "";
let otpCooldownUntil = 0, otpCooldownTimer = null;
let avatarObjectUrl = "";

const EYE_ICON = `<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M2 12s3.6-6.5 10-6.5S22 12 22 12s-3.6 6.5-10 6.5S2 12 2 12z"/><circle cx="12" cy="12" r="3"/></svg>`;
const EYE_OFF_ICON = `<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M3 3l18 18"/><path d="M10.6 5.7A10.5 10.5 0 0 1 12 5.5c6.4 0 10 6.5 10 6.5a17.6 17.6 0 0 1-3.2 4.1"/><path d="M6.5 6.9A17.3 17.3 0 0 0 2 12s3.6 6.5 10 6.5c1.5 0 2.8-.3 4-.8"/><path d="M9.9 9.9a3 3 0 0 0 4.2 4.2"/></svg>`;
const PROFILE_ICON = `<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="8" r="3.5"></circle><path d="M5 20c.6-3.7 3-5.5 7-5.5s6.4 1.8 7 5.5"></path></svg>`;

function setHeaderAvatar(url) {
  const button = document.querySelector("#header-profile-button");
  if (!button) return;
  if (!url) {
    button.innerHTML = PROFILE_ICON;
    return;
  }
  const image = document.createElement("img");
  image.className = "header-avatar-image";
  image.alt = "";
  image.src = url;
  button.replaceChildren(image);
}

// Adds a show/hide button to every password field that appears on the page.
function addPasswordToggles() {
  appContent.querySelectorAll('input[type="password"]:not([data-has-toggle])').forEach((input) => {
    input.dataset.hasToggle = "true";
    const wrapper = document.createElement("span");
    wrapper.className = "password-field";
    input.parentNode.insertBefore(wrapper, input);
    wrapper.appendChild(input);
    const toggle = document.createElement("button");
    toggle.type = "button";
    toggle.className = "password-toggle";
    toggle.setAttribute("aria-label", "Show password");
    toggle.setAttribute("aria-pressed", "false");
    toggle.innerHTML = EYE_ICON;
    wrapper.appendChild(toggle);
  });
}

function otpRemainingSeconds() {
  return Math.max(0, Math.ceil((otpCooldownUntil - Date.now()) / 1000));
}

// Keeps the "Resend OTP" button in step with the cooldown timer.
function updateOtpButton() {
  const left = otpRemainingSeconds();
  const button = document.querySelector('[data-action="resend-otp"]');
  if (button) {
    const label = left > 0 ? `Resend OTP in ${Math.floor(left / 60)}:${String(left % 60).padStart(2, "0")}` : "Resend OTP";
    if (button.textContent !== label) button.textContent = label;
    if (button.disabled !== left > 0) button.disabled = left > 0;
  }
  if (left <= 0 && otpCooldownTimer) { clearInterval(otpCooldownTimer); otpCooldownTimer = null; }
}

function startOtpCooldown(seconds) {
  otpCooldownUntil = Date.now() + Math.max(1, Number(seconds) || 60) * 1000;
  clearInterval(otpCooldownTimer);
  otpCooldownTimer = setInterval(updateOtpButton, 1000);
  updateOtpButton();
}

new MutationObserver(() => { addPasswordToggles(); updateOtpButton(); })
  .observe(appContent, { childList: true, subtree: true });

document.addEventListener("click", (event) => {
  const toggle = event.target.closest(".password-toggle");
  if (!toggle) return;
  const input = toggle.parentElement.querySelector("input");
  const show = input.type === "password";
  input.type = show ? "text" : "password";
  toggle.setAttribute("aria-pressed", String(show));
  toggle.setAttribute("aria-label", show ? "Hide password" : "Show password");
  toggle.innerHTML = show ? EYE_OFF_ICON : EYE_ICON;
});
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
    let retryAfterSeconds;
    try {
      const body = await response.json();
      if (typeof body.message === "string") message = body.message;
      if (Number.isFinite(body.retryAfterSeconds)) retryAfterSeconds = body.retryAfterSeconds;
    } catch (e) {}
    const error = new ApiError(response.status, message);
    if (retryAfterSeconds !== undefined) error.retryAfterSeconds = retryAfterSeconds;
    throw error;
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
  return ["home", "buy", "qr", "history", "profile", "change-password", "privacy-preferences", "reset"].includes(page) ? page : "home";
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
    <span></span>
    <a class="button button-light" href="#buy">Explore passes <span aria-hidden="true">→</span></a></div></div>
    <section class="section-block quick-actions" aria-label="Quick access">
      <a class="quick-card" href="#qr"><span class="quick-icon quick-icon-amber" aria-hidden="true">📱</span><span><strong>My QR pass</strong><small>Ready when you are</small></span></a>
    </section>
    <section class="section-block">
      <!-- ปุ่ม > ลิงก์ไปยัง #attractions -->
      <div class="section-title-row-link">
        <div><p class="eyebrow">Hand-picked for you</p><h2>Explore Chiang Rai</h2></div>
        <a href="#attractions" class="explore-link-btn" aria-label="See all tourist attractions">
          <svg viewBox="0 0 24 24"><polyline points="9 18 15 12 9 6"></polyline></svg>
        </a>
      </div>
      <div class="attraction-list">${featured}</div>
    </section>
  </section>`;
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
async function saveCachedQrs(accountId, records) { const db = await openQrDatabase(); return new Promise((res, rej) => { const tx = db.transaction(QR_STORE_NAME, "readwrite"); tx.objectStore(QR_STORE_NAME).put({ accountId, records }); tx.oncomplete = () => { db.close(); res(); }; tx.onerror = () => { db.close(); rej(tx.error); }; }); }
async function getCachedQrs(accountId) { const db = await openQrDatabase(); return new Promise((res, rej) => { const tx = db.transaction(QR_STORE_NAME, "readonly"); const req = tx.objectStore(QR_STORE_NAME).get(accountId); req.onsuccess = () => { db.close(); const saved = req.result; res(Array.isArray(saved?.records) ? saved.records : saved?.userPassId ? [saved] : []); }; req.onerror = () => { db.close(); rej(req.error); }; }); }
async function clearCachedQr(accountId) { if (!accountId) return; const db = await openQrDatabase(); return new Promise((res) => { const tx = db.transaction(QR_STORE_NAME, "readwrite"); tx.objectStore(QR_STORE_NAME).delete(accountId); tx.oncomplete = () => { db.close(); res(); }; }); }
async function clearAllCachedQr() { const db = await openQrDatabase(); return new Promise((res) => { const tx = db.transaction(QR_STORE_NAME, "readwrite"); tx.objectStore(QR_STORE_NAME).clear(); tx.oncomplete = () => { db.close(); res(); }; }); }

async function clearQrForAccountSwitch(accountId) {
  const previous = localStorage.getItem(LAST_QR_ACCOUNT_KEY);
  if (!previous || previous === accountId) return "";
  try { localStorage.removeItem(LAST_QR_ACCOUNT_KEY); await clearCachedQr(previous); } catch (e) { return "Could not delete previous QR."; }
  return "";
}

function renderQrCard(record, isCached, userPassId) {
  const offlineBanner = isCached ? `<div class="offline-banner"><strong>Saved code</strong></div>` : "";
  return `<div class="qr-card">${offlineBanner}<div class="qr-card-heading"><span>Ready to scan</span></div>
    <img class="qr-image" src="${record.qrImageDataUrl}" alt="QR code for ${escapeHtml(record.passName || "pass")}">
    <p class="qr-expiry" data-expiry="${escapeHtml(record.expiresAt)}">Expires in <strong>--:--</strong></p>
    <button type="button" class="button button-outline button-full" data-action="refresh-qr" data-user-pass-id="${escapeHtml(userPassId)}"${isCached ? " disabled" : ""}>Refresh</button></div>`;
}

function qrPassCardMarkup(pass, record = null, isCached = false, errorMessage = "") {
  const isActive = pass.displayStatus === "ACTIVE";
  const statusClass = pass.displayStatus === "ACTIVE"
    ? "is-active"
    : pass.displayStatus === "EXPIRED" ? "is-expired" : "is-not-started";
  let content = "";
  if (record && new Date(record.expiresAt).getTime() > Date.now()) {
    content = renderQrCard(record, isCached, pass.userPassId);
  } else if (isActive) {
    content = `<div class="qr-pass-message">${escapeHtml(errorMessage || "QR code unavailable. Try refreshing.")}</div>
      <button type="button" class="button button-outline" data-action="refresh-qr" data-user-pass-id="${escapeHtml(pass.userPassId)}">Try again</button>`;
  } else if (pass.displayStatus === "EXPIRED") {
    content = `<div class="qr-pass-message">This pass has expired.</div>`;
  } else {
    const startsAt = pass.startsAt && Number.isFinite(new Date(pass.startsAt).getTime());
    content = startsAt
      ? `<div class="qr-availability">
          <div class="qr-availability-card qr-availability-date">
            <svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="5" width="18" height="16" rx="2"></rect><path d="M16 3v4M8 3v4M3 10h18"></path></svg>
            <div><p>Available on</p><strong>${escapeHtml(formatPassDate(pass.startDate))}</strong></div>
          </div>
          <div class="qr-availability-card qr-availability-countdown">
            <p>Starts in</p>
            <strong class="qr-starts-in" data-starts-at="${escapeHtml(pass.startsAt)}">--</strong>
          </div>
        </div>`
      : `<div class="qr-pass-message">Start date unavailable.</div>`;
  }
  return `<article class="qr-pass-card" data-qr-pass-id="${escapeHtml(pass.userPassId)}">
    <div class="qr-pass-heading"><h2>${escapeHtml(pass.passName)}</h2><span class="me-status history-pass-status ${statusClass}">${escapeHtml(pass.displayStatus === "NOT STARTED" ? "NOT ACTIVE YET" : pass.displayStatus)}</span></div>
    ${content}
  </article>`;
}

function sortQrPasses(passes) {
  const statusOrder = { ACTIVE: 0, "NOT STARTED": 1, EXPIRED: 2 };
  return [...passes].sort((a, b) =>
    (statusOrder[a.displayStatus] ?? 3) - (statusOrder[b.displayStatus] ?? 3));
}

function renderQrCarousel() {
  const carousel = document.querySelector(".qr-carousel");
  if (!carousel || !qrPassCards.length) return;

  qrSelectedIndex = Math.max(0, Math.min(qrSelectedIndex, qrPassCards.length - 1));
  const pass = qrPassCards[qrSelectedIndex];
  const result = qrRecordsByPassId.get(pass.userPassId);
  carousel.innerHTML = `
    <div class="qr-carousel-toolbar">
      <span class="qr-position" aria-live="polite">${qrSelectedIndex + 1} of ${qrPassCards.length} passes</span>
    </div>
    <div class="qr-carousel-viewport" aria-live="polite">
      ${qrPassCardMarkup(pass, result?.record, result?.isCached || false, result?.error || "")}
      <button type="button" class="qr-arrow qr-arrow-previous" data-action="qr-previous" aria-label="Previous pass"${qrSelectedIndex === 0 ? " disabled" : ""}>‹</button>
      <button type="button" class="qr-arrow qr-arrow-next" data-action="qr-next" aria-label="Next pass"${qrSelectedIndex === qrPassCards.length - 1 ? " disabled" : ""}>›</button>
    </div>`;
  const viewport = carousel.querySelector(".qr-carousel-viewport");
  viewport.addEventListener("pointerdown", (event) => {
    qrSwipeStartX = event.clientX;
  });
  viewport.addEventListener("pointerup", (event) => {
    if (qrSwipeStartX === null) return;
    const distance = event.clientX - qrSwipeStartX;
    qrSwipeStartX = null;
    if (Math.abs(distance) < 50) return;
    qrSelectedIndex += distance < 0 ? 1 : -1;
    renderQrCarousel();
  });
  viewport.addEventListener("pointercancel", () => {
    qrSwipeStartX = null;
  });
  startExpiryCountdown();
}

async function renderQrPage() {
  clearInterval(qrRefreshTimer);
  appContent.innerHTML = `<section>${pageHeading("Your visits", "My QR pass", "Loading...")}</section>`;
  let lastAccount = localStorage.getItem(LAST_QR_ACCOUNT_KEY) || "";
  try {
    const [account, passes] = await Promise.all([
      requestJson("/api/v1/auth/me", { cache: "no-store" }),
      requestJson("/api/v1/me/passes", { cache: "no-store" })
    ]);
    qrAccountId = account.accountId;
    qrPassCards = sortQrPasses(passes);
    qrSelectedIndex = 0;
    if (lastAccount && lastAccount !== qrAccountId) await clearCachedQr(lastAccount);
    localStorage.setItem(LAST_QR_ACCOUNT_KEY, qrAccountId);

    const activePasses = qrPassCards.filter((pass) => pass.displayStatus === "ACTIVE");
    const qrResults = await Promise.all(activePasses.map(async (pass) => {
      try {
        const path = `${QR_ENDPOINT}?userPassId=${encodeURIComponent(pass.userPassId)}`;
        return { userPassId: pass.userPassId, record: await requestJson(path, { cache: "no-store" }) };
      } catch (error) {
        return { userPassId: pass.userPassId, error: error.message };
      }
    }));
    const records = qrResults.filter((result) => result.record).map((result) => result.record);
    qrRecordsByPassId = new Map(qrResults.map((result) => [result.userPassId, result]));
    let cacheMessage = "";
    try {
      await saveCachedQrs(qrAccountId, records);
    } catch (cacheError) {
      cacheMessage = "QR codes are ready, but could not be saved for offline use.";
    }

    if (currentPage() === "qr") {
      const carousel = qrPassCards.length
        ? `<div class="qr-carousel"></div>`
        : `<div class="empty-state"><h2>No pass yet</h2><p>Buy a pass to see it here.</p><a class="button button-dark" href="#buy">Explore passes</a></div>`;
      appContent.innerHTML = `<section class="qr-pass-list">${pageHeading("Your visits", "My QR pass")}${carousel}${cacheMessage ? `<p class="qr-cache-message">${escapeHtml(cacheMessage)}</p>` : ""}<button class="text-button cache-clear-button" data-action="clear-qr-cache">Remove saved QR</button></section>`;
      renderQrCarousel();
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
      const cached = await getCachedQrs(lastAccount);
      const valid = cached.filter((record) => new Date(record.expiresAt).getTime() > Date.now());
      if (valid.length) {
        qrPassCards = sortQrPasses(valid.map((record) => ({
          userPassId: record.userPassId,
          passName: record.passName,
          displayStatus: "ACTIVE"
        })));
        qrSelectedIndex = 0;
        qrRecordsByPassId = new Map(valid.map((record) => [
          record.userPassId,
          { record, isCached: true }
        ]));
        appContent.innerHTML = `<section class="qr-pass-list">${pageHeading("Your visits", "My QR pass")}<div class="qr-carousel"></div><button class="text-button cache-clear-button" data-action="clear-qr-cache">Remove saved QR</button></section>`;
        renderQrCarousel();
        return;
      }
    }
    appContent.innerHTML = `<section>${pageHeading("Your visits", "My QR pass")}<div class="empty-state"><h2>Not available</h2><p>${error.message}</p></div></section>`;
  }
}

function startExpiryCountdown() {
  clearInterval(qrRefreshTimer);
  const expiryElements = [...document.querySelectorAll("[data-expiry]")];
  const startElements = [...document.querySelectorAll("[data-starts-at]")];
  if (!expiryElements.length && !startElements.length) return;
  const update = () => {
    let activeCount = 0;
    let pendingStartCount = 0;
    expiryElements.forEach((el) => {
      const s = Math.max(0, Math.floor((new Date(el.dataset.expiry).getTime() - Date.now()) / 1000));
      el.innerHTML = s > 0 ? `Expires in <strong>${Math.floor(s/60).toString().padStart(2,"0")}:${(s%60).toString().padStart(2,"0")}</strong>` : "<strong>Expired.</strong>";
      if (s <= 0) el.closest(".qr-card")?.querySelector(".qr-image")?.remove();
      else activeCount += 1;
    });
    startElements.forEach((el) => {
      const secondsLeft = Math.max(0, Math.floor((new Date(el.dataset.startsAt).getTime() - Date.now()) / 1000));
      const days = Math.floor(secondsLeft / 86400);
      if (secondsLeft > 0) pendingStartCount += 1;
      if (secondsLeft === 0) {
        el.textContent = "Available now";
      } else if (secondsLeft >= 86400) {
        const hours = Math.floor((secondsLeft % 86400) / 3600);
        el.textContent = `${days} ${days === 1 ? "day" : "days"} ${hours} ${hours === 1 ? "hour" : "hours"}`;
      } else {
        const hours = Math.floor(secondsLeft / 3600);
        const minutes = Math.floor((secondsLeft % 3600) / 60);
        const seconds = secondsLeft % 60;
        el.textContent = [hours, minutes, seconds].map((value) => String(value).padStart(2, "0")).join(":");
      }
    });
    if (!activeCount && !pendingStartCount) clearInterval(qrRefreshTimer);
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
      <div class="history-item-copy"><strong class="history-attraction-name">${escapeHtml(item.attractionName)}</strong><p>${escapeHtml(item.passName)}</p><time>${escapeHtml(formatDate(item.usedAt))}</time></div>
      <span class="me-badge ${success ? "is-available" : "is-used"}">${label}</span>
    </article>`;
  }).join("");
  return `<div class="history-list">${rows}</div>`;
}

function historyPassRemaining(pass) {
  if (pass.displayStatus === "EXPIRED") return "Expired";
  const notStarted = pass.displayStatus === "NOT STARTED";
  const target = notStarted ? pass.startsAt : pass.endsAt;
  if (!target || !["ACTIVE", "NOT STARTED"].includes(pass.displayStatus)) return "-";
  const remainingMs = new Date(target).getTime() - Date.now();
  if (!notStarted && remainingMs <= 0) return "0 Days 0 Hours";
  if (notStarted && remainingMs <= 0) return "Starts in 0 Days 0 Hours";
  const hoursLeft = Math[notStarted ? "ceil" : "floor"](remainingMs / 3600000);
  const days = Math.floor(hoursLeft / 24);
  const hours = hoursLeft % 24;
  const remaining = `${days} ${days === 1 ? "Day" : "Days"} ${hours} ${hours === 1 ? "Hour" : "Hours"}`;
  return notStarted ? `Starts in ${remaining}` : remaining;
}

function historyPassCardMarkup(pass) {
  const passId = escapeHtml(pass.userPassId);
  const expanded = historyExpandedPassIds.has(pass.userPassId);
  const passCategory = historyPassCategory(pass);
  const statusClass = passCategory === "USED" && pass.displayStatus === "ACTIVE"
    ? "is-expired"
    : pass.displayStatus === "ACTIVE" ? "is-active"
      : pass.displayStatus === "EXPIRED" ? "is-expired" : "is-not-started";
  const statusLabel = passCategory === "USED" && pass.displayStatus === "ACTIVE"
    ? "USED"
    : pass.displayStatus;
  const benefits = pass.benefits.length
    ? `<ul class="me-benefits">${pass.benefits.map((benefit) => {
        const used = benefit.used >= benefit.limit;
        const displayedUsed = Math.min(benefit.used, benefit.limit);
        return `<li class="me-benefit"><span><strong>${escapeHtml(benefit.attractionName)}</strong><small>${escapeHtml(benefit.detail)}</small></span>
          <span class="me-badge ${used ? "is-used" : "is-available"}">${used ? "Used" : "Available"} (${escapeHtml(displayedUsed)}/${escapeHtml(benefit.limit)})</span></li>`;
      }).join("")}</ul>`
    : `<div class="empty-state"><h2>No benefits for this pass</h2></div>`;

  return `<article class="history-pass-card" data-history-category="${passCategory}">
    <button type="button" class="history-pass-toggle" data-action="history-toggle" data-user-pass-id="${passId}" aria-expanded="${expanded}">
      <span class="history-pass-head">
        <h2>${escapeHtml(pass.passName)}</h2>
        <span class="me-status history-pass-status ${statusClass}">${escapeHtml(statusLabel)}</span>
      </span>
      <svg class="history-chevron${expanded ? " is-expanded" : ""}" viewBox="0 0 24 24" aria-hidden="true"><path d="m6 9 6 6 6-6"></path></svg>
    </button>
    <div class="history-pass-detail">
      <p class="history-remaining-label">Remaining time</p>
      <strong class="history-remaining">${escapeHtml(historyPassRemaining(pass))}</strong>
      <p class="history-validity">Valid: ${escapeHtml(formatPassDate(pass.startDate))} - ${escapeHtml(formatPassDate(pass.endDate))}</p>
    </div>
    <div class="history-pass-collapsible"${expanded ? "" : " hidden"}>
      <section class="history-section"><h3 class="me-subtitle">Pass Benefits &amp; Quota</h3>${benefits}</section>
      <section class="history-section">
        <h3 class="me-subtitle">Check-in History</h3>
        ${historyListMarkup(pass.recentUsage)}
        <a class="me-link history-full-link" href="#history" data-action="history-full" data-user-pass-id="${passId}">View Full Usage History →</a>
      </section>
    </div>
  </article>`;
}

function historyPassCategory(pass) {
  if (pass.displayStatus === "NOT STARTED") return "NOT_STARTED";
  if (pass.displayStatus === "EXPIRED"
      || (pass.benefits.length > 0 && pass.benefits.every((benefit) => benefit.used >= benefit.limit))) {
    return "USED";
  }
  return pass.displayStatus === "ACTIVE" ? "ACTIVE" : "USED";
}

function sortHistoryPasses(passCards) {
  return [...passCards].sort((a, b) => {
    const startComparison = (a.startDate || "9999-12-31").localeCompare(b.startDate || "9999-12-31");
    if (startComparison !== 0) return startComparison;
    return (a.endDate || "9999-12-31").localeCompare(b.endDate || "9999-12-31");
  });
}

function historyOverviewMarkup(account, passCards) {
  const visiblePasses = passCards.filter((pass) =>
    historyPassFilter === "ALL" || historyPassCategory(pass) === historyPassFilter);
  const passContent = visiblePasses.length
    ? visiblePasses.map(historyPassCardMarkup).join("")
    : passCards.length
      ? `<div class="me-card me-empty"><strong>No passes in this category.</strong></div>`
      : `<div class="me-card me-empty"><strong>No pass yet</strong><a class="button button-dark" href="#buy">Buy a pass</a></div>`;
  const filters = [
    ["ALL", "All"],
    ["ACTIVE", "Active"],
    ["NOT_STARTED", "Not started"],
    ["USED", "Expired / used"]
  ];
  return `<section class="history-page">
    ${pageHeading("Your visits", "Pass history")}
    <article class="me-card me-identity history-profile"><strong>${escapeHtml(account.displayName)}</strong></article>
    <div class="history-filters" role="group" aria-label="Filter passes">
      ${filters.map(([filter, label]) => `<button type="button" class="history-filter${historyPassFilter === filter ? " is-selected" : ""}" data-action="history-filter" data-filter="${filter}" aria-pressed="${historyPassFilter === filter}">${label}</button>`).join("")}
    </div>
    <div class="history-pass-list">${passContent}</div>
  </section>`;
}

async function renderHistoryPage(userPassId = null) {
  const selectedPass = userPassId
    ? historyPassCards.find((pass) => pass.userPassId === userPassId)
    : null;
  const heading = selectedPass ? `${escapeHtml(selectedPass.passName)} Usage History` : "Pass history";
  appContent.innerHTML = `<section>${pageHeading("Your visits", heading, "Loading...")}</section>`;
  try {
    if (selectedPass) {
      const path = `${HISTORY_ENDPOINT}?userPassId=${encodeURIComponent(userPassId)}`;
      const history = await requestJson(path, { cache: "no-store" });
      appContent.innerHTML = `<section class="history-page">
        ${pageHeading("Your visits", heading)}
        <button type="button" class="button button-outline history-back" data-action="history-recent">Back</button>
        ${historyListMarkup(history)}
      </section>`;
      return;
    }

    const [account, passCards] = await Promise.all([
      requestJson("/api/v1/auth/me", { cache: "no-store" }),
      requestJson(MY_PASSES_ENDPOINT, { cache: "no-store" })
    ]);
    historyAccount = account;
    historyPassCards = sortHistoryPasses(passCards);
    const availableIds = new Set(passCards.map((pass) => pass.userPassId));
    historyExpandedPassIds = new Set(
      [...historyExpandedPassIds].filter((passId) => availableIds.has(passId)));
    if (!historyHasInitializedCards) {
      if (historyPassCards.length > 0) historyExpandedPassIds.add(historyPassCards[0].userPassId);
      historyHasInitializedCards = true;
    }
    appContent.innerHTML = historyOverviewMarkup(account, historyPassCards);
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

const PRIVACY_LABELS = {
  MARKETING_EMAILS: ["Marketing emails", "Offers and news about Chiang Rai Pass"],
  USAGE_ANALYTICS: ["Usage analytics", "Help improve the app"]
};

function privacyPreferencesCard(authState) {
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
  return `<div class="me-card" id="privacy-card" data-version="${escapeHtml(canChange ? p.noticeVersion : "")}">
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
    profileFeedback = "Privacy choice saved.";
    if (currentPage() === "privacy-preferences") await renderPrivacyPreferencesPage();
    else if (currentPage() === "profile") await renderProfilePage();
  } catch (e) { msg.textContent = e.message; button.disabled = false; }
});

// Privacy Notice modal, shared by Register and Profile. It is created once and lives outside
// #app-content, so showing it never re-renders the page or touches the route.
const PRIVACY_NOTICE_PAGE = "privacy.html?embed=1";
const REGISTER_CONSENT = '#account-form input[name="privacyNoticeAcknowledged"]';

function ensurePrivacyDialog() {
  let dialog = document.querySelector("#privacy-dialog");
  if (dialog) return dialog;
  dialog = document.createElement("dialog");
  dialog.id = "privacy-dialog";
  dialog.className = "privacy-dialog";
  dialog.setAttribute("aria-labelledby", "privacy-dialog-title");
  dialog.innerHTML = `<div class="privacy-dialog-body">
      <div class="privacy-dialog-head">
        <h2 id="privacy-dialog-title">Privacy Notice</h2>
        <button type="button" class="dialog-close" data-privacy-close aria-label="Close Privacy Notice">×</button>
      </div>
      <iframe class="privacy-dialog-frame" title="Privacy Notice"></iframe>
      <div class="privacy-dialog-foot" hidden>
        <label class="check-row"><input type="checkbox" id="privacy-dialog-accept"><span>I have read and accept the Privacy Notice.</span></label>
        <div class="privacy-dialog-actions">
          <button type="button" class="button button-outline" data-privacy-close>Close</button>
          <button type="button" class="button button-dark" id="privacy-dialog-confirm" disabled>Confirm</button>
        </div>
      </div>
    </div>`;
  document.body.appendChild(dialog);
  // A click on the dimmed area outside the dialog closes it, without accepting anything.
  dialog.addEventListener("click", (event) => { if (event.target === dialog) dialog.close(); });
  return dialog;
}

function openPrivacyDialog() {
  const dialog = ensurePrivacyDialog();
  const frame = dialog.querySelector("iframe");
  if (!frame.getAttribute("src")) frame.setAttribute("src", PRIVACY_NOTICE_PAGE);
  const consent = document.querySelector(REGISTER_CONSENT);
  const accept = dialog.querySelector("#privacy-dialog-accept");
  // On Register the visitor can accept here; elsewhere (Profile) the notice is read-only.
  dialog.querySelector(".privacy-dialog-foot").hidden = !consent;
  accept.checked = Boolean(consent && consent.checked);
  dialog.querySelector("#privacy-dialog-confirm").disabled = !accept.checked;
  if (!dialog.open) dialog.showModal();
}

document.addEventListener("click", (event) => {
  const link = event.target.closest("[data-privacy-modal]");
  if (link) {
    // Let Ctrl/Cmd/Shift-click and middle-click still open the page in a new tab.
    if (event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
    event.preventDefault();
    openPrivacyDialog();
    return;
  }
  if (event.target.closest("[data-privacy-close]")) {
    document.querySelector("#privacy-dialog")?.close();
    return;
  }
  if (event.target.closest("#privacy-dialog-confirm")) {
    const dialog = document.querySelector("#privacy-dialog");
    const consent = document.querySelector(REGISTER_CONSENT);
    if (consent && dialog.querySelector("#privacy-dialog-accept").checked) {
      consent.checked = true;
      consent.dispatchEvent(new Event("change", { bubbles: true }));
    }
    dialog.close();
  }
});

document.addEventListener("change", (event) => {
  if (event.target.id !== "privacy-dialog-accept") return;
  document.querySelector("#privacy-dialog-confirm").disabled = !event.target.checked;
});

document.addEventListener("click", (event) => {
  if (event.target.closest("[data-profile-retry]")) void renderProfilePage();
  if (event.target.closest("[data-privacy-retry]")) void renderPrivacyPreferencesPage();
});

function avatarMarkup(url) {
  if (url) return `<img class="me-avatar-img" src="${url}" alt="Profile picture" width="64" height="64">`;
  return `<svg viewBox="0 0 24 24" width="44" height="44" fill="none" stroke="currentColor" stroke-width="1.6" aria-hidden="true"><circle cx="12" cy="12" r="10.2"/><circle cx="12" cy="9.6" r="3.4"/><path d="M5.6 18.6c1.5-2.8 3.6-4.1 6.4-4.1s4.9 1.3 6.4 4.1"/></svg>`;
}

function settingsCard(account) {
  const passwordItem = account.hasPassword
    ? `<a class="settings-menu-item" href="#change-password"><span>Change password</span><span aria-hidden="true">›</span></a>`
    : `<div class="settings-menu-item is-disabled"><span>Change password</span><small>This account has no password.</small></div>`;
  return `<div class="me-card"><h2 class="me-subtitle">Settings</h2>
    <nav class="settings-menu" aria-label="Settings">
      ${passwordItem}
      <a class="settings-menu-item" href="privacy.html" target="_blank" rel="noopener" data-privacy-modal>
        <span>Privacy Notice</span><span aria-hidden="true">↗</span>
      </a>
      <a class="settings-menu-item" href="#privacy-preferences"><span>Privacy preferences</span><span aria-hidden="true">›</span></a>
    </nav>
  </div>`;
}

async function loadAvatarUrl() {
  if (avatarObjectUrl) { URL.revokeObjectURL(avatarObjectUrl); avatarObjectUrl = ""; }
  const response = await fetch(apiUrl("/api/v1/me/avatar"), { credentials: "include", cache: "no-store", signal: AbortSignal.timeout(8000) });
  if (response.status !== 200) return "";
  avatarObjectUrl = URL.createObjectURL(await response.blob());
  return avatarObjectUrl;
}

async function syncHeaderAvatar() {
  try {
    await requestJson("/api/v1/auth/me", { cache: "no-store" });
    setHeaderAvatar(await loadAvatarUrl());
  } catch (e) {
    setHeaderAvatar("");
  }
}

// Crops to a centred square and shrinks to 256 px, so uploads stay small and carry no photo metadata.
async function resizeToJpeg(file, size = 256) {
  let bitmap;
  try { bitmap = await createImageBitmap(file); } catch (e) { throw new Error("That file could not be read as a picture."); }
  const side = Math.min(bitmap.width, bitmap.height);
  const canvas = document.createElement("canvas");
  canvas.width = size; canvas.height = size;
  const context = canvas.getContext("2d");
  context.fillStyle = "#fff"; context.fillRect(0, 0, size, size);
  context.drawImage(bitmap, (bitmap.width - side) / 2, (bitmap.height - side) / 2, side, side, 0, 0, size, size);
  if (bitmap.close) bitmap.close();
  return new Promise((resolve, reject) => canvas.toBlob(
    (blob) => (blob ? resolve(blob) : reject(new Error("Could not process the picture."))), "image/jpeg", 0.85));
}

document.addEventListener("click", (event) => {
  const control = event.target.closest("[data-profile-edit]");
  if (!control) return;
  const card = control.closest(".me-identity");
  const form = card.querySelector("#edit-profile-form"), openButton = card.querySelector('[data-profile-edit="open"]');
  const editing = control.dataset.profileEdit === "open";
  form.hidden = !editing; openButton.hidden = editing;
  if (editing) form.elements.displayName.focus(); else form.reset();
});

document.addEventListener("submit", async (event) => {
  if (event.target.id !== "edit-profile-form") return;
  event.preventDefault();
  const form = event.target, msg = form.querySelector(".form-message"), btn = form.querySelector('[type="submit"]');
  const name = form.elements.displayName.value.trim();
  const file = form.elements.picture.files[0];
  if (!name) { msg.textContent = "Name is required."; return; }
  if (name.length > 120) { msg.textContent = "Name must be at most 120 characters."; return; }
  if (/[\u0000-\u001f\u007f]/.test(name)) { msg.textContent = "Name contains characters that are not allowed."; return; }
  if (file && !["image/jpeg", "image/png"].includes(file.type)) { msg.textContent = "Choose a JPEG or PNG picture."; return; }
  if (file && file.size > 10 * 1024 * 1024) { msg.textContent = "The picture is too large. Choose one under 10 MB."; return; }
  const nameChanged = name !== form.elements.displayName.defaultValue;
  if (!file && !nameChanged) { msg.textContent = "Nothing has changed."; return; }
  btn.disabled = true; msg.textContent = "Saving...";
  try {
    const csrf = await csrfHeaders();
    if (file) {
      const picture = await resizeToJpeg(file);
      await requestJson("/api/v1/me/avatar", { method: "PUT", headers: { ...csrf, "Content-Type": "image/jpeg" }, body: picture });
    }
    if (nameChanged) {
      await requestJson("/api/v1/me/profile", { method: "PATCH", headers: { ...csrf, "Content-Type": "application/json" }, body: JSON.stringify({ displayName: name }) });
    }
    profileFeedback = "Profile updated."; await renderProfilePage();
  } catch (e) { msg.textContent = e.message; btn.disabled = false; }
});

document.addEventListener("submit", async (event) => {
  if (event.target.id !== "password-form") return;
  event.preventDefault();
  const form = event.target, msg = form.querySelector(".form-message"), btn = form.querySelector('[type="submit"]');
  const current = form.elements.current.value, next = form.elements.password.value;
  if (!current) { msg.textContent = "Enter your current password."; return; }
  const problem = passwordProblem(next);
  if (problem) { msg.textContent = problem; return; }
  if (next !== form.elements.confirm.value) { msg.textContent = "The two new passwords do not match."; return; }
  btn.disabled = true; msg.textContent = "Saving...";
  try {
    const csrf = await csrfHeaders();
    const reply = await requestJson("/api/v1/me/password", { method: "POST", headers: { ...csrf, "Content-Type": "application/json" }, body: JSON.stringify({ currentPassword: current, newPassword: next }) });
    profileFeedback = reply.message;
    window.location.hash = "profile";
  } catch (e) { msg.textContent = e.message; btn.disabled = false; }
});

function changePasswordPage(account, notice = "") {
  if (!account.hasPassword) {
    return `<section>${pageHeading("Your account", "Change password")}
      <div class="profile-card"><p class="me-muted">This account has no password to change.</p>
        <a class="button button-outline button-full" href="#profile">Back to profile</a></div></section>`;
  }
  return `<section>${pageHeading("Your account", "Change password", "Choose a new password for your account.")}
    <div class="profile-card">
      ${notice ? `<p class="form-message">${escapeHtml(notice)}</p>` : ""}
      <form id="password-form" class="account-form" novalidate>
        <label class="field-label">Current password</label><input name="current" type="password" required autocomplete="current-password">
        <label class="field-label">New password</label><input name="password" type="password" required autocomplete="new-password">
        <small>At least 8 characters, with at least 1 number and 1 special character (! @ # $ %). English letters only.</small>
        <label class="field-label">Confirm new password</label><input name="confirm" type="password" required autocomplete="new-password">
        <button type="submit" class="button button-dark button-full">Change password</button><p class="form-message"></p>
        <a class="button button-outline button-full" href="#profile">Cancel</a>
      </form>
    </div></section>`;
}

async function renderChangePasswordPage() {
  appContent.innerHTML = `<section>${pageHeading("Your account", "Loading...")}</section>`;
  let account;
  try {
    account = await requestJson("/api/v1/auth/me", { cache: "no-store" });
  } catch (e) {
    if (currentPage() !== "change-password") return;
    profileFeedback = "Please sign in to change your password.";
    window.location.hash = "profile";
    return;
  }
  if (currentPage() !== "change-password") return;
  try { setHeaderAvatar(await loadAvatarUrl()); } catch (e) { setHeaderAvatar(""); }
  appContent.innerHTML = changePasswordPage(account, profileFeedback);
  profileFeedback = "";
}

function profilePage(authState) {
    if (authState.loadFailed) {
    return `<section>${pageHeading("Your account", "Profile")}
      <div class="empty-state"><h2>Could not load your account</h2>
      <p>We could not check your sign-in status just now. Please try again.</p>
      <button type="button" class="button button-outline" data-profile-retry>Try again</button></div></section>`;
    }
      if (authState.account) {
    const account = authState.account;
    return `<section>${pageHeading("Your account", "Profile")}
      ${authState.notice ? `<p class="form-message">${escapeHtml(authState.notice)}</p>` : ""}
      <div class="me-card me-identity">
        <div class="me-identity-row">
          <span class="me-avatar">${avatarMarkup(authState.avatarUrl)}</span>
          <span class="me-identity-text">
            <strong>${escapeHtml(account.displayName)}</strong>
            <small>${escapeHtml(account.email)}</small>
            <button type="button" class="button button-outline me-identity-edit" data-profile-edit="open">Edit Profile</button>
          </span>
        </div>
        <form id="edit-profile-form" class="account-form" hidden novalidate>
          <label class="field-label" for="edit-picture">Profile picture</label>
          <input id="edit-picture" name="picture" type="file" accept="image/jpeg,image/png">
          <small>JPEG or PNG. It is cropped to a square.</small>
          <label class="field-label" for="edit-name">Display name</label>
          <input id="edit-name" name="displayName" value="${escapeHtml(account.displayName)}" maxlength="120" required>
          <button type="submit" class="button button-dark button-full">Save</button>
          <button type="button" class="button button-outline button-full" data-profile-edit="cancel">Cancel</button>
          <p class="form-message"></p>
        </form>
      </div>
      ${settingsCard(account)}
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
        <button type="button" class="button button-outline button-full" data-action="resend-otp">Resend OTP</button>
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
      ${reg ? `<div class="consent-box"><p><a class="me-link" href="privacy.html" target="_blank" rel="noopener" data-privacy-modal>Read Privacy Notice ↗</a></p><label class="check-row"><input type="checkbox" name="privacyNoticeAcknowledged" required><span>I acknowledge the Privacy Notice.</span></label></div>` : ""}
      <button type="submit" class="button button-dark button-full">${reg ? "Create account" : "Sign in"} →</button><p id="account-message" class="form-message">${escapeHtml(authState.notice || authState.error)}</p>
    </form></div></section>`;
}

async function renderProfilePage() {
  const authState = { account: null, config: { registrationEnabled: false }, notice: "", error: "", loadFailed: false };
  appContent.innerHTML = `<section>${pageHeading("Your account", "Loading...")}</section>`;
  try {
    authState.config = await requestJson("/api/v1/auth/config", { cache: "no-store" });
    try {
      authState.account = await requestJson("/api/v1/auth/me", { cache: "no-store" });
      authState.notice = await clearQrForAccountSwitch(authState.account.accountId);
      try { authState.avatarUrl = await loadAvatarUrl(); } catch (e) { authState.avatarUrl = ""; }
      setHeaderAvatar(authState.avatarUrl);
    } catch (e) {
      // 401 means the visitor is signed out. Any other failure (timeout, 5xx, network) is a
      // failure to load, not a sign-out, so it must not show the Sign in form.
      if (!authState.account && e.status !== 401) authState.loadFailed = true;
    }
  } catch (e) { authState.loadFailed = true; }
  if (profileFeedback) { authState.notice = profileFeedback; profileFeedback = ""; }
  if (currentPage() === "profile") appContent.innerHTML = profilePage(authState);
}

async function renderPrivacyPreferencesPage() {
  appContent.innerHTML = `<section>${pageHeading("Your account", "Privacy preferences", "Loading...")}</section>`;
  let privacy;
  try {
    await requestJson("/api/v1/auth/me", { cache: "no-store" });
    privacy = await requestJson("/api/v1/me/privacy-preferences", { cache: "no-store" });
  } catch (error) {
    if (currentPage() !== "privacy-preferences") return;
    const body = error.status === 401
      ? `<div class="empty-state"><h2>Sign in required</h2><p>Your session may have expired. Return to Profile to sign in again.</p></div>`
      : `<div class="empty-state"><h2>Privacy preferences unavailable</h2><p>We could not load your preferences. Your sign-in status has not been changed.</p><button type="button" class="button button-outline" data-privacy-retry>Try again</button></div>`;
    appContent.innerHTML = `<section>${pageHeading("Your account", "Privacy preferences")}
      <a class="button button-outline privacy-back-link" href="#profile">← Back to Profile</a>${body}</section>`;
    return;
  }
  if (currentPage() !== "privacy-preferences") return;
  const notice = profileFeedback;
  profileFeedback = "";
  appContent.innerHTML = `<section>${pageHeading("Your account", "Privacy preferences", "Manage optional preferences.")}
    <a class="button button-outline privacy-back-link" href="#profile">← Back to Profile</a>
    ${notice ? `<p class="form-message">${escapeHtml(notice)}</p>` : ""}
    ${privacyPreferencesCard({ privacy, privacyError: false })}</section>`;
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
      startOtpCooldown(reply.retryAfterSeconds);
      profileMode = "otp"; profileFeedback = reply.message; await renderProfilePage(); return;
    }
    if (id === "otp-form") {
      const reply = await post("/api/v1/auth/password/verify-otp", { email: resetEmail, otp: form.elements.otp.value.trim() });
      resetToken = reply.token; profileMode = "newpass"; profileFeedback = reply.message; await renderProfilePage(); return;
    }
    const reply = await post("/api/v1/auth/password/reset", { token: resetToken, newPassword: password });
    resetEmail = ""; resetToken = ""; profileMode = "login"; profileFeedback = reply.message; await renderProfilePage();
  } catch (e) {
    if (id === "forgot-form" && e.status === 429 && Number.isFinite(e.retryAfterSeconds)) {
      // A code was already requested for this email a moment ago: carry on to the code screen.
      startOtpCooldown(e.retryAfterSeconds);
      profileMode = "otp";
      profileFeedback = "A code was sent to this email a moment ago. Enter it below, or request a new one when the timer ends.";
      await renderProfilePage(); return;
    }
    msg.textContent = e.message;
  } finally { btn.disabled = false; }
});

document.addEventListener("click", async (event) => {
  const button = event.target.closest('[data-action="resend-otp"]');
  if (!button || otpRemainingSeconds() > 0) return;
  const msg = button.closest("form").querySelector(".form-message");
  button.disabled = true; msg.textContent = "Sending...";
  try {
    const csrf = await csrfHeaders();
    const reply = await requestJson("/api/v1/auth/password/forgot", { method: "POST", headers: { ...csrf, "Content-Type": "application/json" }, body: JSON.stringify({ email: resetEmail }) });
    startOtpCooldown(reply.retryAfterSeconds);
    msg.textContent = reply.message;
  } catch (e) {
    if (Number.isFinite(e.retryAfterSeconds)) startOtpCooldown(e.retryAfterSeconds);
    msg.textContent = e.message;
  } finally { updateOtpButton(); }
});

function setProfileMode(mode) { profileMode = mode === "register" ? "register" : mode === "forgot" ? "forgot" : "login"; if (profileMode !== "forgot") { resetEmail = ""; resetToken = ""; } profileFeedback = ""; render(); }

async function render() {
  const page = currentPage();
  const navPage = ["change-password", "privacy-preferences"].includes(page) ? "profile" : page;
  document.querySelectorAll(".bottom-nav [data-page]").forEach(l => l.dataset.page === navPage ? l.setAttribute("aria-current", "page") : l.removeAttribute("aria-current"));
  updateCartCount();
  if (!isCatalogLoaded && (page === "home" || page === "buy")) { appContent.innerHTML = `<div class="loading-panel">Loading...</div>`; await fetchCatalogData(); }
  switch (page) {
    case "buy": appContent.innerHTML = buyPage(); break;
    case "qr": void renderQrPage(); break;
    case "history": void renderHistoryPage(); break;
    case "profile": void renderProfilePage(); break;
    case "change-password": void renderChangePasswordPage(); break;
    case "privacy-preferences": void renderPrivacyPreferencesPage(); break;
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
    case "qr-previous":
      qrSelectedIndex -= 1;
      renderQrCarousel();
      break;
    case "qr-next":
      qrSelectedIndex += 1;
      renderQrCarousel();
      break;
    case "history-toggle": {
      const userPassId = control.dataset.userPassId;
      const body = control.parentElement.querySelector(".history-pass-collapsible");
      const chevron = control.querySelector(".history-chevron");
      body.hidden = !body.hidden;
      control.setAttribute("aria-expanded", String(!body.hidden));
      chevron.classList.toggle("is-expanded", !body.hidden);
      if (body.hidden) historyExpandedPassIds.delete(userPassId);
      else historyExpandedPassIds.add(userPassId);
      break;
    }
    case "history-filter":
      historyPassFilter = control.dataset.filter;
      if (historyAccount) appContent.innerHTML = historyOverviewMarkup(historyAccount, historyPassCards);
      break;
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
    case "history-full":
      event.preventDefault();
      historyExpandedPassIds.add(control.dataset.userPassId);
      await renderHistoryPage(control.dataset.userPassId);
      break;
    case "history-recent": await renderHistoryPage(); break;
    case "logout":
      try { const csrf = await csrfHeaders(); await fetch(apiUrl("/api/v1/auth/logout"), { method: "POST", headers: csrf, credentials: "include" }); setHeaderAvatar(""); if (avatarObjectUrl) { URL.revokeObjectURL(avatarObjectUrl); avatarObjectUrl = ""; } try { await clearAllCachedQr(); localStorage.removeItem(LAST_QR_ACCOUNT_KEY); } catch (e) {} profileMode = "login"; profileFeedback = "Signed out."; render(); } catch (e) {} break;
    case "refresh-qr":
      if (!control.disabled) {
        control.disabled = true;
        try {
          const userPassId = control.dataset.userPassId;
          const path = `${QR_ENDPOINT}?userPassId=${encodeURIComponent(userPassId)}`;
          const record = await requestJson(path, { cache: "no-store" });
          const pass = qrPassCards.find((item) => item.userPassId === userPassId);
          if (pass) {
            qrRecordsByPassId.set(userPassId, { userPassId, record });
            renderQrCarousel();
          }
          try {
            const cached = await getCachedQrs(record.accountId);
            await saveCachedQrs(record.accountId, [
              ...cached.filter((item) => item.userPassId !== record.userPassId),
              record
            ]);
          } catch (cacheError) {
            const message = document.createElement("p");
            message.className = "qr-cache-message";
            message.textContent = "QR updated, but could not be saved for offline use.";
            appContent.querySelector(".qr-pass-list")?.append(message);
          }
        } catch (error) {
          qrRecordsByPassId.set(control.dataset.userPassId, { error: error.message });
          renderQrCarousel();
        }
      }
      break;
    case "clear-qr-cache": {
      const acc = localStorage.getItem(LAST_QR_ACCOUNT_KEY);
      if (acc) await clearCachedQr(acc);
      control.remove();
      const message = document.createElement("p");
      message.className = "qr-cache-message";
      message.textContent = "Saved QR codes removed from this device.";
      appContent.querySelector(".qr-pass-list")?.append(message);
      break;
    }
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
    await renderProfilePage();
  } catch (e) { msg.textContent = e.message; } finally { btn.disabled = false; }
});

window.addEventListener("hashchange", render);
if (currentPage() !== "profile") void syncHeaderAvatar();
render();

// ฟังก์ชันสร้างหน้า Tourist Attraction แสดงการ์ดแนวตั้งทั้ง 5 สถานที่
function touristAttractionsPage() {
  if (catalogLoadError || (isCatalogLoaded && attractions.length === 0)) {
    return `<section class="home-page">${emptyCatalogState()}</section>`;
  }

  const verticalList = attractions.map((item) => {
    // รองรับทั้งแบบ camelCase (imageUrl) และ snake_case (image_url)
    const imgPath = item.imageUrl || item.image_url;
    
    const imageHtml = imgPath 
      ? `<img src="${escapeHtml(imgPath)}" alt="${escapeHtml(item.name || item.publicName)}" class="attraction-horizontal-img" onerror="this.onerror=null; this.parentNode.innerHTML='<div class=\\'attraction-horizontal-placeholder\\'>${item.initials || 'TR'}</div>';">`
      : `<div class="attraction-horizontal-placeholder">${item.initials || 'TR'}</div>`;

    return `<article class="attraction-horizontal-card">
      <div class="attraction-horizontal-art">
        ${imageHtml}
      </div>
      <div class="attraction-horizontal-info">
        <p class="card-kicker">${escapeHtml(item.category || "ATTRACTION")} • ${escapeHtml(item.location || "")}</p>
        <h3>${escapeHtml(item.name || item.publicName)}</h3>
        <p>${escapeHtml(item.description)}</p>
      </div>
    </article>`;
  }).join("");

  return `<section class="home-page">
    <div class="attraction-page-header">
      <a href="#home" class="back-circle-btn" aria-label="Back to home">
        <svg viewBox="0 0 24 24"><polyline points="15 18 9 12 15 6"></polyline></svg>
      </a>
      <h1>Tourist Attraction</h1>
    </div>
    <div class="attractions-vertical-list">
      ${verticalList}
    </div>
  </section>`;
}

// Event Listener สำหรับสลับเปิดหน้า Tourist Attraction เมื่อ URL เป็น #attractions
window.addEventListener("hashchange", () => {
  if (window.location.hash === "#attractions") {
    fetchCatalogData().then(() => {
      appContent.innerHTML = touristAttractionsPage();
      window.scrollTo(0, 0);
    });
  }
});
