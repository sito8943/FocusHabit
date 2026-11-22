# Roadmap

## Next weekend (2026-11-28 / 29)

Order matters: feedback first, it can change what the rest should be.

### 1. Feedback from the first real user

She uses `FocusHabit-0.1.0.apk` (local-only) during the week.

- [ ] Short session: what she used, what confused her, what she missed. Write it down here.
- [ ] Check her real data looks right: streaks, "other days", quit habits, goals.
- [ ] Fix what blocks daily use before anything new. Release as 0.1.x.

### 2. Backend (sync with the web)

The engine exists (`core/sync`); the server does not.

- [ ] Run `docs/supabase-schema.sql` in Supabase (tables `habits`, `habit_logs`, RLS for anon).
- [ ] Add the local-only columns to the schema: `icon`, `kind`, `frequency`, `frequency_value`,
      `goal`, `start_date`. Same names as Room.
- [ ] Push edits: today only creates and soft-deletes sync; PATCH name/colour/icon/frequency/goal.
- [ ] Web (`habbit-tracker`): read/write the new columns, Font Awesome icon by name (same v7).
- [ ] First sync of existing phone data (upload, keep ids), then build with `-Psync=true`.
- [ ] Security note: the 4-char code is the only access control (anon key can read every row).
      Decide: keep, longer code, or Supabase Auth/RLS per code.

### 3. Reminders (local notifications)

No server needed.

- [ ] Per habit: optional reminder time (and days, following its frequency).
- [ ] `AlarmManager` exact/inexact alarm → notification with "Done" action (marks today).
- [ ] `POST_NOTIFICATIONS` permission (Android 13+) asked when enabling the first reminder.
- [ ] Re-schedule on boot (`BOOT_COMPLETED`) and after time/zone changes.
- [ ] Settings: global on/off, quiet hours.
- [ ] Room migration v2 → v3 for reminder fields (with its test).

### 4. Smaller items

- [ ] Room migration test (Robolectric `MigrationTestHelper`) for 1 → 2.
- [ ] Release flow like SitoWallet: version branch, CHANGELOG with hashes, `chore(release)`.
- [ ] Bebas Neue for big numbers (streak, %) if it looks better.
- [ ] Home-screen widget with today's habits (later).
