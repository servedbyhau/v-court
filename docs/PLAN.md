# Project plan

## Goal

An Android app that can complete a scheduled tennis court booking on a physical phone without a computer participating in the run. A computer may still be used to build and debug the app.

## Starting point

The original `vh-tennis-booker` has a working Windows/Python booking flow and a separate Android compatibility lab. The lab demonstrated on-device UI reads and taps on MuMu through Shizuku and an instrumentation driver. It has not run the booking flow on a phone. A real booking made during that research was controlled by Python on the host.

This repository starts with documentation only. Import code selectively after its role, dependencies, and behavior have been reviewed for a phone runtime.

## Milestones

1. **Design:** settle the Android runtime, permissions, scheduling, local data, and recovery behavior.
2. **Phone prototype:** read the target app and verify navigation and taps on a physical device without a PC controller.
3. **Booking flow:** move the necessary state machine and checks to Android; support a dry run and an explicitly initiated real booking.
4. **Unattended run:** schedule the opening time; verify wake, lock-screen, Doze, interruption, and reboot behavior.
5. **Release readiness:** review privacy, logging, setup, recovery instructions, and repeatable builds.

## Reuse review

Potentially reusable: screen labels, booking states, date/slot/court checks, ticket verification, error handling, and the Android driver experiment.

Do not carry over by default: Windows Task Scheduler, MuMu setup, ADB host control, Python/FastAPI services, the React desktop UI, or old configuration and personal run data.

## Decisions to make before implementation

- App identity: display name, repository name, Android application ID, and visual identity.
- Android stack and minimum supported version.
- Whether the Shizuku/instrumentation approach is viable on the intended physical phone, including after reboot.
- How the user configures account, court, slots, and schedule without storing sensitive data unnecessarily.
- What the app should report when a booking result cannot be confirmed.

The acceptance test for the core goal is a booking initiated, completed, and verified on the phone while no computer is controlling it.
