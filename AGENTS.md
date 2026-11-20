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

## Project references

- Project handoff: `BAN_GIAO_DU_AN_ANDROID.md`.
- Proposed shared data contract for Người 3 modules: `THONG_NHAT_DU_LIEU_MODULE_5_7.md` (proposal baseline; team confirmation points are listed inside).
- UI/UX specification: `ui-ux-bep-nha/THIET_KE.md`.
- UI images: `ui-ux-bep-nha/screens/`, `ui-ux-bep-nha/states/`, `ui-ux-bep-nha/showcase.png`, and the design/flow images in `ui-ux-bep-nha/`.
- Course assignment and report requirements: `Yeu cau bai tap nhom Mobile App 11.2025 (1).docx`, `.source-review/1.Huong dan du an nhom Mobile App D22.doc`, and other references under `.source-review/`.

## Current implementation boundary

`MainActivity` now contains the first module 5 implementation pass, with local personal-data storage in `PersonalDataDbHelper`. Module 6/7 and recipe-catalog integration remain unimplemented. The debug build seeds local preview fixtures copied from supplied design content; they are not production recipes or real user data, and release builds do not seed them. The repo's `gradle-daemon-jvm.properties` was removed because it made Gradle select an incomplete JRE. `assembleDebug` succeeds using Android Studio's full JBR 25.0.2. The note-seeding column mismatch was fixed. System bars/insets use AndroidX edge-to-edge APIs and back navigation uses `OnBackPressedDispatcher`. A black emulator display was cleared by rebooting the Android 16 emulator; cold launch then displayed `MainActivity` in about one second without a fatal exception or ANR. Validate on physical devices separately. Apply the responsive UI requirement to every module and screen, including system bars and keyboard; validate multiple viewport sizes. The note screen has a bounded responsive editor and scrollable body; other screens have not had full visual QA. Check current files/status before making implementation or report claims.

## Data decisions proposed for Người 3

Use `THONG_NHAT_DU_LIEU_MODULE_5_7.md` as the recommended contract when analyzing modules 5–7: immutable Firebase recipe IDs, shared canonical ingredient IDs, one planned recipe row with selected servings, exact decimal scaling, only dimension-safe unit conversion, and shopping contributions linked to source recipe ingredient rows. Treat the document as a proposal pending team confirmation, not as a team-approved schema.
