# Project guidance: Android cooking app

## Source of truth and change discipline

- Before implementing or changing a screen, flow, report, or module, read `BAN_GIAO_DU_AN_ANDROID.md` and the relevant sections and image references in `ui-ux-bep-nha/THIET_KE.md`.
- The existing UI/UX handoff and its screen/state images are the approved design reference. Reproduce its Vietnamese labels, layout, colors, typography, spacing, navigation, and states faithfully. Do not invent, omit, rename, or redesign elements. If a requirement is not specified, inspect the current reference assets and handoff first; ask the user only when the source remains genuinely ambiguous.
- Treat the source project and supplied report/course documents as authoritative. Do not fabricate implementation details, test results, data, citations, or report content. Clearly distinguish confirmed requirements, proposed details, and unverified implementation status as the handoff does.
- Preserve existing project content. Keep changes limited to the user's requested scope and do not overwrite supplied reference/report files unless explicitly asked.
- Before work on a report, inspect its corresponding `.docx` and the supplied report requirements. Match the existing document structure and project facts; do not infer missing results.
- Treat responsive behavior as a project-wide requirement: every screen must adapt to phone size, density, orientation, system bars, and IME/soft keyboard while preserving the supplied visual hierarchy, spacing rhythm, colors, and component style. Use scrollable content for constrained viewports, size bounds for flexible regions, and keep important actions reachable. Validate representative small/large viewports and keyboard-visible states; do not stretch a component to consume all remaining height unless the design explicitly calls for it.

## User's selected role

The user selected **Người 3**. Treat this as the active assignment for this project, even though the handoff describes its role table as a proposed update:

- Module 5: Kho món cá nhân (favorites, collections, history, personal notes).
- Module 6: Nấu hôm nay (meal plan, recipes, serving counts).
- Module 7: Mua sắm (shopping list and ingredient aggregation).
- Coordinate shared use cases/database and handoffs with the other modules; do not assume schema/API contracts that have not been agreed.

## Git publishing preference

- Commit completed changes on the current feature branch when they are ready to keep.
- Do not push commits to the remote unless the user explicitly asks to push. A request to implement, fix, or commit does not by itself authorize pushing.

## Architecture and team integration

- The user has confirmed MVVM for the entire project. Keep Java and the approved UI/UX; refactor incrementally without changing screen designs or flows. Activities/views render state and forward actions; ViewModels own screen state and invoke use cases; repositories are the data boundary; SQLite/Firebase data sources perform persistence/network I/O. Do not place database/network I/O or business calculations in Activities/views.
- The team shares the `Android-Project` repository. Each person works on a separate branch and integrates through a Pull Request with AI review. Never push unless explicitly asked. Use `BAN_GIAO_DU_AN_ANDROID.md` as the current decision register; it supersedes stale statements below and older proposals.
- Keep module boundaries: modules do not reach into another module's Activity internals. Coordinate shared navigation/app shell and database schema/migrations with their designated owners before changing them. Owner names are still to be assigned by the group.
- Preserve whether a point is user-confirmed, an operational proposal, or pending team confirmation. Never present an unconfirmed proposal as an approved group or instructor requirement.

## Project references

- Project handoff: `BAN_GIAO_DU_AN_ANDROID.md`.
- Proposed shared data contract for Người 3 modules: `THONG_NHAT_DU_LIEU_MODULE_5_7.md` (proposal baseline; team confirmation points are listed inside).
- UI/UX specification: `ui-ux-bep-nha/THIET_KE.md`.
- UI images: `ui-ux-bep-nha/screens/`, `ui-ux-bep-nha/states/`, `ui-ux-bep-nha/showcase.png`, and the design/flow images in `ui-ux-bep-nha/`.
- Course assignment and report requirements: `Yeu cau bai tap nhom Mobile App 11.2025 (1).docx`, `.source-review/1.Huong dan du an nhom Mobile App D22.doc`, and other references under `.source-review/`.

## Current implementation boundary

`MainActivity` contains module 5 and module 6 UI/flow code; local persistence is in `PersonalDataDbHelper`. Module 6 currently supports recipe selection, independent serving counts, atomic save and draft restoration; nine Android instrumentation tests passed on 2026-11-20. Module 7 and recipe-catalog/Firebase integration remain incomplete. The confirmed target architecture is MVVM for the whole app, but current code has not yet been refactored to MVVM. The debug build seeds local preview fixtures copied from supplied design content; they are not production recipes or real user data, and release builds do not seed them. System bars/insets use AndroidX edge-to-edge APIs and back navigation uses `OnBackPressedDispatcher`. A black emulator display was cleared by rebooting the Android 16 emulator; cold launch then displayed `MainActivity` without a fatal exception or ANR. Validate on physical devices separately. Apply responsive UI to every module/screen, including system bars and keyboard; validate multiple viewport sizes. Check current files/status before making implementation or report claims. The handoff's latest status supersedes this snapshot when code changes.

## Data decisions proposed for Người 3

Use `THONG_NHAT_DU_LIEU_MODULE_5_7.md` alongside `BAN_GIAO_DU_AN_ANDROID.md` for modules 5–7. Confirmed decisions include immutable Firebase recipe IDs, shared canonical ingredient IDs, one planned row per recipe with independent servings, BigDecimal scaling, and the user-provided unit catalog. The common serving limit and detailed shopping update rules still require owner/group confirmation. Treat proposals as proposals until the handoff marks them approved.
