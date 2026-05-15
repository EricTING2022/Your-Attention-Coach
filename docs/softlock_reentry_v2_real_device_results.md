# Softlock Re-entry V2 Real-device Results

Date: 2026-05-15  
Branch: `feature/softlock-reentry-v2`

## Layer 0 Result

Status: Passed.

Reported result:

- Focus timer starts.
- Pause, finish, and exit flows work.
- Apps whitelist and notification interval persist after app restart.

## Layer 1 Result

Status: Pending real-device test.

Layer 1 acceptance requires `AC_ForegroundV2` log evidence that the Accessibility observer can distinguish:

- Attention Coach;
- an Apps whitelist app;
- launcher;
- a non-whitelist app.

Use:

```powershell
adb logcat -c
adb logcat -v time -s AC_ForegroundV2
```

Record the observed `chosenPackage` values here after testing.
