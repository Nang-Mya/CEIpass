"use strict";

const isLocalDevelopment = ["localhost", "127.0.0.1"].includes(window.location.hostname);

window.CEIPASS_CONFIG = Object.freeze({
  apiBaseUrl: isLocalDevelopment ? "http://localhost:8080" : window.location.origin
});
