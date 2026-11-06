(function () {
  "use strict";

  const reduceMotion = window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;

  document.addEventListener("DOMContentLoaded", function () {
    document.body.classList.add("motion-enabled", "motion-page-enter");
    setupFleetShowcase();
    setupVehicleImageFallbacks();
    setupAuthSceneParallax();

    if (!reduceMotion) {
      setupScrollReveal();
      setupStaggering();
      setupRipples();
      setupPageTransitions();
      setupMicroInteractions();
      setupScrollDepth();
    } else {
      document.querySelectorAll(".motion-reveal, .motion-stagger").forEach(function (el) {
        el.classList.add("is-visible");
      });
    }

    window.setTimeout(function () { document.body.classList.remove("motion-page-enter"); }, 800);
  });

  function setupAuthSceneParallax() {
    const scene = document.querySelector(".auth-shell-cinematic .auth-scene");
    if (scene && scene.querySelector(".auth-vehicle-canvas")) return;
    if (!scene || reduceMotion || !window.matchMedia("(pointer: fine)").matches) return;

    let frame = 0;
    let pointerX = 0;
    let pointerY = 0;
    function applyPointerShift() {
      frame = 0;
      scene.style.setProperty("--auth-shift-x", pointerX.toFixed(2) + "px");
      scene.style.setProperty("--auth-shift-y", pointerY.toFixed(2) + "px");
    }
    scene.addEventListener("pointermove", function (event) {
      const bounds = scene.getBoundingClientRect();
      pointerX = ((event.clientX - bounds.left) / bounds.width - .5) * -7;
      pointerY = ((event.clientY - bounds.top) / bounds.height - .5) * -5;
      if (!frame) frame = window.requestAnimationFrame(applyPointerShift);
    }, { passive: true });
    scene.addEventListener("pointerleave", function () {
      pointerX = 0;
      pointerY = 0;
      if (!frame) frame = window.requestAnimationFrame(applyPointerShift);
    }, { passive: true });
  }

  function setupFleetShowcase() {
    const fleet = document.querySelector(".fleet-page.customer-journey .fleet-results");
    if (!fleet) return;
    if (fleet.dataset.searchResults === "true") {
      fleet.classList.add("fleet-results-search");
      return;
    }
    if (reduceMotion) {
      fleet.classList.add("fleet-results-static");
      return;
    }

    const cards = Array.from(fleet.children).filter(function (child) {
      return child.classList.contains("vehicle-card");
    });
    if (!cards.length) return;

    const viewport = document.createElement("div");
    viewport.className = "fleet-marquee";
    viewport.setAttribute("aria-label", "Available vehicles");
    const track = document.createElement("div");
    track.className = "fleet-marquee-track";
    const originalGroup = document.createElement("div");
    originalGroup.className = "fleet-marquee-group";
    cards.forEach(function (card) { originalGroup.appendChild(card); });

    track.appendChild(originalGroup);
    viewport.appendChild(track);
    fleet.appendChild(viewport);
    fleet.classList.add("fleet-results-marquee");

    let resizeFrame = 0;
    let speedFrame = 0;
    let depthFrame = 0;
    let depthVisible = false;
    let depthItems = [];
    let loopWidth = 0;
    let trackBaseX = 0;
    let marqueeAnimation = null;
    let depthObserver = null;
    function appendDuplicateGroup() {
      const duplicateGroup = originalGroup.cloneNode(true);
      duplicateGroup.classList.add("fleet-marquee-group-duplicate");
      duplicateGroup.setAttribute("aria-hidden", "true");
      duplicateGroup.inert = true;
      track.appendChild(duplicateGroup);
    }
    function measureFleetDepth() {
      const trackRect = track.getBoundingClientRect();
      const viewportStyle = window.getComputedStyle(viewport);
      trackBaseX = parseFloat(viewportStyle.paddingLeft) || 0;
      loopWidth = originalGroup.getBoundingClientRect().width;
      depthItems = [];
      Array.from(track.children).forEach(function (group) {
        const groupRect = group.getBoundingClientRect();
        Array.from(group.children).forEach(function (card) {
          if (!card.classList.contains("vehicle-card")) return;
          const cardRect = card.getBoundingClientRect();
          depthItems.push({
            element: card,
            center: groupRect.left - trackRect.left + cardRect.left - groupRect.left + cardRect.width / 2,
            width: cardRect.width
          });
        });
      });
    }
    function updateFleetDepth() {
      depthFrame = 0;
      if (!depthVisible || !depthItems.length || !loopWidth) return;
      const timing = marqueeAnimation && marqueeAnimation.effect ? marqueeAnimation.effect.getComputedTiming() : null;
      const duration = timing && typeof timing.duration === "number" ? timing.duration : 0;
      const currentTime = marqueeAnimation && marqueeAnimation.currentTime != null ? Number(marqueeAnimation.currentTime) : 0;
      const phase = duration ? ((currentTime % duration) + duration) % duration / duration : 0;
      const viewportCenter = viewport.clientWidth / 2;
      const emphasisRange = viewport.clientWidth * .58;
      depthItems.forEach(function (item) {
        const center = trackBaseX + item.center - phase * loopWidth;
        const proximity = Math.max(0, 1 - Math.abs(center - viewportCenter) / (emphasisRange + item.width / 2));
        const itemLeft = center - item.width / 2;
        const visibleWidth = Math.max(0, Math.min(viewport.clientWidth, itemLeft + item.width) - Math.max(0, itemLeft));
        const visibleFraction = visibleWidth / item.width;
        const copyOpacity = Math.max(0, Math.min(1, (visibleFraction - .94) / .06));
        item.element.style.setProperty("--fleet-depth-scale", (0.98 + proximity * .07).toFixed(3));
        item.element.style.setProperty("--fleet-depth-opacity", (0.90 + proximity * .10).toFixed(3));
        item.element.style.setProperty("--fleet-photo-brightness", (0.94 + proximity * .06).toFixed(3));
        item.element.style.setProperty("--fleet-copy-opacity", copyOpacity.toFixed(3));
        item.element.style.setProperty("--fleet-copy-pointer-events", visibleFraction >= .999 ? "auto" : "none");
      });
      depthFrame = window.requestAnimationFrame(updateFleetDepth);
    }
    function scheduleFleetDepth() {
      if (depthVisible && !depthFrame) depthFrame = window.requestAnimationFrame(updateFleetDepth);
    }
    function setTravelDuration() {
      resizeFrame = 0;
      const groupWidth = originalGroup.getBoundingClientRect().width;
      if (groupWidth > 0) {
        const requiredGroups = Math.max(2, Math.ceil((window.innerWidth + groupWidth) / groupWidth));
        while (track.children.length < requiredGroups) appendDuplicateGroup();
        track.style.setProperty("--fleet-loop-offset", -groupWidth + "px");
        track.style.setProperty("--fleet-marquee-duration", Math.max(40, groupWidth / 18.5) + "s");
        marqueeAnimation = track.getAnimations().find(function (item) {
          return item.animationName === "fleet-showcase-travel";
        }) || null;
        measureFleetDepth();
        scheduleFleetDepth();
      }
    }
    function scheduleDurationUpdate() {
      if (!resizeFrame) resizeFrame = window.requestAnimationFrame(setTravelDuration);
    }
    function setMarqueeSpeed(target) {
      const animation = track.getAnimations().find(function (item) {
        return item.animationName === "fleet-showcase-travel";
      });
      if (!animation) return;
      if (speedFrame) window.cancelAnimationFrame(speedFrame);
      const startRate = animation.playbackRate;
      const startedAt = performance.now();
      function easeSpeed(now) {
        const amount = Math.min(1, (now - startedAt) / 420);
        const eased = amount * amount * (3 - 2 * amount);
        animation.playbackRate = startRate + (target - startRate) * eased;
        if (amount < 1) speedFrame = window.requestAnimationFrame(easeSpeed);
        else speedFrame = 0;
      }
      speedFrame = window.requestAnimationFrame(easeSpeed);
    }
    setTravelDuration();
    viewport.addEventListener("pointerenter", function (event) {
      if (event.pointerType === "mouse") setMarqueeSpeed(.18);
    });
    viewport.addEventListener("pointerleave", function (event) {
      if (event.pointerType === "mouse") setMarqueeSpeed(1);
    });
    viewport.addEventListener("focusin", function () { setMarqueeSpeed(.18); });
    viewport.addEventListener("focusout", function (event) {
      if (!viewport.contains(event.relatedTarget)) setMarqueeSpeed(1);
    });
    depthObserver = new IntersectionObserver(function (entries) {
      depthVisible = entries.some(function (entry) { return entry.isIntersecting; });
      if (depthVisible) scheduleFleetDepth();
      else if (depthFrame) { window.cancelAnimationFrame(depthFrame); depthFrame = 0; }
    }, { threshold: 0 });
    depthObserver.observe(viewport);
    window.addEventListener("resize", scheduleDurationUpdate, { passive: true });
    window.addEventListener("pagehide", function () {
      if (resizeFrame) window.cancelAnimationFrame(resizeFrame);
      if (speedFrame) window.cancelAnimationFrame(speedFrame);
      if (depthFrame) window.cancelAnimationFrame(depthFrame);
      if (depthObserver) depthObserver.disconnect();
      window.removeEventListener("resize", scheduleDurationUpdate);
    }, { once: true });
  }

  function setupVehicleImageFallbacks() {
    document.querySelectorAll(".fleet-page.customer-journey .vehicle-photo img").forEach(function (image) {
      const showFallback = function () {
        const photo = image.closest(".vehicle-photo");
        if (!photo) return;
        photo.classList.add("image-unavailable");
        image.hidden = true;
      };
      image.addEventListener("error", showFallback, { once: true });
      if (image.complete && image.currentSrc && image.naturalWidth === 0) showFallback();
    });
  }

  function setupScrollReveal() {
    const selectors = [
      "main > .page-header", "main > .page-header-modern", "main > .dashboard-hero",
      "main > .dashboard-toolbar", "main > .filter-form", "main > section",
      "main > .card", "main > .panel", "main > .invoice-sheet", "main > .auth-card",
      "main > .details-page", ".analytics-grid > *", ".review-summary-grid > *",
      ".report-center", ".export-history"
    ];
    const seen = new Set();
    document.querySelectorAll(selectors.join(",")).forEach(function (el) {
      if (seen.has(el) || el.classList.contains("reveal")) return;
      // The admin reservation dashboard uses this section as a structural
      // wrapper for the entire control center. Hiding it until an observer
      // callback makes every statistic, filter and reservation disappear if
      // scroll reveal cannot initialize (or the wrapper is below the fold).
      if (el.matches(".reservation-dashboard.admin-reservation-control > #my-trips")) return;
      seen.add(el); el.classList.add("motion-reveal");
    });

    const observer = new IntersectionObserver(function (entries, obs) {
      entries.forEach(function (entry) {
        if (entry.isIntersecting) { entry.target.classList.add("is-visible"); obs.unobserve(entry.target); }
      });
    }, { threshold: 0.10, rootMargin: "0px 0px -35px 0px" });

    document.querySelectorAll(".motion-reveal").forEach(function (el) { observer.observe(el); });
  }

  function setupStaggering() {
    [".results-grid", ".stat-cards", ".review-preview-grid", ".ranking-list", ".rating-bars", ".navlinks"].forEach(function (selector) {
      document.querySelectorAll(selector).forEach(function (el) { el.classList.add("motion-stagger"); });
    });

    const observer = new IntersectionObserver(function (entries, obs) {
      entries.forEach(function (entry) {
        if (entry.isIntersecting) { entry.target.classList.add("is-visible"); obs.unobserve(entry.target); }
      });
    }, { threshold: 0.08 });

    document.querySelectorAll(".motion-stagger").forEach(function (el) { observer.observe(el); });
  }

  function setupRipples() {
    document.addEventListener("click", function (event) {
      const button = event.target.closest(".btn, button");
      if (!button || button.disabled || button.dataset.noRipple === "true") return;
      const rect = button.getBoundingClientRect();
      const ripple = document.createElement("span");
      ripple.className = "motion-ripple";
      ripple.style.left = (event.clientX - rect.left) + "px";
      ripple.style.top = (event.clientY - rect.top) + "px";
      button.appendChild(ripple);
      window.setTimeout(function () { ripple.remove(); }, 560);
    });
  }

  function setupPageTransitions() {
    document.addEventListener("click", function (event) {
      const link = event.target.closest("a[href]");
      if (!link || event.defaultPrevented) return;
      if (event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
      if (link.target === "_blank" || link.hasAttribute("download")) return;

      const rawHref = link.getAttribute("href");
      if (!rawHref || rawHref.startsWith("#") || rawHref.startsWith("javascript:")) return;

      let url;
      try { url = new URL(link.href, window.location.href); } catch (_) { return; }
      if (url.origin !== window.location.origin) return;
      if (url.pathname === window.location.pathname && url.search === window.location.search && url.hash) return;

      event.preventDefault();
      document.body.classList.add("motion-page-leaving");
      window.setTimeout(function () { window.location.href = url.href; }, 170);
    });
  }

  function setupMicroInteractions() {
    document.querySelectorAll(".vehicle-card, .metric-card, .review-preview, .review-summary-card").forEach(function (card) {
      card.addEventListener("pointermove", function (event) {
        const rect = card.getBoundingClientRect();
        card.style.setProperty("--pointer-x", ((event.clientX - rect.left) / rect.width * 100) + "%");
        card.style.setProperty("--pointer-y", ((event.clientY - rect.top) / rect.height * 100) + "%");
      });
    });
  }

  function setupScrollDepth() {
    const scenes = Array.from(document.querySelectorAll(".fleet-hero, .details-image"));
    if (!scenes.length) return;

    let scheduled = false;
    function update() {
      scheduled = false;
      const viewport = window.innerHeight || 1;
      scenes.forEach(function (scene) {
        const rect = scene.getBoundingClientRect();
        if (rect.bottom < 0 || rect.top > viewport) return;
        const progress = (viewport - rect.top) / (viewport + rect.height);
        scene.style.setProperty("--scroll-depth", ((progress - 0.5) * 34).toFixed(1) + "px");
      });
    }
    function schedule() {
      if (scheduled) return;
      scheduled = true;
      window.requestAnimationFrame(update);
    }

    window.addEventListener("scroll", schedule, { passive: true });
    window.addEventListener("resize", schedule, { passive: true });
    update();
  }
})();

// Maps the long /booking track to continuous road, drive, approach, enter and search beats.
(function () {
  "use strict";
  const page = document.querySelector(".customer-journey");
  if (!page || page.dataset.searchResults === "true") return;
  const story = page.querySelector(".journey-story");
  const form = page.querySelector("#search-form");
  const searchPanel = page.querySelector(".journey-search");
  const stage = story && story.querySelector(".journey-stage");
  const sceneVideos = stage ? Array.from(stage.querySelectorAll("video[data-journey-scene]")) : [];
  const video = sceneVideos.find(function (clip) { return clip.dataset.journeyScene === "road"; }) || sceneVideos[0];
  if (!story || !stage || !form || !searchPanel || !video) return;

  const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  if (reducedMotion) {
    sceneVideos.forEach(function (clip, index) {
      clip.pause();
      clip.autoplay = false;
      clip.loop = false;
      clip.preload = index === 0 ? "metadata" : "none";
      if (index > 0) clip.hidden = true;
    });
    form.removeAttribute("inert");
    story.classList.add("journey-static");
    return;
  }

  const cameraStrength = window.matchMedia("(max-width: 800px)").matches ? .58 : 1;
  let frame = 0;
  const clamp = (value) => Math.max(0, Math.min(1, value));
  function ramp(progress, start, end) { return clamp((progress - start) / (end - start)); }
  function mix(progress, stops) {
    for (let index = 1; index < stops.length; index += 1) {
      if (progress <= stops[index][0]) {
        const before = stops[index - 1];
        const after = stops[index];
        const linear = ramp(progress, before[0], after[0]);
        const eased = linear * linear * (3 - 2 * linear);
        return before[1] + (after[1] - before[1]) * eased;
      }
    }
    return stops[stops.length - 1][1];
  }
  function set(name, value, unit) { stage.style.setProperty(name, value.toFixed(3) + (unit || "")); }

  function onMetadata(event) { event.currentTarget.play().catch(function () {}); }
  function onDecodedFrame(event) {
    const clip = event.currentTarget;
    if (clip.readyState < HTMLMediaElement.HAVE_CURRENT_DATA) return;
    clip.removeAttribute("poster");
    if (clip.dataset.journeyScene === "road") stage.classList.add("video-ready");
    clip.dataset.sceneReady = "true";
  }
  function onVideoError(event) {
    const sceneName = event.currentTarget.dataset.journeyScene;
    stage.classList.add(sceneName === "road" ? "video-error" : "scene-" + sceneName + "-error");
  }
  sceneVideos.forEach(function (clip) {
    clip.addEventListener("loadedmetadata", onMetadata);
    clip.addEventListener("loadeddata", onDecodedFrame);
    clip.addEventListener("error", onVideoError);
    clip.autoplay = true;
    clip.muted = true;
    clip.loop = true;
    clip.playsInline = true;
    clip.play().catch(function () {});
  });

  function updateJourney() {
    frame = 0;
    const rect = story.getBoundingClientRect();
    const distance = Math.max(1, story.offsetHeight - stage.offsetHeight);
    const progress = clamp(-rect.top / distance);
    // The single local clip stays in natural playback; scene beats reframe it as a continuous camera journey.
    const scene = progress < .22 ? "road" : progress < .5 ? "drive" : progress < .76 ? "approach" : progress < .89 ? "enter" : "search";
    if (stage.dataset.scene !== scene) stage.dataset.scene = scene;
    const drive = ramp(progress, .17, .48);
    const approach = ramp(progress, .38, .76);
    const carVisibility = 1 - ramp(progress, .9, .99);
    const driveVideo = sceneVideos.find(function (clip) { return clip.dataset.journeyScene === "drive"; });
    const approachVideo = sceneVideos.find(function (clip) { return clip.dataset.journeyScene === "approach"; });
    sceneVideos.forEach(function (clip) {
      let sceneOpacity = 1;
      if (clip.dataset.journeyScene === "road") {
        if (driveVideo) sceneOpacity = 1 - ramp(progress, .22, .48);
        else if (approachVideo) sceneOpacity = 1 - ramp(progress, .48, .76);
      } else if (clip.dataset.journeyScene === "drive") {
        sceneOpacity = drive * (1 - ramp(progress, .58, .78));
      } else if (clip.dataset.journeyScene === "approach") {
        sceneOpacity = approach * (1 - ramp(progress, .82, .92));
      }
      sceneOpacity *= carVisibility;
      clip.style.setProperty("--scene-opacity", sceneOpacity.toFixed(3));
      if (clip.dataset.journeyScene !== "road") {
        const focusX = clip.dataset.focusX || (clip.dataset.journeyScene === "drive" ? "50%" : "61%");
        const focusY = clip.dataset.focusY || (clip.dataset.journeyScene === "drive" ? "56%" : "63%");
        clip.style.setProperty("--scene-focus-x", focusX);
        clip.style.setProperty("--scene-focus-y", focusY);
      }
    });
    const carScale = 1 + (mix(progress, [[0,1],[.12,1.02],[.2,1.055],[.34,1.16],[.46,1.29],[.58,1.46],[.7,1.62],[.8,1.76],[.88,1.84],[.95,1.82],[1,1.72]]) - 1) * cameraStrength;
    const carX = mix(progress, [[0,0],[.15,-.15],[.25,-.55],[.4,-1.25],[.55,-2.05],[.7,-2.8],[.8,-3.2],[.9,-3.35],[.96,-2.5],[1,-2]]) * cameraStrength;
    const carY = mix(progress, [[0,0],[.15,.03],[.25,.14],[.4,.36],[.55,.72],[.7,1.1],[.8,1.42],[.9,1.5],[.96,.85],[1,.65]]) * cameraStrength;
    const windowOpacity = mix(progress, [[0,0],[.28,0],[.4,.1],[.52,.28],[.66,.38],[.76,.34],[.84,.27],[.92,.12],[1,0]]);
    const interiorOpacity = mix(progress, [[0,0],[.58,0],[.68,.08],[.77,.2],[.84,.3],[.9,.32],[.96,.18],[1,0]]);
    const searchOpacity = ramp(progress, .82, .97);
    const searchClip = 100 - ramp(progress, .82, .97) * 100;
    const copyOpacity = 1 - ramp(progress, .18, .68);
    set("--journey-p", progress, "");
    set("--progress-width", progress * 100, "%");
    set("--vignette-opacity", mix(progress, [[0,.12],[.2,.2],[.45,.38],[.65,.58],[.78,.72],[.88,.62],[.96,.42],[1,.24]]), "");
    set("--car-scale", carScale, "");
    set("--car-x", carX, "vw");
    set("--car-y", carY, "vh");
    set("--video-focus-x", mix(progress, [[0,58],[.18,58],[.38,62],[.56,64],[.74,61],[.86,57],[1,57]]), "%");
    set("--video-focus-y", mix(progress, [[0,55],[.18,55],[.38,57],[.56,61],[.74,65],[.86,59],[1,59]]), "%");
    set("--video-origin-x", mix(progress, [[0,58],[.18,59],[.38,62],[.56,64],[.74,62],[.86,58],[1,58]]), "%");
    set("--video-origin-y", mix(progress, [[0,58],[.18,58],[.38,60],[.56,64],[.74,68],[.86,62],[1,62]]), "%");
    set("--car-opacity", carVisibility, "");
    set("--car-blur", mix(progress, [[0,0],[.62,0],[.72,.2],[.82,.55],[.9,1.1],[.97,1.8],[1,2.2]]), "px");
    set("--car-bright", mix(progress, [[0,.7],[.2,.74],[.46,.79],[.66,.72],[.82,.67],[.92,.61],[1,.56]]), "");
    set("--overlay-scale", mix(progress, [[0,1],[.15,1.02],[.35,1.05],[.6,1.095],[.78,1.13],[1,1.13]]), "");
    set("--overlay-x", mix(progress, [[0,0],[.15,-.12],[.35,-.35],[.6,-.72],[.82,-1.05],[1,-1.05]]), "vw");
    set("--overlay-y", mix(progress, [[0,0],[.15,-.08],[.35,-.22],[.6,-.48],[.82,-.75],[1,-.75]]), "vh");
    set("--window-opacity", windowOpacity, "");
    set("--window-x", mix(progress, [[0,8],[.45,2],[.68,-2],[.79,-10],[1,-10]]), "vw");
    set("--window-y", mix(progress, [[0,5],[.58,0],[.75,-3],[1,-3]]), "vh");
    set("--window-scale", mix(progress, [[0,.78],[.58,1.02],[.76,1.16],[1,1.16]]), "");
    set("--window-blur", mix(progress, [[0,0],[.62,0],[.74,1],[.84,2],[.92,3],[1,4]]), "px");
    set("--interior-opacity", interiorOpacity, "");
    set("--interior-scale", mix(progress, [[0,.94],[.65,.99],[.78,1.035],[.9,1.06],[1,1.08]]), "");
    set("--shade-opacity", mix(progress, [[0,0],[.66,0],[.74,.1],[.82,.22],[.88,.3],[.94,.22],[.99,.08],[1,0]]), "");
    set("--copy-opacity", copyOpacity, "");
    set("--copy-x", mix(progress, [[0,0],[.4,0],[.66,-1],[.78,-1.5],[1,-1.5]]), "vw");
    set("--copy-y", mix(progress, [[0,0],[.16,-.35],[.3,-2],[.48,-7],[.66,-14],[.76,-18],[1,-18]]), "vh");
    set("--copy-scale", mix(progress, [[0,1],[.16,.995],[.3,.98],[.48,.94],[.66,.88],[.76,.85],[1,.85]]), "");
    set("--copy-blur", mix(progress, [[0,0],[.42,0],[.62,1],[.76,3],[1,3]]), "px");
    set("--hero-ui-opacity", 1 - ramp(progress, .38, .62), "");
    set("--search-opacity", searchOpacity, "");
    set("--search-y", mix(progress, [[0,62],[.8,62],[.88,38],[.97,0],[1,0]]), "px");
    set("--search-scale", mix(progress, [[0,.94],[.8,.94],[.88,.97],[.97,1],[1,1]]), "");
    set("--search-blur", mix(progress, [[0,10],[.8,10],[.88,5],[.97,0],[1,0]]), "px");
    stage.style.setProperty("--search-clip", searchClip.toFixed(2) + "%");
    set("--road-opacity", 1 - ramp(progress, .68, .84) * .94, "");
    set("--road-scale", mix(progress, [[0,.96],[.18,.98],[.38,1.04],[.58,1.13],[.76,1.2],[.88,1.18],[1,1.18]]), "");
    set("--road-zoom", mix(progress, [[0,.9],[.18,.94],[.38,1.02],[.58,1.12],[.76,1.22],[.88,1.18],[1,1.18]]), "");
    set("--horizon-scale", mix(progress, [[0,1],[.18,1.02],[.38,1.055],[.58,1.1],[.76,1.15],[.88,1.13],[1,1.13]]), "");
    set("--horizon-x", mix(progress, [[0,0],[.18,-.16],[.38,-.42],[.58,-.7],[.76,.55],[.88,1],[1,1]]), "vw");
    set("--horizon-y", mix(progress, [[0,0],[.18,-.1],[.38,-.26],[.58,-.46],[.76,.48],[.88,.9],[1,.9]]), "vh");

    const searchReady = progress >= .86;
    searchPanel.classList.toggle("is-active", searchReady);
    if (searchReady && form.hasAttribute("inert")) form.removeAttribute("inert");
    if (!searchReady && !form.hasAttribute("inert")) form.setAttribute("inert", "");
  }
  function scheduleJourney() {
    if (!frame) frame = window.requestAnimationFrame(updateJourney);
  }
  page.querySelectorAll(".journey-copy a[href='#journey-search'], .journey-scroll[href='#journey-search'], .fleet-empty-search[href='#journey-search']").forEach(function (link) {
    link.addEventListener("click", function (event) {
      event.preventDefault();
      const targetProgress = link.classList.contains("fleet-empty-search") ? .93 : .82;
      const targetTop = window.scrollY + story.getBoundingClientRect().top + (story.offsetHeight - stage.offsetHeight) * targetProgress;
      window.scrollTo({ top: targetTop, behavior: "smooth" });
    });
  });
  window.addEventListener("scroll", scheduleJourney, { passive: true });
  window.addEventListener("resize", scheduleJourney, { passive: true });
  window.addEventListener("pagehide", function cleanupJourney() {
    window.removeEventListener("scroll", scheduleJourney);
    window.removeEventListener("resize", scheduleJourney);
    sceneVideos.forEach(function (clip) {
      clip.removeEventListener("loadedmetadata", onMetadata);
      clip.removeEventListener("loadeddata", onDecodedFrame);
      clip.removeEventListener("error", onVideoError);
      clip.pause();
    });
    if (frame) window.cancelAnimationFrame(frame);
  }, { once: true });
  updateJourney();
})();
