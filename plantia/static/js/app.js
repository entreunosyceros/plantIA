document.addEventListener("DOMContentLoaded", () => {
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
            window.setTimeout(() => {
                splash.remove();
            }, 400);
        }, durationMs);
        try {
            sessionStorage.setItem("plantiaSplashShown", "1");
        } catch (e) {
            // ignore
        }
    } else if (splash) {
        splash.remove();
    }

    const form = document.getElementById("upload-form");
    if (!form) return;

    const dropZone = document.getElementById("drop-zone");
    const fileInput = document.getElementById("file-input");
    const browseBtn = document.getElementById("browse-btn");
    const preview = document.getElementById("preview");
    const submitBtn = document.getElementById("submit-btn");
    const dropContent = dropZone.querySelector(".drop-zone-content");

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
            alert("Selecciona un archivo de imagen válido (JPEG, PNG o WebP).");
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
    });
});

document.addEventListener("DOMContentLoaded", () => {
    const grids = document.querySelectorAll(".candidate-grid");
    grids.forEach((grid) => {
        const raw = grid.getAttribute("data-candidates") || "[]";
        let candidates = [];
        try {
            candidates = JSON.parse(raw);
        } catch (e) {
            candidates = [];
        }
        if (!Array.isArray(candidates) || candidates.length === 0) return;

        grid.innerHTML = candidates
            .map((c) => {
                const name = c.nombre_comun || c.nombre_cientifico || "Candidato";
                const sci = c.nombre_cientifico || "";
                const reason = c.razon || "";
                const conf = c.confianza || "medio";
                const img = c.thumbnail_url || "";
                const imgTag = img
                    ? `<img class="candidate-thumb" src="${img}" alt="${sci}">`
                    : `<div class="candidate-thumb candidate-thumb-placeholder" aria-hidden="true">🌿</div>`;

                // Wikipedia en inglés. Usamos el nombre científico como título del artículo.
                const wikiUrl = sci
                    ? `https://en.wikipedia.org/wiki/${encodeURIComponent(
                          sci.trim().replace(/\s+/g, "_")
                      )}`
                    : "";
                const wikiLinkOpen = wikiUrl
                    ? `<a class="candidate-wiki-link" href="${wikiUrl}" target="_blank" rel="noreferrer noopener">`
                    : "";
                const wikiLinkClose = wikiUrl ? `</a>` : "";

                return `
                <div class="candidate-card">
                    ${imgTag}
                    <div class="candidate-body">
                        <div class="candidate-title">
                            ${wikiLinkOpen}${name}${wikiLinkClose}
                        </div>
                        ${sci ? `<div class="candidate-sci"><em>${wikiLinkOpen}${sci}${wikiLinkClose}</em></div>` : ""}
                        <div class="candidate-meta">
                            <span class="badge badge-${conf}">${conf}</span>
                        </div>
                        ${reason ? `<div class="candidate-reason">${reason}</div>` : ""}
                    </div>
                </div>
                `;
            })
            .join("");
    });
});

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

document.addEventListener("DOMContentLoaded", initJournalSections);
