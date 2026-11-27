# ChemMe

**Student:** Yana Chulovska (C24490782) · **Module:** Mobile Software Development CMPU3026, TU Dublin · **Repo:** https://github.com/Y-A-N-C-H-A-2/ChemMe

ChemMe is an Android app for young adults (18–30) who want to understand how their daily habits
relate to their body chemistry. You log your day (mood, energy, anxiety, sleep, caffeine, movement,
time outdoors, meals, supplements) and ChemMe shows which body chemicals those habits are linked to,
each explained by a character (Sera for serotonin, Corty for cortisol, Addy for adenosine...) and
backed by a source link.

ChemMe is educational. It does not measure chemical levels and does not give medical advice.

---

## 1. Requirements

| Item | Version |
|---|---|
| Android Studio | A 2025 release or newer that supports Android Gradle Plugin 8.13 (older versions offer to downgrade AGP: say no and update Android Studio instead) |
| JDK | 17 (the one bundled with Android Studio is fine) |
| Android SDK | Platform 35 (compileSdk 35, targetSdk 34) |
| Device / emulator | Android 8.0 (API 26) or later. Tested on: Pixel 9 |
| Internet | Only needed for the first Gradle sync (downloads Gradle 9.1, Kotlin 2.3, KSP, Room 2.8) |

No API keys, accounts or backend are needed. All data is stored on the device with Room.

## 2. Open, build and run

1. Unzip the submission and, in Android Studio, choose **File > Open** and select the `ChemMe` folder
   (the one that contains `settings.gradle.kts`).
2. Wait for **Gradle sync** to finish. If Android Studio asks to install **Android SDK Platform 35**
   or build tools, accept.
3. Start an emulator (Device Manager > a phone with API 26+) or connect a phone with USB debugging on.
4. Select the **app** run configuration and press **Run ▶**.
5. On first launch the onboarding screens appear. Tap **Let's go** and allow (or deny) location and
   notifications. The app works either way.

**Location on an emulator:** open the emulator's three-dot menu > **Location**, pick a place and press
**Set location** before logging a day, so the daylight-hours feature has a position to use.

**Run the unit tests:** right-click `app/src/test/java/com/example/chemme/LogicTest.kt` > **Run 'LogicTest'**,
or run `./gradlew test` from the project folder.

**Troubleshooting**
- *"SDK location not found"*: open **File > Project Structure > SDK Location** and pick your SDK;
  Android Studio then writes `local.properties` (it is not included in the ZIP on purpose).
- *Old texts after updating*: the chemicals and rules are imported from `assets/*.csv` on first launch.
  Clear the app's storage or reinstall so the import runs again.

## 3. What the app does (core workflow)

1. **Home** shows whether today is logged and which characters showed up this week.
2. **Log (+ button)**: nine short questions, one per screen. Numbers are chosen with minus/plus and
   chips, so they cannot go out of range. The optional note is validated (max 300 characters).
3. **Save** validates the entry, stores it in Room, runs the insight rules and opens **Insights**,
   which shows the matching characters, each with an explanation and source.
4. **History** lists every saved day (RecyclerView). Tapping a day opens its Insights, where the
   entry can be **edited** or **deleted** (with a confirmation dialog).
5. **Trends** shows a sleep bar chart for the last 7 or 30 days, a sleep/mood pattern, and how often
   each character appeared.
6. **Library** lists all eleven chemicals in a grid; the detail screen opens the source in the browser.
7. **Settings**: daily reminder time, location on/off, delete all entries.

## 4. How the brief's requirements are met

| Requirement | Where it is in ChemMe |
|---|---|
| Room, at least two related entities with keys | Four tables: `daily_log`, `chemical`, `insight_rule` (FK → chemical, one-to-many), `log_insight` (junction table, composite PK, FKs to `daily_log` with CASCADE and to `insight_rule`). Unique index on `daily_log.logDate`. See `data/` |
| CRUD on meaningful data | Create/update: `LogViewModel.saveLog()`; read: `DailyLogDao.getAll()` (Flow) and the JOIN queries in `InsightDao`; delete: `InsightsActivity.confirmDelete()` and Settings > Delete all |
| List screen from Room | `HistoryActivity` (RecyclerView + `ListAdapter`/`DiffUtil`), also Library and Insights |
| Input screen with validation | `LogEntryActivity` + `LogValidator` (ranges for every field, note ≤ 300 chars, nothing saved when invalid) |
| Another screen for the core task | `InsightsActivity` (results of the rules for that day), `TrendsActivity` |
| Data correct after navigation / restart | Everything is read from Room through Flow; `LogViewModel` survives rotation; demonstrated in the video by closing the app from recents and reopening |
| ActivityResultLauncher between activities | `InsightsActivity.editEntry` launches `LogEntryActivity` with `StartActivityForResult`; `LogEntryActivity` returns `RESULT_OK` and Insights shows "Entry updated". Permission requests also use `registerForActivityResult` |
| Two Android features serving the user need | **Location** (`DaylightHelper`): reads the latitude with `LocationManager` and calculates the day's daylight hours, which feed the vitamin D / melatonin insights. **Notifications + AlarmManager** (`ReminderScheduler`): a daily reminder at the time chosen in Settings, so the user keeps logging |
| Extras | Coroutines + Flow, custom views (`BarChartView`, `FlowLayout`), SharedPreferences settings, dark theme, onboarding, implicit Intent for source links, CSV seed import, JVM unit tests |

## 5. Project structure

| Package / folder | Contents |
|---|---|
| `ui` | The nine Activities, the RecyclerView adapters (`Adapters.kt`), custom views `FlowLayout` and `BarChartView`, shared helpers (`UiHelpers.kt`) |
| `logic` | `LogViewModel`, `LibraryViewModel`, `LogValidator`, `InsightEngine`, `LogChoices` (also holds `TrendsCalculator`), `DaylightHelper`, `ReminderScheduler`, `AppPrefs` |
| `data` | Room entities, DAOs, `ChemMeDatabase`, `ChemMeRepository`, `SeedImporter` |
| `assets` | `chemicals.csv` and `rules.csv`, imported into Room on first launch |
| `res` | Layouts (View Binding), colours (light + `values-night`), styles, character vector drawables, Nunito font |
| `video/` | `ChemMe_walkthrough.mp4`, the 2–3 minute walkthrough (in the submission ZIP only) |

## 6. Test checklist (manual)

| Check | Steps | Expected |
|---|---|---|
| First launch | Install and open | Three intro pages; "Let's go" asks for the chosen permissions and opens Home |
| Valid input | Tap +, answer the nine questions with sleep 5 and caffeine 3 | Insights opens with "Entry saved" and cards for Corty and Addy |
| Required answer | On the mood question, do not pick a number | Next stays greyed out until a number is chosen |
| Invalid input | Type a note longer than 300 characters and tap Save my day | Red outline, counter and message; nothing saved, the text stays |
| Persistence | Save an entry, close the app from recents, reopen, open History | The entry is still listed with the same characters |
| Same date twice (edge case) | Tap + again after saving | Today's answers open for editing; History still has one row |
| Edit (ActivityResultLauncher) | History > a day > Edit, change sleep, Save | Back on Insights with "Entry updated" and new cards |
| Delete | Open an entry, tap Delete, confirm | The row disappears from History |
| No location | Deny the location permission | The entry still saves; the daylight card says "not available" |
| Empty list | Settings > Delete all entries, then open History and Trends | "No entries yet" / "Nothing to chart yet", each with a Log today button |
| Dark theme | Switch the phone to dark mode | Every screen uses the dark colours |

Automated unit tests (`LogicTest.kt`) cover the validator, the insight engine, the log choices,
the trends calculation and the daylight calculation.

## 7. AI assistance and external sources

**AI assistance (disclosed as required by the brief).**
- I used Claude (Anthropic) as support in three areas:
  - **Code:** help writing and fixing the Kotlin and XML, including the Room entities, DAOs and JOIN
    queries (`data/`), the ViewModels, validator and insight engine (`logic/`), the Activities, adapters
    and custom views (`ui/`), the layouts (`res/layout/`), and the unit tests (`LogicTest.kt`).
  - **Design:** help turning my Figma design into the XML layouts, colours and styles.
  - **Data research:** help finding and summarising the science behind the chemical and rule texts in
    `assets/chemicals.csv` and `assets/rules.csv`. Each text keeps its source link (`sourceUrl`) so it can be checked.
- The app idea, the screens and the characters are my own design, made in Figma.
- How I tested the AI-assisted parts: the manual checklist in section 6 on a Pixel 9, and the unit tests in `LogicTest.kt`.

**External code and assets**
- Gradle, Kotlin, KSP and Room versions and the Room/Repository structure follow the module's
  Pocket Catalog and Room demo projects (CMPU3026 lab material).
- Nunito font by Vernon Adams et al., SIL Open Font License 1.1 (`app/src/main/NUNITO_LICENSE_OFL.txt`).
- Apache Commons CSV 1.10 (Apache License 2.0) for reading the seed files.
- Daylight calculation: standard sunrise equation (solar declination and hour angle), see `DaylightHelper.kt`.
- Science texts: every chemical and rule in `chemicals.csv` / `rules.csv` has its source URL in the
  `sourceUrl` column, shown in the app.
- UI design and characters: designed by me in Figma (file "ChemMe – App Redesign"); the characters are vector drawables in `res/drawable/ic_*.xml`.
