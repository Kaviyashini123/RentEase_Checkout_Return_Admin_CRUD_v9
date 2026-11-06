(function () {
  "use strict";

  document.addEventListener("DOMContentLoaded", function () {
    const workspace = document.querySelector(".reservation-ops-workspace");
    if (!workspace) return;

    const cards = Array.from(workspace.querySelectorAll("[data-reservation-card]"));
    const search = workspace.querySelector("[data-reservation-search]");
    const status = workspace.querySelector("[data-reservation-status]");
    const payment = workspace.querySelector("[data-payment-status]");
    const dateRange = workspace.querySelector("[data-date-range]");
    const resultCount = workspace.querySelector("[data-reservation-result-count]");
    const resultLabel = workspace.querySelector("[data-reservation-result-label]");
    const emptyState = workspace.querySelector("[data-reservation-empty]");
    const clearButtons = Array.from(workspace.querySelectorAll("[data-clear-reservation-filters]"));
    const actionMessage = workspace.querySelector("[data-reservation-action-message]");
    const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    const hideTimers = new WeakMap();
    const exitDuration = 220;

    function revealCard(card) {
      const pendingHide = hideTimers.get(card);
      if (pendingHide) {
        window.clearTimeout(pendingHide);
        hideTimers.delete(card);
      }
      if (!card.hidden && !card.classList.contains("is-filter-hiding")) return;

      card.hidden = false;
      card.classList.remove("is-filter-hiding");
      if (reducedMotion) return;
      card.classList.add("is-filter-entering");
      requestAnimationFrame(function () {
        requestAnimationFrame(function () { card.classList.remove("is-filter-entering"); });
      });
    }

    function concealCard(card) {
      if (card.hidden || card.classList.contains("is-filter-hiding")) return;
      if (reducedMotion) {
        card.hidden = true;
        return;
      }
      card.classList.remove("is-filter-entering");
      card.classList.add("is-filter-hiding");
      hideTimers.set(card, window.setTimeout(function () {
        card.hidden = true;
        card.classList.remove("is-filter-hiding");
        hideTimers.delete(card);
      }, exitDuration));
    }

    function matchesDate(card, range, today) {
      if (!range) return true;
      const start = card.dataset.startDate || "";
      const end = card.dataset.endDate || "";
      if (range === "upcoming") return start > today;
      if (range === "current") return start <= today && end >= today;
      if (range === "past") return end < today;
      return true;
    }

    function filterReservations() {
      const term = search ? search.value.trim().toLocaleLowerCase() : "";
      const wantedStatus = status ? status.value : "";
      const wantedPayment = payment ? payment.value : "";
      const wantedDate = dateRange ? dateRange.value : "";
      const now = new Date();
      const today = [now.getFullYear(), String(now.getMonth() + 1).padStart(2, "0"), String(now.getDate()).padStart(2, "0")].join("-");
      let matchesCount = 0;

      cards.forEach(function (card) {
        const searchable = [
          card.dataset.reservationId,
          card.dataset.customer,
          card.dataset.vehicle,
          card.dataset.registration,
          card.dataset.pickup,
          card.dataset.dropoff
        ].filter(Boolean).join(" ").toLocaleLowerCase();
        const matches = (!term || searchable.includes(term))
          && (!wantedStatus || card.dataset.status === wantedStatus)
          && (!wantedPayment || card.dataset.paymentStatus === wantedPayment)
          && matchesDate(card, wantedDate, today);

        if (matches) {
          matchesCount += 1;
          revealCard(card);
        } else {
          concealCard(card);
        }
      });

      if (resultCount) resultCount.textContent = String(matchesCount);
      if (resultLabel) resultLabel.textContent = matchesCount === 1 ? "reservation" : "reservations";
      if (emptyState) emptyState.hidden = matchesCount !== 0 || cards.length === 0;
    }

    [search, status, payment, dateRange].forEach(function (control) {
      if (!control) return;
      control.addEventListener(control.tagName === "INPUT" ? "input" : "change", filterReservations);
    });

    clearButtons.forEach(function (button) {
      button.addEventListener("click", function () {
        if (search) search.value = "";
        if (status) status.value = "";
        if (payment) payment.value = "";
        if (dateRange) dateRange.value = "";
        filterReservations();
        if (search) search.focus();
      });
    });

    workspace.querySelectorAll("[data-confirm-reservation]").forEach(function (button) {
      button.addEventListener("click", function () {
        const reservationId = button.dataset.reservationId;
        if (!reservationId) return;
        const endpoint = workspace.dataset.confirmEndpoint || "/api/reservation/";
        button.disabled = true;
        if (actionMessage) {
          actionMessage.hidden = true;
          actionMessage.textContent = "";
        }
        fetch(endpoint.replace(/\/?$/, "/") + encodeURIComponent(reservationId) + "/confirm", {
          method: "PUT",
          credentials: "same-origin",
          headers: { Accept: "application/json" }
        }).then(function (response) {
          if (!response.ok) throw new Error("The reservation could not be confirmed. Refresh and try again.");
          window.location.reload();
        }).catch(function (error) {
          button.disabled = false;
          if (actionMessage) {
            actionMessage.textContent = error.message || "The reservation could not be confirmed.";
            actionMessage.hidden = false;
          }
        });
      });
    });

    const counters = Array.from(workspace.querySelectorAll("[data-count-up]"));
    function animateCounters() {
      if (reducedMotion) return;
      counters.forEach(function (counter) {
        const target = Number(counter.textContent.trim());
        if (!Number.isFinite(target) || target <= 0) return;
        const start = performance.now();
        const duration = 680;
        counter.textContent = "0";
        function tick(now) {
          const progress = Math.min(1, (now - start) / duration);
          counter.textContent = String(Math.round(target * (1 - Math.pow(1 - progress, 3))));
          if (progress < 1) requestAnimationFrame(tick);
        }
        requestAnimationFrame(tick);
      });
    }

    if (counters.length && !reducedMotion && "IntersectionObserver" in window) {
      const observer = new IntersectionObserver(function (entries) {
        if (!entries.some(function (entry) { return entry.isIntersecting; })) return;
        observer.disconnect();
        animateCounters();
      }, { threshold: 0.2 });
      observer.observe(workspace.querySelector(".reservation-ops-stats") || workspace);
    } else {
      animateCounters();
    }

    if (!reducedMotion && "IntersectionObserver" in window && cards.length) {
      try {
        const cardObserver = new IntersectionObserver(function (entries) {
          entries.forEach(function (entry) {
            if (!entry.isIntersecting) return;
            entry.target.classList.add("is-visible");
            cardObserver.unobserve(entry.target);
          });
        }, { threshold: 0.08 });
        cards.forEach(function (card) { cardObserver.observe(card); });
        // Only enable the hidden-until-visible card state after observation
        // has been initialized successfully. Otherwise, keep real records
        // visible as a graceful fallback.
        workspace.classList.add("reservation-motion-enabled");
      } catch (error) {
        cards.forEach(function (card) { card.classList.add("is-visible"); });
        if (window.console && typeof window.console.warn === "function") {
          window.console.warn("Reservation card reveal unavailable; showing records without animation.", error);
        }
      }
    }
  });
})();
