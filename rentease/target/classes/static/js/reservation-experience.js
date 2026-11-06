(function () {
  "use strict";

  const localVehicleImages = {
    "toyota|corolla axio": "/images/vehicles/toyota-corolla-axio.jpg",
    "honda|vezel": "/images/vehicles/honda-vezel.jpg",
    "suzuki|wagon r": "/images/vehicles/suzuki-wagon-r.png",
    "toyota|hiace": "/images/vehicles/toyota-hiace.jpg",
    "bmw|5 series": "/images/vehicles/bmw-5-series.jpg",
    "yamaha|fz-s": "/images/vehicles/yamaha-fz-s.jpg",
    "nissan|caravan": "/images/vehicles/nissan-caravan.jpg",
    "toyota|prius": "/images/vehicles/toyota-prius.jpg",
    "tesla|model 3": "/images/vehicles/tesla-model-3.jpg"
  };
  const pendingReservationKey = "rentease.pendingReservationRequest";
  const vehicleRequests = new Map();

  document.addEventListener("DOMContentLoaded", function () {
    const panel = document.querySelector("[data-reservation-vehicle]");
    if (panel) loadSelectedVehicle(panel);
    setupReservationSubmission();
    setupJourneyDashboard();
  });

  function loadSelectedVehicle(panel) {
    const id = panel.dataset.vehicleId;
    if (!id || !/^\d+$/.test(id)) {
      showUnavailable(panel, "Choose a vehicle before continuing.");
      return;
    }
    fetchVehicle(panel.dataset.vehicleEndpoint || "/api/booking/", id)
      .then(function (vehicle) { renderVehicle(panel, vehicle); })
      .catch(function () { showUnavailable(panel, "Vehicle details are temporarily unavailable. You can still return to the fleet and choose a vehicle."); });
  }

  function fetchVehicle(endpoint, id) {
    if (vehicleRequests.has(String(id))) return vehicleRequests.get(String(id));
    const request = fetch(endpoint.replace(/\/?$/, "/") + encodeURIComponent(id), {
      credentials: "same-origin",
      headers: { Accept: "application/json" }
    }).then(function (response) {
      if (!response.ok) throw new Error("Vehicle details could not be loaded.");
      return response.json();
    });
    vehicleRequests.set(String(id), request);
    return request;
  }

  function setupReservationSubmission() {
    const form = document.querySelector(".customer-reservation #booking-form");
    if (!form) return;
    try { sessionStorage.removeItem(pendingReservationKey); } catch (_) { /* Storage may be unavailable. */ }
    form.addEventListener("submit", function (event) {
      if (event.defaultPrevented) return;
      const value = function (name) {
        const field = form.elements[name];
        return field ? String(field.value || "").trim() : "";
      };
      const request = {
        vehicleId: value("vehicleId"),
        startDate: value("startDate"),
        endDate: value("endDate"),
        pickup: value("pickupLocation"),
        dropoff: value("dropoffLocation")
      };
      if (!request.vehicleId || !request.startDate || !request.endDate) return;
      try { sessionStorage.setItem(pendingReservationKey, JSON.stringify(request)); } catch (_) { /* Reservation submission remains unaffected. */ }
    });
  }

  function setupJourneyDashboard() {
    const dashboard = document.querySelector(".reservation-dashboard.customer-dashboard");
    if (!dashboard) return;
    const endpoint = dashboard.dataset.vehicleEndpoint || "/api/booking/";
    const records = Array.from(dashboard.querySelectorAll("[data-reservation-record]"));
    records.forEach(function (record) {
      const id = record.dataset.vehicleId;
      if (!id || !/^\d+$/.test(id)) return;
      fetchVehicle(endpoint, id).then(function (vehicle) {
        const name = [vehicle.brand, vehicle.model].filter(Boolean).join(" ");
        const nameNode = record.querySelector("[data-journey-vehicle-name]");
        const categoryNode = record.querySelector("[data-journey-vehicle-category]");
        if (nameNode && name) nameNode.textContent = name;
        if (categoryNode && vehicle.type) categoryNode.textContent = formatLabel(vehicle.type).toUpperCase();
        if (record.hasAttribute("data-journey-card")) {
          setVehicleImage(record, vehicle, "[data-journey-vehicle-image]", "[data-journey-image-placeholder]");
        }
      }).catch(function () { /* Keep the existing vehicle reference visible if details are unavailable. */ });
    });

    let submittedRequest = null;
    try {
      submittedRequest = JSON.parse(sessionStorage.getItem(pendingReservationKey) || "null");
      sessionStorage.removeItem(pendingReservationKey);
    } catch (_) { submittedRequest = null; }
    if (!submittedRequest) return;

    const successfulRecord = records.find(function (record) {
      return record.dataset.vehicleId === submittedRequest.vehicleId
        && record.dataset.startDate === submittedRequest.startDate
        && record.dataset.endDate === submittedRequest.endDate
        && (record.dataset.pickup || "") === submittedRequest.pickup
        && (record.dataset.dropoff || "") === submittedRequest.dropoff;
    });
    if (!successfulRecord) return;
    showConfirmation(dashboard, successfulRecord, endpoint);
  }

  function showConfirmation(dashboard, record, endpoint) {
    const confirmation = dashboard.querySelector("[data-reservation-confirmation]");
    if (!confirmation) return;
    setText(confirmation, "[data-confirmation-start]", formatDate(record.dataset.startDate));
    setText(confirmation, "[data-confirmation-end]", formatDate(record.dataset.endDate));
    setText(confirmation, "[data-confirmation-pickup]", record.dataset.pickup || "Not specified");
    setText(confirmation, "[data-confirmation-dropoff]", record.dataset.dropoff || "Not specified");
    setText(confirmation, "[data-confirmation-status]", formatLabel(record.dataset.status).toUpperCase());

    const total = Number(record.dataset.estimatedCost);
    const totalRow = confirmation.querySelector("[data-confirmation-total-row]");
    if (record.dataset.estimatedCost && Number.isFinite(total) && total > 0) {
      setText(confirmation, "[data-confirmation-total]", "LKR " + formatMoney(total));
      if (totalRow) totalRow.hidden = false;
    }

    confirmation.hidden = false;
    dashboard.classList.add("has-reservation-confirmation");
    fetchVehicle(endpoint, record.dataset.vehicleId).then(function (vehicle) {
      const name = [vehicle.brand, vehicle.model].filter(Boolean).join(" ");
      setText(confirmation, "[data-confirmation-name]", name);
      setText(confirmation, "[data-confirmation-category]", formatLabel(vehicle.type).toUpperCase());
      const rate = Number(vehicle.dailyRate);
      setText(confirmation, "[data-confirmation-rate]", Number.isFinite(rate) ? "LKR " + formatMoney(rate) : "");
      setVehicleImage(confirmation, vehicle, "[data-confirmation-image]", "[data-confirmation-placeholder]");
    }).catch(function () {
      setText(confirmation, "[data-confirmation-name]", "Vehicle #" + record.dataset.vehicleId);
      const placeholder = confirmation.querySelector("[data-confirmation-placeholder]");
      if (placeholder) placeholder.textContent = "VEHICLE IMAGE UNAVAILABLE";
    });
  }

  function renderVehicle(panel, vehicle) {
    const fullName = [vehicle.brand, vehicle.model].filter(Boolean).join(" ");
    setText(panel, "[data-vehicle-name]", fullName || "Selected vehicle");
    setText(panel, "[data-vehicle-category]", formatLabel(vehicle.type) || "SELECTED VEHICLE");
    setText(panel, "[data-vehicle-status]", formatLabel(vehicle.status));
    setText(panel, "[data-vehicle-transmission]", formatLabel(vehicle.transmission));
    setText(panel, "[data-vehicle-fuel]", formatLabel(vehicle.fuelType));
    setText(panel, "[data-vehicle-seats]", vehicle.seatingCapacity ? vehicle.seatingCapacity + (Number(vehicle.seatingCapacity) === 1 ? " seat" : " seats") : "");
    setText(panel, "[data-vehicle-location]", vehicle.location ? "LOCATION · " + vehicle.location : "");
    const description = panel.querySelector("[data-vehicle-description]");
    if (description && vehicle.description) {
      description.textContent = vehicle.description;
      description.hidden = false;
    }

    const price = Number(vehicle.dailyRate);
    setText(panel, "[data-vehicle-price]", Number.isFinite(price)
      ? "LKR " + price.toLocaleString("en-LK", { minimumFractionDigits: 2, maximumFractionDigits: 2 })
      : "");

    ["transmission", "fuel", "seats"].forEach(function (key) {
      const value = key === "transmission" ? vehicle.transmission : key === "fuel" ? vehicle.fuelType : vehicle.seatingCapacity;
      const row = panel.querySelector("[data-spec-" + key + "]");
      if (row && (value === null || value === undefined || value === "")) row.hidden = true;
    });

    const status = panel.querySelector("[data-vehicle-status]");
    if (status && vehicle.status) {
      status.hidden = false;
      status.dataset.status = String(vehicle.status).toLowerCase();
    }

    setVehicleImage(panel, vehicle, "[data-vehicle-image]", "[data-vehicle-placeholder]");

    panel.dataset.loadState = "ready";
  }

  function setVehicleImage(root, vehicle, imageSelector, placeholderSelector) {
    const image = root.querySelector(imageSelector);
    const placeholder = root.querySelector(placeholderSelector);
    const localKey = String(vehicle.brand || "").trim().toLowerCase() + "|" + String(vehicle.model || "").trim().toLowerCase();
    const imageUrl = localVehicleImages[localKey] || vehicle.imageUrl;
    if (!image || !imageUrl) {
      if (placeholder) {
        placeholder.hidden = false;
        placeholder.textContent = "IMAGE UNAVAILABLE";
      }
      return;
    }
    image.alt = [vehicle.brand, vehicle.model].filter(Boolean).join(" ") + " vehicle";
    image.addEventListener("load", function () {
      image.hidden = false;
      if (placeholder) placeholder.hidden = true;
    }, { once: true });
    image.addEventListener("error", function () {
      image.hidden = true;
      if (placeholder) {
        placeholder.hidden = false;
        placeholder.textContent = "IMAGE UNAVAILABLE";
      }
    }, { once: true });
    image.src = imageUrl;
    if (image.complete && image.naturalWidth > 0) {
      image.hidden = false;
      if (placeholder) placeholder.hidden = true;
    }
  }

  function showUnavailable(panel, message) {
    setText(panel, "[data-vehicle-name]", "Vehicle details unavailable");
    setText(panel, "[data-vehicle-category]", "SELECTED VEHICLE");
    setText(panel, "[data-vehicle-message]", message);
    const emptyState = panel.querySelector("[data-vehicle-empty]");
    if (emptyState) emptyState.hidden = false;
    const placeholder = panel.querySelector("[data-vehicle-placeholder]");
    if (placeholder) {
      placeholder.hidden = false;
      placeholder.textContent = "VEHICLE DETAILS UNAVAILABLE";
    }
    panel.dataset.loadState = "unavailable";
  }

  function setText(root, selector, value) {
    const element = root.querySelector(selector);
    if (element) element.textContent = value == null ? "" : String(value);
  }

  function formatLabel(value) {
    return value ? String(value).toLowerCase().replace(/_/g, " ").replace(/\b\w/g, function (letter) { return letter.toUpperCase(); }) : "";
  }

  function formatDate(value) {
    if (!value) return "—";
    const parts = value.split("-").map(Number);
    if (parts.length !== 3 || parts.some(function (part) { return !Number.isFinite(part); })) return value;
    return new Date(parts[0], parts[1] - 1, parts[2]).toLocaleDateString("en-LK", { day: "2-digit", month: "short", year: "numeric" }).toUpperCase();
  }

  function formatMoney(value) {
    return value.toLocaleString("en-LK", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }
})();
