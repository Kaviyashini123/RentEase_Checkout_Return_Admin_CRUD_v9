(function () {
  "use strict";

  document.addEventListener("DOMContentLoaded", function () {
    const cards = Array.from(document.querySelectorAll(".admin-vehicle-card"));
    const search = document.getElementById("fleetSearch");
    const status = document.getElementById("fleetStatusFilter");
    const type = document.getElementById("fleetTypeFilter");
    const count = document.getElementById("fleetVisibleCount");
    const filteredEmpty = document.getElementById("fleetFilterEmpty");
    const clearButton = document.getElementById("clearFleetFilters");
    const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    const hideTimers = new WeakMap();
    const filterTransitionMs = 220;

    function showCard(card) {
      const timer = hideTimers.get(card);
      if (timer) {
        window.clearTimeout(timer);
        hideTimers.delete(card);
      }

      if (!card.hidden && !card.classList.contains("is-filter-hiding")) return;

      card.hidden = false;
      card.classList.remove("is-filter-hiding");
      if (reducedMotion) {
        card.classList.remove("is-filter-entering");
        return;
      }

      card.classList.add("is-filter-entering");
      requestAnimationFrame(function () {
        requestAnimationFrame(function () {
          card.classList.remove("is-filter-entering");
        });
      });
    }

    function hideCard(card) {
      if (card.hidden || card.classList.contains("is-filter-hiding")) return;
      if (reducedMotion) {
        card.hidden = true;
        return;
      }

      card.classList.remove("is-filter-entering");
      card.classList.add("is-filter-hiding");
      const timer = window.setTimeout(function () {
        card.hidden = true;
        card.classList.remove("is-filter-hiding");
        hideTimers.delete(card);
      }, filterTransitionMs);
      hideTimers.set(card, timer);
    }

    function filterVehicles() {
      const term = (search ? search.value : "").trim().toLocaleLowerCase();
      const wantedStatus = status ? status.value : "";
      const wantedType = type ? type.value : "";
      let visible = 0;

      cards.forEach(function (card) {
        const text = [card.dataset.name, card.dataset.registration, card.dataset.location].filter(Boolean).join(" ").toLocaleLowerCase();
        const matches = (!term || text.includes(term)) && (!wantedStatus || card.dataset.status === wantedStatus) && (!wantedType || card.dataset.type === wantedType);
        if (matches) {
          visible += 1;
          showCard(card);
        } else {
          hideCard(card);
        }
      });

      if (count) count.textContent = String(visible);
      if (filteredEmpty && cards.length) filteredEmpty.hidden = visible !== 0;
    }

    if (cards.length) {
      if (search) search.addEventListener("input", filterVehicles);
      if (status) status.addEventListener("change", filterVehicles);
      if (type) type.addEventListener("change", filterVehicles);
      if (clearButton) {
        clearButton.addEventListener("click", function () {
          if (search) search.value = "";
          if (status) status.value = "";
          if (type) type.value = "";
          filterVehicles();
          if (search) search.focus();
        });
      }
    }

    const counters = Array.from(document.querySelectorAll(".admin-fleet-stat [data-count]"));
    function animateCounters() {
      if (reducedMotion) return;
      counters.forEach(function (element) {
        const target = Number(element.textContent.trim());
        if (!Number.isFinite(target) || target <= 0) return;
        const start = performance.now();
        const duration = 720;
        element.textContent = "0";

        function tick(now) {
          const progress = Math.min(1, (now - start) / duration);
          const eased = 1 - Math.pow(1 - progress, 3);
          element.textContent = String(Math.round(target * eased));
          if (progress < 1) requestAnimationFrame(tick);
        }
        requestAnimationFrame(tick);
      });
    }

    const stats = document.querySelector(".admin-fleet-stats");
    if (stats && counters.length && !reducedMotion && "IntersectionObserver" in window) {
      const observer = new IntersectionObserver(function (entries) {
        if (!entries.some(function (entry) { return entry.isIntersecting; })) return;
        observer.disconnect();
        animateCounters();
      }, { threshold: 0.25 });
      observer.observe(stats);
    } else if (stats && !reducedMotion) {
      animateCounters();
    }
  });
})();
