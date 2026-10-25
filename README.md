# V Court

V Court is an Android project for scheduled tennis court booking. Its intended
phone run will not depend on a Windows PC, emulator or desktop service.

**Current phase:** develop and validate the Android app on MuMu. Physical-phone
testing follows after the emulator implementation works. This repository is the
main development home; the sibling `vh-tennis-booker-android` folder is a
compatibility lab of the original desktop project, not this app's source tree.

The lab demonstrated an Android-controlled driver read and tap on MuMu. A separate
real booking during that research was controlled by Python on the host. This
repository has no Android booking engine yet. The driver core will be imported
selectively with its protocol and tests; the desktop Python backend and React UI
are references, not app dependencies.

Start with [context](docs/CONTEXT.md), [the plan](docs/PLAN.md),
[progress](docs/PROGRESS.md), [next-session work](docs/NEXT_SESSION.md) and
[workflow](docs/WORKFLOW.md).

V Court is a personal project and is not affiliated with or endorsed by Vinhomes.
