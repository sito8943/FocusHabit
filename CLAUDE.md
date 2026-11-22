# Focus Habit (Android) — Agent Instructions

- Product spec = the web app `sito8943/habbit-tracker` (local clone:
  `~/Documents/HSCode/HS Ecole/React Web/habbit-tracker`). Intentional differences are listed in README.md.
- Conventions = `../SitoWallet/ARCHITECTURE_RULES.md` (Room as local truth, `AppContainer`, no Hilt,
  StateFlow ViewModels, minimal dependencies, explicit failures).
- Never change the Supabase contract on one side only: `core/sync/HabitApi.kt` must stay compatible
  with the web's `src/entities/*` queries and `docs/supabase-schema.sql`.
- Room schema changes need a `Migration`; never `fallbackToDestructiveMigration`.
- Never commit `local.properties`, `keystore.properties` or `*.jks`.
- Verify with `./gradlew :app:testDebugUnitTest :app:assembleDebug`.
