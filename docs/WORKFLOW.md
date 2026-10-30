# Work and branch rules

Adopted from the user's instructions on 2026-10-25; branch topology updated
2026-10-29. These rules apply to the `v-court` repository.

## Branch topology

- `main` is the stable line. Promote reviewed, tested `develop` work to
  `main` only when it is ready for a stable milestone or release.
- `develop` is V Court's integration line and the base for new work. Keep it
  buildable; merge completed child branches into it. This is separate from
  the desktop repository's `develop`, which is a read-only behavior reference.
- Each reviewable part gets a named branch from the current `develop`, named
  as [AGENTS.md](../AGENTS.md) ("Planning and branches") says:
  `<type>/vcourt-<topic>`. Record the base commit, deliverable and exit check in the plan.
  Review and merge the branch into `develop` after its exit check passes.
- Do not push implementation work directly to `main`, force-push shared
  branches or silently promote an unverified MuMu/phone milestone. A stable
  promotion from `develop` to `main` gets its own review and evidence.

1. **Plan first.** State the outcome, small steps and validation before project
   work. Before editing code or docs, record the actionable plan in
   [NEXT_SESSION.md](NEXT_SESSION.md) or a linked task plan.
2. **One branch per part.** Split a plan into small reviewable deliverables.
   Give each its own named Git branch from `develop`, base commit, scope and
   exit check. Record any split before starting the new part.
3. **Preserve work.** Read [PROGRESS.md](PROGRESS.md), the next-session plan and
   Git status at session start. Finish or safely preserve uncommitted changes
   before switching branches; do not mix unrelated parts.
4. **Verify and update.** Test what the part changes. Update PROGRESS with
   actual branch/commit, evidence, limits and remaining work. Update
   NEXT_SESSION before ending a session.
5. **Respect the repo boundary.** This repo is the development destination.
   Import reviewed mobile files from sibling projects selectively. Do not
   bulk-copy the desktop tree or private test artifacts.

For a read-only answer, state a brief plan if inspection is needed; no branch
or file update is required unless the repository changes. Product milestones
are in [PLAN.md](PLAN.md); [CONTEXT.md](CONTEXT.md) holds durable decisions.
