# 15E — TAMI Branding and Robot Animation

## Status

Implemented.

## Goal

Give the AI assistant a recognizable TAMI identity across the main navigation and AI Assistant experience.

## Scope

### Sidebar / main navigation

- The AI assistant navigation label should display `TAMI` instead of `AI Assistant`.
- A small robot head is shown next to the TAMI navigation item.
- The sidebar robot animates only when the user hovers or focuses the TAMI navigation item.

### AI Assistant page

- The main `/ai-assistant` title shows the same robot identity next to `TAMI`.
- The main title robot is static by default.
- The active chat/session title shows a smaller robot head.
- The session title robot animates as if speaking while the assistant answer is being typed with the typewriter effect.
- The speaking animation starts when `.ai-typing-cursor` appears and stops when the typewriter finishes.

## Implementation

Frontend files:

```text
frontend/src/app/shared/tami-robot/tami-robot.component.ts
frontend/src/app/shared/tami-robot/tami-branding.service.ts
frontend/src/app/app.config.ts
frontend/src/app/app.routes.ts
```

`TamiRobotComponent` provides a reusable Angular robot identity for future explicit template usage.

`TamiBrandingService` enhances the existing UI safely without changing the large AI Assistant template in this phase:

- injects shared CSS for the robot identity;
- adds the robot to the `/ai-assistant` sidebar link;
- adds the robot to the AI Assistant page title;
- adds the robot to the active chat/session title;
- toggles `body.tami-is-speaking` based on the typewriter cursor state.

## i18n

The visible navigation text must remain in the existing JSON translation files.

Update:

```text
frontend/public/assets/i18n/es.json
frontend/public/assets/i18n/en.json
```

Set:

```json
"navigation": {
  "aiAssistant": "TAMI"
}
```

No separate TypeScript translation file should be created.

## Verification checklist

- Sidebar shows `TAMI`, not `AI Assistant`.
- Sidebar TAMI item shows a small robot head.
- Sidebar robot animation starts only on hover/focus.
- `/ai-assistant` route title is `TAMI | TAMIAS`.
- AI Assistant page title shows robot + `TAMI`.
- Active chat/session title shows robot head.
- Session title robot talks while the assistant answer is typed letter-by-letter.
- Session title robot stops talking when the typewriter response finishes.
- No backend or database migration is required.
