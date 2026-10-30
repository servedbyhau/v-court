# Current desktop reference for Android work

Always read the latest `develop` source at `D:\VH-Booker\vh-tennis-booker`
before translating behavior into V Court. Do not use the archived Android
lab's older Python tree as the specification. The desktop checkout is a
read-only reference for V Court work; its daily runner and schedule are separate.

## Freshness check

At the 2026-10-30 sync, GitHub `develop` pointed to
`a16240a9b0dc138cf82ecaeb6da716398e1b1b77` (desktop release 0.18.1 plus
unreleased tooling and docs). What changed since the earlier baseline `50ed350`
is listed [below](#desktop-changes-since-the-last-v-court-baseline). That is a
dated observation, not a pinned requirement. At each new session:

```powershell
git -C D:\VH-Booker\vh-tennis-booker status --short --branch
git -C D:\VH-Booker\vh-tennis-booker rev-parse develop
git -C D:\VH-Booker\vh-tennis-booker ls-remote origin refs/heads/develop
```

If the remote hash differs, fetch `origin develop`, then read files from
`origin/develop` with `git show origin/develop:<path>` or update the local
reference without switching the desktop working tree. If the remote cannot be
checked, report the local commit and the freshness limit. Before modifying a
slice, read desktop `AGENTS.md`, its referenced `CLAUDE.md`, the current
`CHANGELOG.md`, and relevant files below.

## Archived compatibility lab

The old `vh-tennis-booker-android` worktree folder no longer exists. Its
reviewed source is still available in the desktop repository at commit
`b85a70e1bc582020d7bea4dbe65175c18fdbf24a`, retained by local branch
`feat/android-on-device`. Use the commit to inspect the experiment without
switching or changing the live desktop checkout:

```powershell
git -C D:\VH-Booker\vh-tennis-booker show b85a70e1bc582020d7bea4dbe65175c18fdbf24a:android/mobile/README.md
git -C D:\VH-Booker\vh-tennis-booker show b85a70e1bc582020d7bea4dbe65175c18fdbf24a:android/mobile/src/main/java/dev/courtbooker/mobile/ShellBridge.java
```

At that same commit, `tools/build_mobile.py` and `tools/build_driver.py` record
the lab's build versions and dependency hashes. The files
`android/mobile/src/main/java/dev/courtbooker/mobile/ProbeService.java` and
`MainActivity.java` show the probe lifecycle and safe tap target. The original
lab stack document is not present in that commit; V Court's adopted choices
are preserved in [TECH_STACK.md](TECH_STACK.md). Read current desktop `develop`
for protocol client and booking behavior. The lab branch was local-only when
checked on 2026-10-30; it was not listed on desktop `origin`. A fresh clone
needs that reviewed commit transferred from the existing local repository
before these commands work. Do not assume it can be fetched from GitHub or
substitute an unverified copy when the commit is unavailable.

## Source map

| V Court concern | Latest desktop sources and tests to inspect |
|---|---|
| Driver protocol, command IDs, timeout and version | `android/app/`, `src/court_booker/driver.py`, `tests/test_driver.py`, `tests/test_build_driver.py` |
| Screen parsing and safe target selection | `src/court_booker/screen.py`, `geometry.py`, `tests/test_screen.py`, `tests/test_geometry.py` |
| Booking state machine and outcomes | `src/court_booker/flow.py`, `runner.py`, `tests/test_flow.py`, `tests/test_runner.py` |
| Languages, sports, courts and venues | `src/court_booker/app_language.py`, `catalog.py`, `tests/test_app_language.py`, `tests/test_catalog.py` |
| Timing, runs and failure handling | `src/court_booker/clock.py`, `watchdog.py`, `lock.py`, `events.py`, matching tests |
| Settings and history behavior | `src/court_booker/config.py`, `history.py`, `history_read.py`, matching tests |
| Observed target app behavior | `docs/field-notes.md`, `docs/how-it-works.md`, `docs/architecture.md` |
| Open desktop engine bugs | `docs/status/known-issues.md` (status notes moved into the repo on 2026-10-30) |

The desktop UI and API can inform wording and settings behavior. Do not copy
FastAPI, React, Windows scheduling, ADB forwarding or the desktop SQLite
schema into the Android runtime. For example, desktop 0.18.0 added a second
MuMu machine tab and automatic MuMu/wake-timer setup; those are desktop
features, not phone requirements. Current booking invariants include a
verified ticket for success, separate refusal and unknown outcomes, optional
venue handling, quota notices and recovery from a moving court list. Confirm
their implementation against the current desktop source and tests each time.

## Desktop changes since the last V Court baseline

From `50ed350` (0.18.0) to `a16240a` (0.18.1 plus unreleased work), compared on
2026-10-30. Carry the engine changes into the slices that port them.

**Booking engine (slices 4 and 5):**

- Calendar day tap (0.18.1, desktop PR #216). A month switched to slides in for
  about 0.2 s, its days not clickable, and a tap then only stops the slide. The
  flow taps the day once it is clickable (`_still_day`, up to 1.0 s), then taps
  it again every 0.5 s (`_DATE_RETAP_AFTER`) while the calendar's prompt stays
  (`_day_taken`), up to `timeout`. `select_date` now returns the screen read
  after the accepted tap and `select_slot` takes it, saving one read. Tests:
  `test_select_date_waits_for_a_sliding_day_to_stop`,
  `test_select_date_taps_the_day_again_while_the_prompt_stays` and
  `test_select_date_gives_up_when_no_tap_is_taken` in `tests/test_flow.py`.
- New label `pick_date`: "Chọn ngày dự kiến sử dụng" in Vietnamese, "Select
  expected date of use" in English (`config.py` `Labels`, `app_language.py`,
  `config.example.toml`).
- Field notes, "Reading the app": the prompt goes within 0.05 s of a tap the app
  takes and the slots show about 0.8 s later; the first real month switch
  worked on 2026-10-30 for 2026-11-01.
- The fix awaits its first 06:00 confirmation (desktop known issue K1, run of
  2026-10-31). Port the version that `docs/status/known-issues.md` shows as
  confirmed.

**Driver:** unchanged. Protocol 1, `driver.py` and the `android/app` sources are
the same; V Court's `Driver.java` differs only in comments (mobile wording),
and `Tree.java` and the manifest are identical.

**Not relevant to V Court:** the Ruff version pin, `tools/release.py`, the `Tag`
workflow, README and process text, and `LabelSettings.pick_date` in the
desktop backend.
