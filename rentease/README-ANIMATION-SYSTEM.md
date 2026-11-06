# RentEase Professional Animation System

This update adds a reusable, framework-free motion layer to the existing Spring Boot + Thymeleaf frontend. No backend or database changes were made.

## Included

- Smooth page entrance and short page-exit transitions for internal navigation
- Scroll-triggered reveal animations
- Staggered entrance for vehicle cards, KPI cards, reviews, rankings and rating bars
- Premium button hover sheen and click ripple feedback
- Subtle card hover depth and pointer-following highlight
- Vehicle image zoom on hover
- Cinematic customer and staff login scenes with a slow-moving car image, headlight bloom, and road-light trails
- Scroll-depth parallax for fleet and vehicle imagery
- Animated navbar entrance and active navigation underline
- Input focus lift/glow
- Animated toast/alert entrances
- Subtle ambient motion with restrained automotive lighting
- Existing report count-up/progress animations remain intact
- Reduced-motion accessibility support via `prefers-reduced-motion`

The login forms keep their existing field names and server routes. The photographic login backdrop reuses a vehicle image already used by the supplied seed data.

## Files

- `src/main/resources/static/css/animation-system.css` — reusable motion styles
- `src/main/resources/static/js/animation-system.js` — reveal, transition, ripple and micro-interaction logic
- `src/main/resources/templates/fragments/layout.html` — loads the motion system globally

## Run

Open the project in IntelliJ and run the existing `RenteaseSystemApplication`. The animation system does not require an additional Maven dependency or frontend framework.

The SQL Server login problem shown previously (`Login failed for user 'sa'`) is separate from this frontend update and was intentionally not changed.
