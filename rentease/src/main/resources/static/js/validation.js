/* ==========================================================================
   RentEase — Client-side form validation
   Generic helpers + per-form rule sets. Attached on DOMContentLoaded.
   NOTE: this is a usability layer only — the server (controllers/services)
   still re-validates and is the source of truth.
   ========================================================================== */

(function () {
  "use strict";

  /* ---------------------- generic helpers ---------------------- */

  function fieldWrap(input) {
    return input.closest("label") || input.parentElement;
  }

  function showError(input, message) {
    input.classList.add("input-invalid");
    let wrap = fieldWrap(input);
    let err = wrap.querySelector(".field-error");
    if (!err) {
      err = document.createElement("span");
      err.className = "field-error";
      wrap.appendChild(err);
    }
    err.textContent = message;
  }

  function clearError(input) {
    input.classList.remove("input-invalid");
    const wrap = fieldWrap(input);
    const err = wrap.querySelector(".field-error");
    if (err) err.remove();
  }

  function clearAllErrors(form) {
    form.querySelectorAll(".input-invalid").forEach((el) => el.classList.remove("input-invalid"));
    form.querySelectorAll(".field-error").forEach((el) => el.remove());
  }

  function isEmpty(value) {
    return value === null || value === undefined || String(value).trim() === "";
  }

  function byName(form, name) {
    return form.elements[name];
  }

  function todayISO() {
    const d = new Date();
    return d.toISOString().slice(0, 10);
  }

  /** Runs a list of [input, testFn, message] rules; returns true if all pass. */
  function runRules(form, rules) {
    clearAllErrors(form);
    let valid = true;
    rules.forEach(([input, test, message]) => {
      if (!input) return;
      let ok;
      try { ok = test(); } catch (e) { ok = false; }
      if (!ok) {
        showError(input, message);
        valid = false;
      }
    });
    return valid;
  }

  /** Attaches validation to every element matching `selector` (handles both
   *  a single #id form and repeated forms sharing a class, e.g. one
   *  cancel-reason form per table row). */
  function guardAll(selector, buildRules) {
    document.querySelectorAll(selector).forEach((form) => {
      form.addEventListener("submit", function (e) {
        const rules = buildRules(form);
        if (!runRules(form, rules)) {
          e.preventDefault();
          const firstInvalid = form.querySelector(".input-invalid");
          if (firstInvalid) firstInvalid.focus();
        }
      });
      form.addEventListener("input", function (e) {
        if (e.target && e.target.classList.contains("input-invalid")) {
          clearError(e.target);
        }
      });
      form.addEventListener("change", function (e) {
        if (e.target && e.target.classList.contains("input-invalid")) {
          clearError(e.target);
        }
      });
    });
  }

  function guard(formId, buildRules) {
    guardAll("#" + formId, buildRules);
  }

  document.addEventListener("DOMContentLoaded", function () {

    /* ---------------- 1. Vehicle Search & Browsing ---------------- */
    guard("search-form", (form) => {
      const min = byName(form, "minPrice");
      const max = byName(form, "maxPrice");
      return [
        [min, () => isEmpty(min.value) || Number(min.value) >= 0, "Minimum price cannot be negative."],
        [max, () => isEmpty(max.value) || Number(max.value) >= 0, "Maximum price cannot be negative."],
        [max, () => isEmpty(min.value) || isEmpty(max.value) || Number(max.value) >= Number(min.value),
          "Maximum price must be greater than or equal to minimum price."],
      ];
    });

    /* ---------------- 2. Booking & Reservation Management ---------------- */
    guard("booking-form", (form) => {
      const customerId = byName(form, "customerId");
      const vehicleId = byName(form, "vehicleId");
      const startDate = byName(form, "startDate");
      const endDate = byName(form, "endDate");
      return [
        [customerId, () => !isEmpty(customerId.value) && Number(customerId.value) > 0, "Enter a valid customer ID."],
        [vehicleId, () => !isEmpty(vehicleId.value) && Number(vehicleId.value) > 0, "Enter a valid vehicle ID."],
        [startDate, () => !isEmpty(startDate.value), "Start date is required."],
        [endDate, () => !isEmpty(endDate.value), "End date is required."],
        [startDate, () => isEmpty(startDate.value) || startDate.value >= todayISO(),
          "Start date cannot be in the past."],
        [endDate, () => isEmpty(startDate.value) || isEmpty(endDate.value) || endDate.value > startDate.value,
          "End date must be after the start date."],
      ];
    });

    guardAll(".cancel-reason-form", (form) => {
      const reason = byName(form, "reason");
      return [
        [reason, () => !isEmpty(reason.value) && reason.value.trim().length >= 5,
          "Please provide a reason (at least 5 characters) for the cancellation."],
      ];
    });

    /* ---------------- 3. Payment & Invoicing ---------------- */
    guard("payment-form", (form) => {
      const method = byName(form, "method");
      const cardholder = form.querySelector("[data-field='cardholder']");
      const cardNumber = form.querySelector("[data-field='cardNumber']");
      const expiry = form.querySelector("[data-field='expiry']");
      const cvv = form.querySelector("[data-field='cvv']");
      const needsCard = () => method && (method.value === "CREDIT_CARD" || method.value === "DEBIT_CARD");
      const luhnValid = () => {
        const digits = (cardNumber.value || "").replace(/\D/g, "");
        if (!/^\d{16}$/.test(digits)) return false;
        let sum = 0;
        let doubleDigit = false;
        for (let i = digits.length - 1; i >= 0; i--) {
          let digit = Number(digits[i]);
          if (doubleDigit) { digit *= 2; if (digit > 9) digit -= 9; }
          sum += digit;
          doubleDigit = !doubleDigit;
        }
        return sum % 10 === 0;
      };

      const expiryValid = () => {
        if (isEmpty(expiry.value)) return false;
        const m = expiry.value.match(/^(\d{2})\/(\d{2})$/);
        if (!m) return false;
        const month = parseInt(m[1], 10);
        const year = 2000 + parseInt(m[2], 10);
        if (month < 1 || month > 12) return false;
        const now = new Date();
        const expiryDate = new Date(year, month, 0);
        return expiryDate >= new Date(now.getFullYear(), now.getMonth(), 1);
      };

      return [
        [method, () => !isEmpty(method.value), "Please select a payment method."],
        [cardholder, () => !needsCard() || !isEmpty(cardholder.value), "Enter the cardholder name."],
        [cardNumber, () => !needsCard() || luhnValid(), "Enter a valid card number."],
        [expiry, () => !needsCard() || expiryValid(), "Enter a valid, non-expired expiry date (MM/YY)."],
        [cvv, () => !needsCard() || /^\d{3}$/.test(cvv.value.trim()), "CVV must be exactly 3 digits."],
      ];
    });

    /* ---------------- 4. Booking & Vehicle Management ---------------- */
    guard("add-vehicle-form", (form) => {
      const brand = byName(form, "brand");
      const model = byName(form, "model");
      const plateNumber = byName(form, "plateNumber");
      const acquisitionDate = byName(form, "acquisitionDate");
      const purchaseValue = byName(form, "purchaseValue");
      const odometerReading = byName(form, "odometerReading");
      const nextServiceDue = byName(form, "nextServiceDue");

      return [
        [brand, () => !isEmpty(brand.value), "Brand is required."],
        [model, () => !isEmpty(model.value), "Model is required."],
        [plateNumber, () => !isEmpty(plateNumber.value) && /^[A-Za-z0-9\- ]{4,12}$/.test(plateNumber.value.trim()),
          "Enter a valid plate number (letters, numbers, spaces or hyphens, 4-12 characters)."],
        [acquisitionDate, () => isEmpty(acquisitionDate.value) || acquisitionDate.value <= todayISO(),
          "Acquisition date cannot be in the future."],
        [purchaseValue, () => isEmpty(purchaseValue.value) || Number(purchaseValue.value) >= 0,
          "Purchase value cannot be negative."],
        [odometerReading, () => isEmpty(odometerReading.value) || Number(odometerReading.value) >= 0,
          "Odometer reading cannot be negative."],
        [nextServiceDue, () =>
          isEmpty(nextServiceDue.value) || isEmpty(acquisitionDate.value) ||
          nextServiceDue.value >= acquisitionDate.value,
          "Next service due date must be on or after the acquisition date."],
      ];
    });

    /* ---------------- 5. Rental Checkout & Return Processing ---------------- */
    guard("handover-form", (form) => {
      const reservationId = byName(form, "reservationId");
      const vehicleId = byName(form, "vehicleId");
      const odometer = byName(form, "odometer");
      const staffId = byName(form, "staffId");
      const expectedReturn = byName(form, "expectedReturn");

      return [
        [reservationId, () => !isEmpty(reservationId.value) && Number(reservationId.value) > 0,
          "Enter a valid reservation ID."],
        [vehicleId, () => !isEmpty(vehicleId.value) && Number(vehicleId.value) > 0,
          "Enter a valid vehicle ID."],
        [odometer, () => !isEmpty(odometer.value) && Number(odometer.value) >= 0,
          "Odometer reading must be zero or a positive number."],
        [staffId, () => !isEmpty(staffId.value), "Staff ID is required."],
        [expectedReturn, () => !isEmpty(expectedReturn.value) && new Date(expectedReturn.value) > new Date(),
          "Expected return date/time must be in the future."],
      ];
    });

    guard("return-form", (form) => {
      const odometer = byName(form, "odometer");
      const staffId = byName(form, "staffId");
      const damageDescription = byName(form, "damageDescription");
      const damageSeverity = byName(form, "damageSeverity");

      return [
        [odometer, () => !isEmpty(odometer.value) && Number(odometer.value) >= 0,
          "Odometer reading must be zero or a positive number."],
        [staffId, () => !isEmpty(staffId.value), "Staff ID is required."],
        [damageDescription, () => isEmpty(damageSeverity.value) || !isEmpty(damageDescription.value),
          "Please describe the damage if a severity is selected."],
        [damageSeverity, () => isEmpty(damageDescription.value) || !isEmpty(damageSeverity.value),
          "Please select a severity for the reported damage."],
      ];
    });

    /* ---------------- 6. Reports & Reviews ---------------- */
    guard("review-form", (form) => {
      const customerId = byName(form, "customerId");
      const rating = byName(form, "rating");
      const comment = byName(form, "comment");

      return [
        [customerId, () => !customerId || (!isEmpty(customerId.value) && Number(customerId.value) > 0),
          "Enter a valid customer ID."],
        [rating, () => !isEmpty(rating.value) && Number(rating.value) >= 1 && Number(rating.value) <= 5,
          "Rating must be between 1 and 5."],
        [comment, () => isEmpty(comment.value) || comment.value.length <= 1000,
          "Comment must be 1000 characters or fewer."],
      ];
    });

    /* ---------------- 7. Authentication ---------------- */
    const EMAIL_RE = /^[\w.+-]+@[\w-]+\.[\w.-]+$/;

    function loginRules(form) {
      const email = byName(form, "email");
      const password = byName(form, "password");
      return [
        [email, () => EMAIL_RE.test((email.value || "").trim()), "Enter a valid email address."],
        [password, () => !isEmpty(password.value), "Password is required."],
      ];
    }

    guard("user-login-form", loginRules);
    guard("admin-login-form", loginRules);

    guard("register-form", (form) => {
      const fullName = byName(form, "fullName");
      const email = byName(form, "email");
      const phone = byName(form, "phone");
      const password = byName(form, "password");
      const confirm = byName(form, "confirmPassword");

      return [
        [fullName, () => !isEmpty(fullName.value), "Full name is required."],
        [email, () => EMAIL_RE.test((email.value || "").trim()), "Enter a valid email address."],
        [phone, () => isEmpty(phone.value) || /^[\d+\-\s]{9,15}$/.test(phone.value.trim()),
          "Enter a valid phone number."],
        [password, () => (password.value || "").length >= 6,
          "Password must be at least 6 characters."],
        [confirm, () => confirm.value === password.value, "Passwords do not match."],
      ];
    });

    function exportRules(form) {
      const periodLabel = byName(form, "periodLabel");
      const staffId = byName(form, "staffId");
      return [
        [periodLabel, () => /^\d{4}-\d{2}$/.test((periodLabel.value || "").trim()),
          "Enter the period as YYYY-MM, e.g. 2026-09."],
        [staffId, () => !isEmpty(staffId.value), "Staff ID is required."],
      ];
    }

    guard("export-form", exportRules);
    guard("review-export-form", exportRules);

  });
})();
