# V Court

V Court is an Android project for scheduled tennis court booking. Its intended
phone run will not depend on a Windows PC, emulator or desktop service.

**Current phase:** develop and validate the Android app on MuMu. Physical-phone
testing follows after the emulator implementation works. This repository is the
main development home. The original compatibility lab is preserved in the
desktop repository's Git history; its old worktree folder has been removed.

The completed lab demonstrated an Android-controlled driver read and tap on
MuMu. A separate real booking during that research was controlled by Python
on the host. This repository now builds a Java instrumentation driver and a
minimal native app shell that checks Shizuku readiness. It has no Android
booking engine or daily scheduler yet. The desktop Python backend and React UI
are references, not app dependencies.

The current builds produce two development APKs. The release target is one
V Court APK. Python in `tools/` runs only on the build machine; Shizuku remains
a separate installed prerequisite. See [the migration roadmap](docs/ROADMAP.md)
for the ordered conversion from the desktop behavior.

Development happens on short-lived branches from `develop`; validated work
merges back into `develop`, and a stable milestone is promoted to `main`.
See [the branch workflow](docs/WORKFLOW.md).

**Desktop reference:** always check the latest `develop` at
`D:\VH-Booker\vh-tennis-booker` (GitHub `servedbyhau/vh-tennis-booker`) before
porting behavior. The lab's inherited
Python tree is older. See [desktop source map](docs/DESKTOP_REFERENCE.md).

For a fresh machine, follow [Android setup](docs/SETUP.md). The chosen stack
and the boundary between implemented and planned parts are in
[technology stack](docs/TECH_STACK.md).

AI sessions start with [AGENTS.md](AGENTS.md), [context](docs/CONTEXT.md), [the plan](docs/PLAN.md),
[progress](docs/PROGRESS.md), [next-session work](docs/NEXT_SESSION.md) and
[workflow](docs/WORKFLOW.md).

V Court is a personal project and is not affiliated with or endorsed by Vinhomes.
