// =====================================================================
//  Login state
// =====================================================================
let token = localStorage.getItem("travelsyncToken") || "";
let authMode = "login";
let feedTimer = null;

// small helper: make an HTML element with a class and text
function el(tag, cls, text) {
  const e = document.createElement(tag);
  if (cls) e.className = cls;
  if (text !== undefined) e.textContent = text;
  return e;
}

function show(id, text) {
  document.getElementById(id).textContent = text;
}

// Talks to the Java server and automatically sends our login token.
async function api(path, options) {
  options = options || {};
  options.headers = Object.assign({ "X-Token": token }, options.headers || {});
  try {
    const res = await fetch(path, options);
    let data = {};
    try { data = await res.json(); } catch (e) { data = {}; }
    if (res.status === 401 && path !== "/api/login" && path !== "/api/signup") {
      showLogin();   // our login is no longer valid (for example the server restarted)
    }
    return { ok: res.ok, status: res.status, data: data };
  } catch (e) {
    return { ok: false, status: 0, data: { error: "Could not reach the server. Is WebServer.java running?" } };
  }
}

function postForm(path, fields) {
  return api(path, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams(fields).toString()
  });
}

// =====================================================================
//  Tabs
// =====================================================================
function openTab(name) {
  document.querySelectorAll(".tab-btn").forEach(b => b.classList.toggle("active", b.dataset.tab === name));
  document.querySelectorAll(".tab-content").forEach(c => c.classList.toggle("active", c.id === name));
  if (name === "trips") loadTrips();
  if (name === "feed") refreshFeed();
}

document.querySelectorAll(".tab-btn").forEach(btn => {
  btn.addEventListener("click", () => openTab(btn.dataset.tab));
});

// =====================================================================
//  Login / sign up
// =====================================================================
function showLogin() {
  token = "";
  localStorage.removeItem("travelsyncToken");
  stopFeed();
  document.getElementById("user-name").textContent = "-";
  document.getElementById("auth-overlay").style.display = "flex";
}

function onLoggedIn(username) {
  document.getElementById("user-name").textContent = username;
  document.getElementById("auth-overlay").style.display = "none";
  document.getElementById("auth-error").textContent = "";
  openTab("explore");
  searchDestinations();
  startFeed();
}

function setAuthMode(mode) {
  authMode = mode;
  document.getElementById("auth-tab-login").classList.toggle("active", mode === "login");
  document.getElementById("auth-tab-signup").classList.toggle("active", mode === "signup");
  document.getElementById("auth-submit").textContent = mode === "login" ? "Log in" : "Create account";
  document.getElementById("auth-error").textContent = "";
}

async function submitAuth() {
  const username = document.getElementById("auth-username").value.trim();
  const password = document.getElementById("auth-password").value;
  const path = authMode === "signup" ? "/api/signup" : "/api/login";
  const r = await postForm(path, { username: username, password: password });
  if (!r.ok) {
    document.getElementById("auth-error").textContent = r.data.error || "Something went wrong.";
    return;
  }
  token = r.data.token;
  localStorage.setItem("travelsyncToken", token);
  document.getElementById("auth-password").value = "";
  onLoggedIn(r.data.username);
}

async function logout() {
  await postForm("/api/logout", {});
  showLogin();
}

// on page load: do we already have a valid login?
async function checkSession() {
  if (!token) {
    showLogin();
    return;
  }
  const r = await api("/api/me");
  if (r.ok) onLoggedIn(r.data.username);
  else showLogin();
}

document.getElementById("explore-input").addEventListener("keydown", e => {
  if (e.key === "Enter") searchDestinations();
});
["auth-username", "auth-password"].forEach(id => {
  document.getElementById(id).addEventListener("keydown", e => {
    if (e.key === "Enter") submitAuth();
  });
});

// =====================================================================
//  Explore & Book
// =====================================================================
function notice(text, kind) {
  const box = document.getElementById("explore-msg");
  box.hidden = false;
  box.className = "notice " + kind;
  box.textContent = text;
  return box;
}

async function searchDestinations() {
  const q = document.getElementById("explore-input").value;
  const r = await api("/api/destinations?q=" + encodeURIComponent(q));
  if (!r.ok) return;

  const grid = document.getElementById("explore-results");
  grid.innerHTML = "";
  document.getElementById("explore-msg").hidden = true;

  if (r.data.results.length === 0) {
    const box = notice('No matches for "' + q + '".', "info");
    if (r.data.suggestion) {
      box.textContent = 'No matches for "' + q + '". Did you mean ';
      const b = el("button", "link-btn", r.data.suggestion + "?");
      b.onclick = () => {
        document.getElementById("explore-input").value = r.data.suggestion;
        searchDestinations();
      };
      box.appendChild(b);
    }
    return;
  }
  r.data.results.forEach(d => grid.appendChild(destinationCard(d)));
}

function destinationCard(d) {
  const card = el("div", "dest-card");

  const head = el("div", "dest-head");
  head.appendChild(el("h3", "", d.name));
  head.appendChild(el("span", "chip", d.country));
  card.appendChild(head);

  card.appendChild(el("p", "dest-desc", d.description));
  card.appendChild(el("div", "dest-meta", d.searches + " searches"));

  d.hotels.forEach(h => {
    const row = el("div", "hotel-row");

    const info = el("div", "hotel-info");
    info.appendChild(el("div", "hotel-name", h.name));
    info.appendChild(el("div", "hotel-sub", h.description + " (" + h.price + " per night)"));
    row.appendChild(info);

    const nights = el("input", "nights-input");
    nights.type = "number";
    nights.min = 1;
    nights.max = 30;
    nights.value = 2;
    nights.title = "Number of nights";
    row.appendChild(nights);

    const btn = el("button", "", "Book");
    btn.onclick = () => bookHotel(h.id, nights.value);
    row.appendChild(btn);

    card.appendChild(row);
  });
  return card;
}

async function bookHotel(hotelId, nights) {
  const r = await postForm("/api/book", { hotelId: hotelId, nights: nights });
  let box;
  if (r.ok) {
    box = notice("Booked! Code " + r.data.code + ": " + r.data.hotel + " in " + r.data.destination +
      ", " + r.data.nights + " night(s), total " + r.data.total + ". See it under My Trips.", "ok");
  } else {
    box = notice(r.data.error || "Booking failed.", "err");
  }
  box.scrollIntoView({ behavior: "smooth", block: "nearest" });
}

// =====================================================================
//  My Trips
// =====================================================================
function timeAgo(ms) {
  const s = Math.max(0, Math.floor(ms / 1000));
  if (s < 5) return "just now";
  if (s < 60) return s + " s ago";
  if (s < 3600) return Math.floor(s / 60) + " min ago";
  if (s < 86400) return Math.floor(s / 3600) + " h ago";
  return Math.floor(s / 86400) + " d ago";
}

async function loadTrips() {
  const r = await api("/api/mytrips");
  const list = document.getElementById("trips-list");
  list.innerHTML = "";
  if (!r.ok) return;

  if (r.data.length === 0) {
    list.appendChild(el("p", "empty", "No trips yet. Book one from Explore & Book."));
    return;
  }
  r.data.forEach(t => {
    const row = el("div", "list-row");

    const left = el("div");
    left.appendChild(el("div", "code", t.code));
    left.appendChild(el("div", "", t.hotel + " in " + t.destination));
    left.appendChild(el("div", "sub", t.nights + " night(s) | booked " + new Date(t.createdAt).toLocaleString()));
    row.appendChild(left);

    row.appendChild(el("div", "price", String(t.total)));
    list.appendChild(row);
  });
}

// =====================================================================
//  Live Feed (the page asks the server again every 3 seconds)
// =====================================================================
function startFeed() {
  stopFeed();
  feedTimer = setInterval(() => {
    if (document.getElementById("feed").classList.contains("active")) refreshFeed();
  }, 3000);
}

function stopFeed() {
  if (feedTimer) clearInterval(feedTimer);
  feedTimer = null;
}

async function refreshFeed() {
  const r = await api("/api/feed");
  if (!r.ok) return;
  const d = r.data;

  document.getElementById("stat-bookings").textContent = d.totalBookings;
  document.getElementById("stat-nights").textContent = d.totalNights;
  document.getElementById("stat-value").textContent = Number(d.totalValue).toLocaleString();

  const bars = document.getElementById("feed-bars");
  bars.innerHTML = "";
  const max = Math.max(1, ...d.byDestination.map(x => x.count));
  d.byDestination.forEach(x => {
    const row = el("div", "bar-row");
    row.appendChild(el("span", "", x.name));
    const track = el("div", "bar-track");
    const fill = el("div", "bar-fill");
    fill.style.width = (100 * x.count / max) + "%";
    track.appendChild(fill);
    row.appendChild(track);
    row.appendChild(el("span", "bar-count", String(x.count)));
    bars.appendChild(row);
  });

  const recent = document.getElementById("feed-recent");
  recent.innerHTML = "";
  d.recent.forEach(b => {
    const row = el("div", "list-row");
    const left = el("div");
    left.appendChild(el("div", "code", b.code));
    left.appendChild(el("div", "", b.user + " booked " + b.hotel + " in " + b.destination));
    left.appendChild(el("div", "sub", b.nights + " night(s)"));
    row.appendChild(left);
    row.appendChild(el("div", "sub", timeAgo(d.now - b.createdAt)));
    recent.appendChild(row);
  });
}

async function simulateBookings() {
  await postForm("/api/simulate", { count: 5 });
  refreshFeed();
}

// =====================================================================
//  Algorithm Lab
// =====================================================================

// for the endpoints that send back {"text": "..."}
async function getText(url, outId) {
  try {
    const res = await fetch(url);
    const data = await res.json();
    show(outId, data.text);
  } catch (e) {
    show(outId, "Could not reach the server. Is WebServer.java running?");
  }
}

// ---- M1 ----
async function kmpSearch() {
  const q = document.getElementById("m1-input").value;
  const res = await fetch("/api/kmp-search?q=" + encodeURIComponent(q));
  const data = await res.json();
  show("m1-output", data.length ? data.map(n => "Found in: " + n).join("\n") : "No matches.");
}

async function rabinKarpSearch() {
  const q = document.getElementById("m1-input").value;
  const res = await fetch("/api/rabinkarp?q=" + encodeURIComponent(q));
  const data = await res.json();
  show("m1-output", data.length ? data.join("\n") : "No bookings found.");
}

// ---- M2 ----
async function suffixSearch() {
  const q = document.getElementById("m2-input").value;
  const res = await fetch("/api/suffix-search?q=" + encodeURIComponent(q));
  const data = await res.json();
  show("m2-output", data.length ? data.join("\n") : "No matches.");
}

async function similarPlaces() {
  const res = await fetch("/api/similar");
  const data = await res.json();
  show("m2-output", data.result);
}

// ---- M3 ----
async function spellCheck() {
  const q = document.getElementById("m3-input").value;
  const res = await fetch("/api/spellcheck?q=" + encodeURIComponent(q));
  const data = await res.json();
  show("m3-output", "Did you mean: " + data.suggestion + " (distance " + data.distance + ")");
}

async function planTrip() {
  const res = await fetch("/api/trip");
  const data = await res.json();
  show("m3-trip-output", "Cost: " + data.cost + "\nRoute: " + data.route.join(" -> "));
}

// ---- M4 ----
async function computeFlow() {
  const res = await fetch("/api/flow");
  const data = await res.json();
  show("m4-output",
    "Ford-Fulkerson: " + data.fordFulkerson +
    "\nEdmonds-Karp:  " + data.edmondsKarp +
    "\nDinic:         " + data.dinic);
}

async function computeMinCut() {
  const res = await fetch("/api/mincut");
  const data = await res.json();
  show("m4-output", "Min-cut value: " + data.value + "\nBottleneck links:\n" + data.edges.join("\n"));
}

async function computeMatch() {
  const res = await fetch("/api/match");
  const data = await res.json();
  show("m4-output", data.join("\n"));
}

// ---- M5 ----
function runNp(which) {
  getText("/api/np?case=" + which, "m5-output");
}

function runVertexCover() {
  getText("/api/vertexcover", "m5-output");
}

// ---- M6 ----
function runRank(by) {
  getText("/api/rank?by=" + by, "m6-rank-output");
}

function runReservoir(mode) {
  getText("/api/reservoir?mode=" + mode, "m6-reservoir-output");
}

function runPrime() {
  const n = document.getElementById("m6-prime-input").value;
  getText("/api/prime?n=" + encodeURIComponent(n), "m6-prime-output");
}

function runScan() {
  getText("/api/scan", "m6-scan-output");
}

function runReduce() {
  getText("/api/reduce", "m6-reduce-output");
}

function runBrent() {
  const n = document.getElementById("m6-brent-input").value;
  getText("/api/brent?n=" + encodeURIComponent(n), "m6-brent-output");
}

// start: check if we are already logged in
checkSession();