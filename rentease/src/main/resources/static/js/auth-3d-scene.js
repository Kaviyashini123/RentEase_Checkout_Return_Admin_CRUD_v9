import * as THREE from "three";
import { GLTFLoader } from "/js/vendor/GLTFLoader.js";

(() => {
  const stage = document.querySelector(".auth-shell-cinematic .auth-vehicle-stage");
  const shell = stage?.closest(".auth-scene");
  const canvas = stage?.querySelector("canvas");
  const status = stage?.querySelector(".auth-vehicle-status");
  console.info("[RentEase 3D] script loaded");
  if (!stage || !shell || !canvas) return;
  console.info("[RentEase 3D] container found", { stage: !!stage, shell: !!shell, canvas: !!canvas });
  console.info("[RentEase 3D] THREE loaded", THREE.REVISION);
  console.info("[RentEase 3D] scene initialized");

  const motionPreference = window.matchMedia("(prefers-reduced-motion: reduce)");
  const smallScreen = window.matchMedia("(max-width: 680px)");
  const finePointer = window.matchMedia("(pointer: fine)");
  const sceneProfile = stage.dataset.scene === "admin" ? {
    name: "admin", rotationSeconds: 33, azimuth: .72, elevation: .27,
    cameraFill: 1.12, horizontalComposition: .24, targetHeight: .29,
    mobileCompositionScale: .55,
    pointerAzimuth: .10, pointerElevation: .045, lightRate: .17
  } : {
    name: "customer", rotationSeconds: 27, azimuth: .68, elevation: .17,
    cameraFill: 1.03, horizontalComposition: .24, targetHeight: .24,
    mobileCompositionScale: 1,
    pointerAzimuth: .14, pointerElevation: .065, lightRate: .24
  };
  let renderer;
  let raf = 0;
  let disposed = false;
  let previousTime = 0;
  let modelRoot;
  let modelPivot;
  let keyLight;
  let carRadius = 5.4;
  const cameraTarget = new THREE.Vector3();
  const cameraLookTarget = new THREE.Vector3();
  let animationStartLogged = false;
  let pointerX = 0;
  let pointerY = 0;
  let targetX = 0;
  let targetY = 0;
  const scene = new THREE.Scene();
  const camera = new THREE.PerspectiveCamera(31, 1, 0.1, 100);
  const loader = new GLTFLoader();
  console.info("[RentEase 3D] GLTFLoader loaded", loader.constructor.name);
  let canvasStateLogged = false;

  function setStatus(message) {
    if (status) status.textContent = message;
  }

  function makeStudioEnvironment() {
    const envCanvas = document.createElement("canvas");
    envCanvas.width = 1024;
    envCanvas.height = 512;
    const context = envCanvas.getContext("2d");
    const gradient = context.createLinearGradient(0, 0, 0, 512);
    gradient.addColorStop(0, "#353a3b");
    gradient.addColorStop(.34, "#111516");
    gradient.addColorStop(.62, "#25251f");
    gradient.addColorStop(1, "#090b0b");
    context.fillStyle = gradient;
    context.fillRect(0, 0, 1024, 512);
    context.fillStyle = "rgba(246,239,220,.76)";
    context.fillRect(175, 46, 92, 12);
    context.fillRect(722, 42, 125, 9);
    context.fillStyle = "rgba(197,157,92,.48)";
    context.fillRect(400, 62, 42, 8);
    context.fillStyle = "rgba(206,220,226,.24)";
    context.fillRect(525, 44, 66, 11);
    const texture = new THREE.CanvasTexture(envCanvas);
    texture.mapping = THREE.EquirectangularReflectionMapping;
    const pmrem = new THREE.PMREMGenerator(renderer);
    const target = pmrem.fromEquirectangular(texture);
    scene.environment = target.texture;
    texture.dispose();
    pmrem.dispose();
    return target;
  }

  function fitVehicle(root) {
    const sourceBounds = new THREE.Box3().setFromObject(root);
    const sourceSize = sourceBounds.getSize(new THREE.Vector3());
    console.info("[RentEase 3D] source model bounds", {
      x: sourceSize.x.toFixed(2), y: sourceSize.y.toFixed(2), z: sourceSize.z.toFixed(2)
    });

    // This vehicle asset stores its long axis on Y and height on Z.
    // Convert it to Three.js's Y-up scene coordinates before framing.
    if (sourceSize.y > Math.max(sourceSize.x, sourceSize.z) * 1.25 && sourceSize.z < sourceSize.y * .55) {
      root.rotation.x = -Math.PI / 2;
      root.updateMatrixWorld(true);
    }

    const bounds = new THREE.Box3().setFromObject(root);
    const center = bounds.getCenter(new THREE.Vector3());
    root.position.x -= center.x;
    root.position.z -= center.z;
    root.position.y -= bounds.min.y;
    root.traverse((object) => {
      if (!object.isMesh) return;
      object.castShadow = true;
      object.receiveShadow = true;
      if (/(khronos|3d.?commerce|logo|emblem|license.?plate)/i.test(object.name)) object.visible = false;
      const materials = Array.isArray(object.material) ? object.material : [object.material];
      materials.forEach((material) => {
        if (!material || !material.isMeshStandardMaterial) return;
        if (/paint|body.?shell|exterior/i.test(material.name)) {
          material.color.set("#24282b");
          material.metalness = .82;
          material.roughness = .24;
          material.envMapIntensity = 1.5;
          if ("clearcoat" in material) material.clearcoat = .72;
        }
        material.needsUpdate = true;
      });
    });
    modelPivot.add(root);
    root.updateMatrixWorld(true);
    const scaledBounds = new THREE.Box3().setFromObject(root);
    const scaledSize = scaledBounds.getSize(new THREE.Vector3());
    const sphere = scaledBounds.getBoundingSphere(new THREE.Sphere());
    const vehicleCenter = scaledBounds.getCenter(new THREE.Vector3());
    const azimuth = sceneProfile.azimuth;
    const elevation = sceneProfile.elevation;
    const verticalFov = THREE.MathUtils.degToRad(camera.fov);
    const tanVertical = Math.tan(verticalFov / 2);
    const tanHorizontal = tanVertical * Math.max(camera.aspect, .1);
    cameraTarget.copy(vehicleCenter);
    const compositionOffset = sphere.radius * sceneProfile.horizontalComposition
      * (smallScreen.matches ? sceneProfile.mobileCompositionScale : 1);
    // Aim left of the vehicle and slightly lower so it sits in the open space
    // beside the copy and appears higher without lifting it off the floor.
    cameraTarget.x -= Math.cos(azimuth) * compositionOffset;
    cameraTarget.z += Math.sin(azimuth) * compositionOffset;
    cameraTarget.y = scaledBounds.min.y + scaledSize.y * sceneProfile.targetHeight;
    // Fit the actual oriented bounding-box corners. This keeps the full car in
    // frame while allowing a tighter, more prominent composition than fitting
    // its much larger enclosing sphere alone.
    const direction = new THREE.Vector3(Math.sin(azimuth) * Math.cos(elevation), Math.sin(elevation), Math.cos(azimuth) * Math.cos(elevation));
    const screenRight = new THREE.Vector3(Math.cos(azimuth), 0, -Math.sin(azimuth));
    const screenUp = new THREE.Vector3(-Math.sin(elevation) * Math.sin(azimuth), Math.cos(elevation), -Math.sin(elevation) * Math.cos(azimuth));
    let fitDistance = 0;
    for (const x of [scaledBounds.min.x, scaledBounds.max.x]) {
      for (const y of [scaledBounds.min.y, scaledBounds.max.y]) {
        for (const z of [scaledBounds.min.z, scaledBounds.max.z]) {
          const relative = new THREE.Vector3(x - cameraTarget.x, y - cameraTarget.y, z - cameraTarget.z);
          const alongView = relative.dot(direction);
          fitDistance = Math.max(
            fitDistance,
            alongView + Math.abs(relative.dot(screenRight)) / tanHorizontal,
            alongView + Math.abs(relative.dot(screenUp)) / tanVertical
          );
        }
      }
    }
    carRadius = fitDistance * sceneProfile.cameraFill * (smallScreen.matches ? 1.07 : 1);
    const horizontal = Math.cos(elevation) * carRadius;
    camera.position.set(
      cameraTarget.x + Math.sin(azimuth) * horizontal,
      cameraTarget.y + Math.sin(elevation) * carRadius,
      cameraTarget.z + Math.cos(azimuth) * horizontal
    );
    camera.near = Math.max(.01, carRadius - sphere.radius * 1.5);
    camera.far = carRadius + sphere.radius * 4;
    camera.updateProjectionMatrix();
    camera.lookAt(cameraTarget);
    console.info("[RentEase 3D] camera framed", {
      profile: sceneProfile.name,
      vehicle: [scaledSize.x.toFixed(2), scaledSize.y.toFixed(2), scaledSize.z.toFixed(2)],
      distance: carRadius.toFixed(2)
    });
  }

  function resize() {
    if (!renderer || disposed) return;
    const bounds = stage.getBoundingClientRect();
    if (!bounds.width || !bounds.height) return;
    const pixelRatio = Math.min(window.devicePixelRatio || 1, smallScreen.matches ? 1.15 : 1.5);
    renderer.setPixelRatio(pixelRatio);
    renderer.setSize(bounds.width, bounds.height, false);
    camera.aspect = bounds.width / bounds.height;
    camera.updateProjectionMatrix();
    if (!canvasStateLogged) {
      const style = window.getComputedStyle(canvas);
      console.info("[RentEase 3D] canvas attached", {
        connected: canvas.isConnected,
        backingWidth: canvas.width,
        backingHeight: canvas.height,
        clientWidth: canvas.clientWidth,
        clientHeight: canvas.clientHeight,
        display: style.display,
        visibility: style.visibility,
        opacity: style.opacity,
        zIndex: style.zIndex,
        position: style.position,
        stageRect: { width: Math.round(bounds.width), height: Math.round(bounds.height) }
      });
      canvasStateLogged = true;
    }
  }

  function renderFrame(now) {
    if (disposed) return;
    raf = 0;
    const elapsed = previousTime ? Math.min((now - previousTime) / 1000, .05) : 0;
    previousTime = now;
    const reduceMotion = motionPreference.matches;
    const lerp = 1 - Math.exp(-elapsed * 3.5);
    pointerX += (targetX - pointerX) * lerp;
    pointerY += (targetY - pointerY) * lerp;

    if (modelPivot && !reduceMotion) {
      modelPivot.rotation.y += elapsed * (Math.PI * 2 / sceneProfile.rotationSeconds);
      keyLight.position.x = Math.sin(now / 1000 * sceneProfile.lightRate) * 4.2;
      keyLight.position.z = 3.2 + Math.cos(now / 1000 * sceneProfile.lightRate) * 1.4;
    }
    const azimuth = sceneProfile.azimuth + (reduceMotion ? 0 : pointerX * sceneProfile.pointerAzimuth);
    const elevation = sceneProfile.elevation + (reduceMotion ? 0 : pointerY * sceneProfile.pointerElevation);
    const horizontal = Math.cos(elevation) * carRadius;
    const lookY = cameraTarget.y + (reduceMotion ? 0 : pointerY * .1);
    camera.position.x += (cameraTarget.x + Math.sin(azimuth) * horizontal - camera.position.x) * lerp;
    camera.position.z += (cameraTarget.z + Math.cos(azimuth) * horizontal - camera.position.z) * lerp;
    camera.position.y += (lookY + Math.sin(elevation) * carRadius - camera.position.y) * lerp;
    cameraLookTarget.set(cameraTarget.x, lookY, cameraTarget.z);
    camera.lookAt(cameraLookTarget);
    renderer.render(scene, camera);
    if (modelRoot && !renderFrame.hasLoggedVisibleFrame) {
      const rect = canvas.getBoundingClientRect();
      const style = window.getComputedStyle(canvas);
      const front = document.elementFromPoint(rect.left + rect.width * .68, rect.top + rect.height * .62);
      console.info("[RentEase 3D] animation running", {
        canvasSize: [canvas.width, canvas.height],
        display: style.display, visibility: style.visibility, opacity: style.opacity, zIndex: style.zIndex,
        centerFrontElement: front?.className || front?.tagName,
        cameraPosition: camera.position.toArray().map((value) => Number(value.toFixed(2))),
        cameraTarget: cameraLookTarget.toArray().map((value) => Number(value.toFixed(2)))
      });
      renderFrame.hasLoggedVisibleFrame = true;
    }
    if (!reduceMotion) raf = window.requestAnimationFrame(renderFrame);
  }

  function start() {
    if (raf || disposed || !renderer) return;
    previousTime = 0;
    if (!animationStartLogged) {
      console.info("[RentEase 3D] animation started");
      animationStartLogged = true;
    }
    raf = window.requestAnimationFrame(renderFrame);
  }

  function stop() {
    if (raf) window.cancelAnimationFrame(raf);
    raf = 0;
  }

  try {
    renderer = new THREE.WebGLRenderer({ canvas, alpha: true, antialias: !smallScreen.matches, powerPreference: "high-performance" });
    renderer.outputColorSpace = THREE.SRGBColorSpace;
    renderer.toneMapping = THREE.ACESFilmicToneMapping;
    renderer.toneMappingExposure = 1.05;
    renderer.shadowMap.enabled = true;
    renderer.shadowMap.type = THREE.PCFSoftShadowMap;
    scene.background = null;
    makeStudioEnvironment();
    console.info("[RentEase 3D] renderer initialized", {
      width: canvas.clientWidth, height: canvas.clientHeight,
      pixelRatio: renderer.getPixelRatio()
    });

    scene.add(new THREE.HemisphereLight(0xd8e1e0, 0x191615, 1.4));
    const key = new THREE.DirectionalLight(0xfff2dc, 3.1);
    key.position.set(-3.7, 5.3, 4.2);
    key.castShadow = true;
    key.shadow.mapSize.set(1024, 1024);
    key.shadow.camera.left = -7;
    key.shadow.camera.right = 7;
    key.shadow.camera.top = 6;
    key.shadow.camera.bottom = -4;
    key.shadow.bias = -.00025;
    scene.add(key);
    keyLight = new THREE.PointLight(0xd4ad72, 8, 14, 2);
    keyLight.position.set(3.5, 2.8, -3.3);
    scene.add(keyLight);
    const fill = new THREE.DirectionalLight(0xb7c7ce, .9);
    fill.position.set(4, 2.4, 5);
    scene.add(fill);
    const top = new THREE.DirectionalLight(0xffe9c3, 1.25);
    top.position.set(-1, 6, -1);
    scene.add(top);

    const ground = new THREE.Mesh(new THREE.PlaneGeometry(80, 80), new THREE.ShadowMaterial({ color: 0x050606, opacity: .42 }));
    ground.rotation.x = -Math.PI / 2;
    ground.position.y = -.015;
    ground.receiveShadow = true;
    scene.add(ground);
    modelPivot = new THREE.Group();
    modelPivot.position.set(.62, 0, .05);
    scene.add(modelPivot);

    resize();
    const observer = new ResizeObserver(resize);
    observer.observe(stage);
    const visibilityObserver = new IntersectionObserver((entries) => {
      if (entries[0].isIntersecting) start();
      else stop();
    }, { threshold: .01 });
    visibilityObserver.observe(shell);

    console.info("[RentEase 3D] loading GLB", stage.dataset.modelSrc);
    loader.load(stage.dataset.modelSrc, (gltf) => {
      if (disposed) return;
      modelRoot = gltf.scene;
      fitVehicle(modelRoot);
      stage.classList.add("is-model-ready");
      shell.classList.add("has-3d-vehicle");
      setStatus("Interactive vehicle · Move your pointer to explore");
      console.info("[RentEase 3D] GLB loaded and parsed");
      start();
    }, (event) => {
      if (event.lengthComputable) {
        console.info("[RentEase 3D] GLB download", `${Math.round(event.loaded / event.total * 100)}%`);
      }
    }, (error) => {
      console.error("[RentEase 3D] GLB load/parse failed", stage.dataset.modelSrc, error);
      stage.classList.add("is-model-fallback");
      setStatus("Vehicle preview unavailable · see console");
    });

    if (finePointer.matches) {
      shell.addEventListener("pointermove", (event) => {
        const rect = shell.getBoundingClientRect();
        targetX = THREE.MathUtils.clamp((event.clientX - rect.left) / rect.width - .5, -.5, .5);
        targetY = THREE.MathUtils.clamp((event.clientY - rect.top) / rect.height - .5, -.5, .5);
      }, { passive: true });
      shell.addEventListener("pointerleave", () => { targetX = 0; targetY = 0; }, { passive: true });
    }

    motionPreference.addEventListener?.("change", () => { if (!raf) start(); });
    window.addEventListener("resize", resize, { passive: true });
    window.addEventListener("pagehide", () => {
      disposed = true;
      stop();
      visibilityObserver.disconnect();
      observer.disconnect();
      renderer.dispose();
    }, { once: true });
    if (motionPreference.matches) setStatus("Still vehicle view · Reduced motion enabled");
  } catch (error) {
    console.error("[RentEase 3D] renderer/scene initialization failed", error);
    stage.classList.add("is-model-fallback");
    setStatus("Vehicle preview unavailable · see console");
  }
})();
