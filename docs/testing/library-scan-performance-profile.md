# Library Scan Performance Profile

**Date:** 2026-08-21  
**Status:** Awaiting connected-device run

## Environment

- Host implementation environment: Windows
- Connected Android devices during implementation: none
- Library corpus: unavailable without a connected device and selected SAF tree

## Profiling command

Install the debug app, select the real music folder, and run:

```powershell
.\gradlew.bat connectedDebugAndroidTest --instrumentation-arg class=ca.stewark.nocturnel.library.profile.LibraryScanPerformanceProfileTest --instrumentation-arg scanProfile=true
```

The instrumentation output reports five measured runs and medians for `DocumentFile` discovery, direct `DocumentsContract` discovery, sequential/two-worker/four-worker tag extraction, and in-memory Room persistence. It asserts discovery and metadata equivalence before reporting results.

## Results

No measurements were recorded because `adb devices -l` reported no connected devices.

| Candidate | Decision | Reason |
|---|---|---|
| Direct `DocumentsContract` traversal | NOT ADOPTED | No same-device measurement or provider compatibility evidence is available. |
| Metadata parallelism 2 | NOT ADOPTED | No same-device timing or resource-stability evidence is available. |
| Metadata parallelism 4 | NOT ADOPTED | No same-device timing or resource-stability evidence is available. |

The production path therefore retains `DocumentFile` traversal and sequential metadata reads. Rerun the command above on the target device and replace this section with the raw runs, medians, equivalence result, errors, and explicit `ADOPT` or `REJECT` decisions before promoting either candidate.
