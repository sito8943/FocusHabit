# Focus Habit (Android)

Native Kotlin/Compose port of the web habit tracker
[`sito8943/habbit-tracker`](https://github.com/sito8943/habbit-tracker).
Same Supabase backend and the same 4-character recovery code, so the web and the
phone can share habits.

## What it does

- Create habits (name + one of 6 colours), delete them (with confirmation).
- Mark any day as done; streak counted back from today.
- Calendar with month navigation: blue = selected, green = something done, orange = today.
- Recovery code: shows yours, or restores another one to load its habits.
- Offline-first: everything is saved on the phone (Room) and synced on its own
  when there is network (engine modelled on SitoWallet's).

## Setup

`local.properties` (not in Git):

```properties
sdk.dir=/Users/<you>/Library/Android/sdk
supabase.url=https://<project>.supabase.co      # VITE_SUPABASE_CO of the web
supabase.anonKey=<anon key>                     # VITE_SUPABASE_ANON of the web
```

Without them the app still works, only locally ("sync not configured" banner).

The backend tables are in [`docs/supabase-schema.sql`](docs/supabase-schema.sql).

## Build

```bash
./gradlew :app:testDebugUnitTest      # unit tests
./gradlew :app:assembleDebug          # debug, installs as com.sito8943.focushabit.debug
./gradlew :app:assembleRelease        # signed APK if keystore.properties exists
```

Release signing: `keystore.properties` + `focushabit-release.jks` at the root,
both gitignored. **Back them up**: an APK signed with another key cannot update
an installed one (uninstall = local data lost).

## Architecture

Single `:app` module, same conventions as SitoWallet / SitoChat:

| Package | Role |
|---|---|
| `core/data` | Room (local source of truth), `HabitRepository` |
| `core/network` | `SupabaseRest`: OkHttp against PostgREST with the anon key |
| `core/sync` | `HabitApi` (endpoints), `SyncEngine` (push + pull/merge), `SyncCoordinator` (auto sync, status) |
| `core/settings` | `SyncCodeStore` (recovery code) |
| `core/di` | `AppContainer`, the only construction root (no Hilt) |
| `feature/today`, `feature/calendar`, `feature/recovery` | UI + ViewModel per screen |

## Differences from the web (on purpose)

- Dates are the phone's **local** date. The web uses the UTC date, so near midnight
  "today" can differ between the two.
- Deleting a habit soft-deletes its logs first, then the habit (the web does it the
  other way round and its logs stay alive).
- Calendar has month navigation; Today has previous/next day arrows.
- Delete asks for confirmation. UI in English and Spanish.
