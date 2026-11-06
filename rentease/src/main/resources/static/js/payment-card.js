(function () {
  "use strict";

  document.addEventListener("DOMContentLoaded", function () {
    const cardNumber = document.querySelector("[data-field='cardNumber']");
    const expiry = document.querySelector("[data-field='expiry']");
    const cvv = document.querySelector("[data-field='cvv']");

    if (cardNumber) {
      cardNumber.addEventListener("input", function () {
        const digits = this.value.replace(/\D/g, "").slice(0, 16);
        this.value = digits.replace(/(\d{4})(?=\d)/g, "$1 ");
      });
    }
    if (expiry) {
      expiry.addEventListener("input", function () {
        const digits = this.value.replace(/\D/g, "").slice(0, 4);
        this.value = digits.length > 2 ? digits.slice(0, 2) + "/" + digits.slice(2) : digits;
      });
    }
    if (cvv) {
      cvv.addEventListener("input", function () {
        this.value = this.value.replace(/\D/g, "").slice(0, 3);
      });
    }
  });
})();
