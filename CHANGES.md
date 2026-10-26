# What was wrong, and what changed

## The project could not be built
* `debug` builds were signed with `debug.keystore`, a file that is git-ignored and missing from the
  project, so Gradle failed. Debug builds now use the normal Android debug key.
* Release signing no longer breaks the build when no keystore/passwords are set.
* Removed dependencies that nothing used (Firebase, Retrofit/OkHttp, Secrets & Google-services plugins).
* Deleted the stray `SettingsScreen.kt.patch` file.

## Data-loss bugs (the serious ones)
Room's `REPLACE` insert was used to "edit" rows. Replacing a row deletes it first, which fires the
foreign-key cascades:
* starring a group or renaming it **deleted all its students**;
* editing a teacher **removed that teacher from every group**;
* changing one status in History **deleted the whole saved roster** of that session;
* editing a student moved them to the end of the list.
All edits now use `UPDATE` queries. Deleting a group now also deletes its sessions (and the
confirmation text says so - it used to claim students would only be "unassigned").

## Features that were broken or fake
* **Export All Data** produced an empty file (two copies of the launcher; the inner one read
  variables that were never set). Fixed.
* **Restore** lost student ID numbers, genders and favourite groups. Backups now keep them
  (old backups still import), and rows pointing at missing groups no longer abort a restore.
* **Reports** showed hard-coded numbers (30 present, 86 %) and a dummy CSV. Now everything is
  computed from saved sessions, with All time / 7 days / 30 days, per-group rates, a
  "Needs attention" list, and a real CSV (one row per student per session).
* **History order** sorted dates as text ("03 October" before "21 September"). Now chronological.
* **Clear All Data** did nothing. Now it works (settings are kept).
* **Settings** (theme, "everyone present", auto-save) were lost on every restart. Now saved.
  "Start with everyone Present" and "Auto-save drafts" now actually do something.
* **App Lock** was just a switch. It now asks for the phone's PIN/pattern/fingerprint.
* **Report chat link** was never used. The review dialog has "Copy & open report chat".
* Removed the **Language** selector - nothing was translated, so it did nothing.
* Importing a Telegram message created a duplicate group (`#GENERAL_7` vs `GENERAL 7`) and a
  teacher called "Not specified". Fixed; names are matched ignoring `#`/`_`/case.
* Demo data was re-inserted whenever the group list was empty; now only on first launch.

## Navigation / UI
* Every screen now has a top bar with a Back button and a Settings shortcut; content no longer
  draws under the status bar; status-bar icons follow the chosen theme; keyboard no longer covers fields.
* Attendance: pick the group right on the screen (it was stuck on "GENERAL 7"), subject/teacher come
  from the group, real date/time, legend, bigger tap targets, empty states, and it no longer saves
  an empty roll call with a fake "35 students".
* History: all four statuses, group filter, delete a session, safe status editing.
* Groups/Teachers/Students: duplicate-name checks, teacher auto-created when typed in a group,
  per-student attendance %, padding so the floating button doesn't hide the last row.
