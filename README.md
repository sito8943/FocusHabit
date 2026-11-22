# Focus Habit (Android)

Native Kotlin/Compose habit tracker, the Android side of the web app
[`sito8943/habbit-tracker`](https://github.com/sito8943/habbit-tracker).

## Features

- Habits with name, colour and a Font Awesome icon (≈1,400, searchable in English and Spanish).
- **Do** habits and **Quit** habits (days clean, relapses).
- Frequencies: every day, some weekdays, every N days, N per week, N per month.
- Optional goal: N times in total, or N days in a row without it.
- Today: day strip, collapsible progress (n/m, %, perfect-days streak, 4-row heatmap),
  "due today" vs "other days", celebration when the day is complete.
- Habit detail: 6-month grid, streak and best streak, weekday bars, goal progress, edit/delete.
- Calendar with month navigation; week starting Sunday or Monday.
- Settings: language (system/Español/English), theme (system/light/dark), recovery code.
- Offline-first: everything lives on the phone (Room).

**Sync is off.** The engine (Room + push/pull against Supabase, modelled on SitoWallet)
is in `core/sync`, but the backend tables do not exist yet — see
[`docs/supabase-schema.sql`](docs/supabase-schema.sql) and [`docs/ROADMAP.md`](docs/ROADMAP.md).

## Build

```bash
./gradlew :app:testDebugUnitTest   # unit tests
./gradlew :app:assembleDebug        # debug, installs as com.sito8943.focushabit.debug
./gradlew dist                      # production APK -> dist/FocusHabit-<version>.apk
```

`dist` builds the release variant: R8 minify + resource shrinking, trimmed META-INF,
no dependency-info blob, signed with the release key. `dist/` and `*.apk` are gitignored.

Local, never committed:

- `keystore.properties` + `focushabit-release.jks`: release signing. **Back them up**: an APK
  signed with another key cannot update an installed one (uninstall = local data lost).
- `local.properties`: `sdk.dir`, and for `-Psync=true` builds `supabase.url` / `supabase.anonKey`
  (the web's `VITE_SUPABASE_CO` / `VITE_SUPABASE_ANON`).

## Architecture

Single `:app` module, same conventions as SitoWallet (`../SitoWallet/ARCHITECTURE_RULES.md`):

| Package | Role |
|---|---|
| `core/model` | Habit, frequency, `HabitSchedule` (due days, streaks, goals) |
| `core/data` | Room (local source of truth, migrations), `HabitRepository` |
| `core/sync`, `core/network` | Supabase PostgREST client, push/pull engine, auto-sync coordinator |
| `core/icons` | Font Awesome 7 Free solid font + catalog (`tools/fontawesome`) |
| `core/settings` | Recovery code, app settings |
| `core/di` | `AppContainer`, the only construction root (no Hilt) |
| `feature/*` | Today, calendar, habit detail, habit form, settings, recovery |
| `ui/patterns`, `ui/theme` | Reusable UI, brand colours, Bebas Neue + Roboto |

## Differences from the web (on purpose)

- Dates use the phone's local day (the web uses UTC).
- Streaks stay alive until the day ends (the web resets to 0 every morning).
- Deleting a habit soft-deletes its logs first (the web leaves them alive).
- Icon, kind, frequency and goal are local-only until the backend has those columns.

## Credits

Icons: Font Awesome Free (CC BY 4.0). Fonts: Bebas Neue, Roboto (SIL OFL 1.1).
