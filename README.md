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

## The design

The screens follow the Figma file "ChemMe – App Redesign": a purple-on-cream palette, the Nunito
typeface, and eleven blob characters (Sera, Corty, Addy and the rest) that speak in the first person.

- **Onboarding** (first launch only): meet the characters, how the app works, and the two optional
  extras (location for daylight, the daily reminder).
- **Bottom navigation**: Home, History, Trends and Library, with a round Log button in the middle.
  Settings opens from the icon at the top of Home.
- **Log flow**: nine questions, one per screen. Numbers are changed with minus/plus and chips, so
  they cannot be out of range; the only thing left to validate by typing is the note.
- **Dark theme**: follows the phone's setting (`values-night/colors.xml`).

## How it is organised

| Package | What is in it |
|---|---|
| `ui` | The nine Activities, three RecyclerView adapters, and two small custom views (`FlowLayout` for wrapping chips, `BarChartView` for the sleep chart) |
| `logic` | ViewModels, `LogValidator`, `InsightEngine`, `LogChoices`, `TrendsCalculator`, `DaylightHelper`, `ReminderScheduler` |
| `data` | Room entities, DAOs, `ChemMeDatabase`, `ChemMeRepository`, `SeedImporter` |

The chemicals and the insight rules live in `app/src/main/assets/chemicals.csv` and `rules.csv`.
They are imported into Room on first launch. To add an explanation, add a line to `rules.csv`
and clear the app's storage (or reinstall) so the import runs again. The same applies after
updating from the earlier version of the app: its texts were written in the third person, and the
new first-person texts only appear after the import runs again.

Colours are in `res/values/colors.xml` (light) and `res/values-night/colors.xml` (dark), text sizes
and buttons in `res/values/styles.xml`, and the characters are vector drawables (`ic_sera.xml` ...).
The Nunito font files in `res/font` are under the SIL Open Font License (`app/src/main/NUNITO_LICENSE_OFL.txt`).

## Module techniques used

- **Room**: four related tables (`daily_log`, `chemical`, `insight_rule`, `log_insight`) with
  foreign keys, a unique index and a cascade delete. Home, History and Trends use JOIN and
  GROUP BY queries to show which characters belong to which day.
- **RecyclerView**: History, Insights and the Chemical Library grid with `ListAdapter` and `DiffUtil`.
- **Intents**: explicit Intents with extras between screens; an implicit Intent to open a source link.
- **Location**: `LocationManager` gives the latitude, and the app calculates daylight hours from it.
- **Coroutines and Flow**: all database work runs off the main thread; lists update by themselves.
- **Extras**: daily reminder notification, SharedPreferences settings, custom views, dark theme, unit tests.

## Test checklist

| Check | Steps | Expected |
|---|---|---|
| First launch | Install and open | Three intro pages; "Let's go" asks for the chosen permissions and opens Home |
| Valid input | Tap +, answer the nine questions with sleep 5 and caffeine 3 | Insights opens with "Entry saved" and cards for Corty and Addy |
| Required answer | On the mood question, do not pick a number | Next stays greyed out until a number is chosen |
| Invalid input | On the last question type a note longer than 300 characters and tap Save my day | Red outline, red counter and a red message; nothing saved, the text stays |
| Persistence | Save an entry, close the app from recents, reopen, open History | The entry is still listed with the same characters |
| Same date twice | Tap + again after saving | The questions open with today's answers for editing; History still has one row |
| Delete | Open an entry, tap Delete, confirm | The row disappears from History |
| No location | Deny the location permission | The entry still saves; the daylight card says "not available" |
| Location off | Settings > switch location off, then log | The outdoors question offers "Turn on in Settings" |
| Empty list | Settings > Delete all entries, then open History and Trends | "No entries yet" and "Nothing to chart yet", each with a Log today button |
| Dark theme | Switch the phone to dark mode | Every screen changes to the dark colours |

Unit tests for the validator, the insight engine, the log choices, the trends calculation and the
daylight calculation are in `app/src/test/java/com/example/chemme/LogicTest.kt`.

## Demo tip for location

On an emulator, open the three-dot menu > **Location**, choose a place and press **Set location**
before starting a log, so the phone has a position to report.
