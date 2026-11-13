# ChemMe

An Android app for the TU Dublin Mobile Software Development module (CMPU3026).
You log your day (mood, sleep, caffeine, movement, daylight, food) and ChemMe explains which
body chemicals those habits are linked to, with a source for every explanation.

ChemMe is educational. It does not measure chemical levels and does not give medical advice.

## Open and run

1. In Android Studio choose **File > Open** and select this folder.
2. Wait for Gradle sync. Install **Android SDK Platform 35** if Android Studio asks.
3. Pick an emulator or phone running Android 8.0 (API 26) or later and press **Run**.

The Gradle, Kotlin, KSP and Room versions are the same as the module's Pocket Catalog project.

## How it is organised

| Package | What is in it |
|---|---|
| `ui` | The seven Activities and three RecyclerView adapters |
| `logic` | ViewModels, `LogValidator`, `InsightEngine`, `DaylightHelper`, `ReminderScheduler` |
| `data` | Room entities, DAOs, `ChemMeDatabase`, `ChemMeRepository`, `SeedImporter` |

The chemicals and the insight rules live in `app/src/main/assets/chemicals.csv` and `rules.csv`.
They are imported into Room on first launch. To add an explanation, add a line to `rules.csv`
and clear the app's storage (or reinstall) so the import runs again.

## Module techniques used

- **Room**: four related tables (`daily_log`, `chemical`, `insight_rule`, `log_insight`) with
  foreign keys, a unique index and a cascade delete.
- **RecyclerView**: History, Insights and Chemical Library lists with `ListAdapter` and `DiffUtil`.
- **Intents**: explicit Intents with extras between screens; an implicit Intent to open a source link.
- **Location**: `LocationManager` gives the latitude, and the app calculates daylight hours from it.
- **Coroutines and Flow**: all database work runs off the main thread; lists update by themselves.
- **Extras**: daily reminder notification, SharedPreferences settings, unit tests.

## Test checklist

| Check | Steps | Expected |
|---|---|---|
| Valid input | Log today with sleep 5, caffeine 3 | Insights opens with "Entry saved" and cards for cortisol and adenosine |
| Invalid input | Enter sleep 30, or leave a number box empty, and tap Save | Red message under the field, nothing saved, typed values stay |
| Persistence | Save an entry, close the app from recents, reopen, open History | The entry is still listed with the same insights |
| Same date twice | Tap Log today again after saving | The form opens with today's values for editing; History still has one row |
| Delete | Open an entry, tap Delete, confirm | The row disappears from History |
| No location | Deny the location permission | The entry still saves; daylight shows "not available" |
| Empty list | Settings > Delete all entries, then open History | "No entries yet" with the Log button |

Unit tests for the validator, the insight engine and the daylight calculation are in
`app/src/test/java/com/example/chemme/LogicTest.kt`.

## Demo tip for location

On an emulator, open the three-dot menu > **Location**, choose a place and press **Set location**
before opening Log Entry, so the phone has a position to report.
