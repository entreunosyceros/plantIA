/* ============================================================
   PlantIA — interacciones de la interfaz
   ============================================================ */

/* ---------- Tema claro/oscuro ---------- */
function initThemeToggle() {
    const btn = document.getElementById("theme-toggle");
    if (!btn) return;

    const applyTheme = (theme) => {
        document.documentElement.setAttribute("data-theme", theme);
        try {
            localStorage.setItem("plantiaTheme", theme);
        } catch (e) {
            /* ignore */
        }
    };

    btn.addEventListener("click", () => {
        const current = document.documentElement.getAttribute("data-theme") || "light";
        applyTheme(current === "dark" ? "light" : "dark");
    });
}

/* ---------- Menú móvil ---------- */
function initMobileNav() {
    const toggle = document.getElementById("nav-toggle");
    const nav = document.getElementById("main-nav");
    if (!toggle || !nav) return;

    const close = () => {
        nav.classList.remove("is-open");
        toggle.setAttribute("aria-expanded", "false");
        toggle.setAttribute("aria-label", "Abrir menú");
    };

    toggle.addEventListener("click", () => {
        const open = nav.classList.toggle("is-open");
        toggle.setAttribute("aria-expanded", open ? "true" : "false");
        toggle.setAttribute("aria-label", open ? "Cerrar menú" : "Abrir menú");
    });

    nav.querySelectorAll("a").forEach((link) => {
        link.addEventListener("click", close);
    });

    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape") close();
    });
}

/* ---------- Notificaciones toast ---------- */
function showToast(message, type = "success", timeout = 4200) {
    const root = document.getElementById("toast-root");
    if (!root || !message) return;

    const toast = document.createElement("div");
    toast.className = `toast toast--${type}`;
    toast.setAttribute("role", type === "error" ? "alert" : "status");

    const icon = type === "error" ? "⚠️" : "✅";
    toast.innerHTML = `
        <span class="toast-icon" aria-hidden="true">${icon}</span>
        <span class="toast-body"></span>
        <button type="button" class="toast-close" aria-label="Cerrar">×</button>
    `;
    toast.querySelector(".toast-body").textContent = message;
    root.appendChild(toast);

    requestAnimationFrame(() => toast.classList.add("is-visible"));

    const dismiss = () => {
        toast.classList.remove("is-visible");
        window.setTimeout(() => toast.remove(), 400);
    };

    toast.querySelector(".toast-close").addEventListener("click", dismiss);
    if (timeout) window.setTimeout(dismiss, timeout);
}
window.showToast = showToast;

function initFlashToast() {
    const flash = document.body.getAttribute("data-flash");
    if (!flash) return;
    const type = document.body.getAttribute("data-flash-type") || "success";
    showToast(flash, type);
    // Ocultar la alerta en línea equivalente (respaldo sin JS)
    document.querySelectorAll(".alert").forEach((el) => el.classList.add("hidden"));
}

/* ---------- Lightbox (zoom de foto) ---------- */
function initLightbox() {
    const zoomables = document.querySelectorAll(".zoomable");
    if (zoomables.length === 0) return;

    let overlay = document.getElementById("lightbox");
    if (!overlay) {
        overlay = document.createElement("div");
        overlay.id = "lightbox";
        overlay.className = "lightbox";
        overlay.setAttribute("role", "dialog");
        overlay.setAttribute("aria-modal", "true");
        overlay.setAttribute("aria-label", "Imagen ampliada");
        overlay.innerHTML = `
            <button type="button" class="lightbox-close" aria-label="Cerrar">×</button>
            <img alt="Imagen ampliada">
        `;
        document.body.appendChild(overlay);
    }

    const img = overlay.querySelector("img");
    let lastFocus = null;

    const open = (src, alt) => {
        img.src = src;
        img.alt = alt || "Imagen ampliada";
        lastFocus = document.activeElement;
        overlay.classList.add("is-open");
        overlay.querySelector(".lightbox-close").focus();
    };

    const close = () => {
        overlay.classList.remove("is-open");
        img.src = "";
        if (lastFocus && lastFocus.focus) lastFocus.focus();
    };

    zoomables.forEach((el) => {
        el.addEventListener("click", () => open(el.getAttribute("src"), el.getAttribute("alt")));
    });

    overlay.addEventListener("click", (e) => {
        if (e.target === overlay || e.target.classList.contains("lightbox-close")) close();
    });
    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape" && overlay.classList.contains("is-open")) close();
    });
}

/* ---------- Micro-animaciones de aparición ---------- */
function initReveal() {
    const reduce = window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    document.querySelectorAll(".result-card, .detail-card").forEach((el) => {
        el.classList.add("animate-in");
    });
    if (reduce) return;
    document.querySelectorAll(".plant-grid .plant-card").forEach((el, i) => {
        el.style.animationDelay = `${Math.min(i * 45, 360)}ms`;
        el.classList.add("animate-in");
    });
}

/* ---------- Splash ---------- */
function initSplash() {
    const splash = document.getElementById("splash");
    const shouldShowSplash = document.documentElement.classList.contains("show-splash");
    if (splash && shouldShowSplash) {
        const durationMs = 4000;
        const bar = document.getElementById("splash-progress-bar");
        const progress = splash.querySelector(".splash-progress");
        const start = window.performance.now();

        if (bar && progress) {
            const tick = (now) => {
                const elapsed = Math.max(0, now - start);
                const pct = Math.min(100, (elapsed / durationMs) * 100);
                bar.style.width = `${pct}%`;
                progress.setAttribute("aria-valuenow", String(Math.round(pct)));
                if (elapsed < durationMs) {
                    window.requestAnimationFrame(tick);
                }
            };
            window.requestAnimationFrame(tick);
        }

        window.setTimeout(() => {
            splash.classList.add("splash-hide");
            window.setTimeout(() => splash.remove(), 400);
        }, durationMs);
        try {
            sessionStorage.setItem("plantiaSplashShown", "1");
        } catch (e) {
            /* ignore */
        }
    } else if (splash) {
        splash.remove();
    }
}

/* ---------- Subida de imagen ---------- */
function initUploadForm() {
    const form = document.getElementById("upload-form");
    if (!form) return;

    const dropZone = document.getElementById("drop-zone");
    const fileInput = document.getElementById("file-input");
    const browseBtn = document.getElementById("browse-btn");
    const preview = document.getElementById("preview");
    const submitBtn = document.getElementById("submit-btn");
    const dropContent = dropZone.querySelector(".drop-zone-content");
    const loading = document.getElementById("identify-loading");

    function showPreview(file) {
        const reader = new FileReader();
        reader.onload = (e) => {
            preview.src = e.target.result;
            preview.classList.remove("hidden");
            dropContent.classList.add("hidden");
            dropZone.classList.add("has-preview");
            submitBtn.disabled = false;
        };
        reader.readAsDataURL(file);
    }

    function handleFile(file) {
        if (!file || !file.type.startsWith("image/")) {
            showToast("Selecciona un archivo de imagen válido (JPEG, PNG o WebP).", "error");
            return;
        }
        const dt = new DataTransfer();
        dt.items.add(file);
        fileInput.files = dt.files;
        showPreview(file);
    }

    browseBtn.addEventListener("click", (e) => {
        e.preventDefault();
        e.stopPropagation();
        fileInput.click();
    });

    dropZone.addEventListener("click", () => fileInput.click());

    fileInput.addEventListener("change", () => {
        if (fileInput.files.length) handleFile(fileInput.files[0]);
    });

    dropZone.addEventListener("dragover", (e) => {
        e.preventDefault();
        dropZone.classList.add("dragover");
    });

    dropZone.addEventListener("dragleave", () => {
        dropZone.classList.remove("dragover");
    });

    dropZone.addEventListener("drop", (e) => {
        e.preventDefault();
        dropZone.classList.remove("dragover");
        if (e.dataTransfer.files.length) handleFile(e.dataTransfer.files[0]);
    });

    form.addEventListener("submit", () => {
        submitBtn.disabled = true;
        submitBtn.textContent = "Identificando…";
        form.classList.add("loading");
        if (loading) loading.classList.add("is-active");
    });
}

/* ---------- Candidatos ---------- */
function renderCandidates() {
    document.querySelectorAll(".candidate-grid").forEach((grid) => {
        const raw = grid.getAttribute("data-candidates") || "[]";
        let candidates = [];
        try {
            candidates = JSON.parse(raw);
        } catch (e) {
            candidates = [];
        }
        if (!Array.isArray(candidates) || candidates.length === 0) return;

        const escapeHtml = (s) =>
            String(s).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");

        grid.innerHTML = candidates
            .map((c) => {
                const name = escapeHtml(c.nombre_comun || c.nombre_cientifico || "Candidato");
                const sci = escapeHtml(c.nombre_cientifico || "");
                const reason = escapeHtml(c.razon || "");
                const conf = escapeHtml(c.confianza || "medio");
                const img = c.thumbnail_url || "";
                const imgTag = img
                    ? `<img class="candidate-thumb" src="${encodeURI(img)}" alt="${sci}">`
                    : `<div class="candidate-thumb candidate-thumb-placeholder" aria-hidden="true">🌿</div>`;

                const wikiUrl = c.nombre_cientifico
                    ? `https://en.wikipedia.org/wiki/${encodeURIComponent(
                          c.nombre_cientifico.trim().replace(/\s+/g, "_")
                      )}`
                    : "";
                const wikiOpen = wikiUrl
                    ? `<a class="candidate-wiki-link" href="${wikiUrl}" target="_blank" rel="noreferrer noopener">`
                    : "";
                const wikiClose = wikiUrl ? "</a>" : "";
                const wikiRow = wikiUrl
                    ? `<a class="candidate-wiki-badge" href="${wikiUrl}" target="_blank" rel="noreferrer noopener">Ver en Wikipedia ↗</a>`
                    : "";

                return `
                <div class="candidate-card">
                    ${imgTag}
                    <div class="candidate-body">
                        <div class="candidate-title">${wikiOpen}${name}${wikiClose}</div>
                        ${sci ? `<div class="candidate-sci"><em>${wikiOpen}${sci}${wikiClose}</em></div>` : ""}
                        <div class="candidate-meta">
                            <span class="badge badge-${conf}">${conf}</span>
                        </div>
                        ${reason ? `<div class="candidate-reason">${reason}</div>` : ""}
                        ${wikiRow}
                    </div>
                </div>`;
            })
            .join("");
    });
}

/* ---------- Cuaderno ---------- */
function formatJournalDate() {
    const now = new Date();
    const day = now.getDate();
    const months = [
        "enero", "febrero", "marzo", "abril", "mayo", "junio",
        "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
    ];
    return `${day} de ${months[now.getMonth()]}`;
}

function initJournalSections() {
    document.querySelectorAll(".journal-section").forEach((section) => {
        const textarea = section.querySelector(".journal-textarea");
        if (!textarea) return;

        section.querySelectorAll("[data-journal-template]").forEach((chip) => {
            chip.addEventListener("click", () => {
                const template = chip.getAttribute("data-journal-template") || "";
                const line = template.replace("{fecha}", formatJournalDate());
                const current = textarea.value.trimEnd();
                textarea.value = current ? `${current}\n${line}` : line;
                textarea.focus();
            });
        });

        if (section.getAttribute("data-focus-cuaderno") === "1") {
            section.scrollIntoView({ behavior: "smooth", block: "start" });
            window.setTimeout(() => textarea.focus(), 350);
        }
    });
}

/* ---------- Mostrar/ocultar API key ---------- */
function initRevealInputs() {
    document.querySelectorAll("[data-reveal-target]").forEach((btn) => {
        btn.addEventListener("click", () => {
            const target = document.getElementById(btn.getAttribute("data-reveal-target"));
            if (!target) return;
            const show = target.type === "password";
            target.type = show ? "text" : "password";
            btn.textContent = show ? "🙈" : "👁️";
            btn.setAttribute("aria-label", show ? "Ocultar" : "Mostrar");
        });
    });
}

/* ---------- Scroll to top (ficha) ---------- */
function initScrollTop() {
    const btn = document.getElementById("scroll-top-btn");
    if (!btn) return;
    const toggle = () => btn.classList.toggle("is-visible", window.scrollY > 280);
    toggle();
    window.addEventListener("scroll", toggle, { passive: true });
    btn.addEventListener("click", () => window.scrollTo({ top: 0, behavior: "smooth" }));
}

/* ---------- Init ---------- */
document.addEventListener("DOMContentLoaded", () => {
    initSplash();
    initThemeToggle();
    initMobileNav();
    initFlashToast();
    renderCandidates();
    initLightbox();
    initReveal();
    initUploadForm();
    initJournalSections();
    initRevealInputs();
    initScrollTop();
});
