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

Status: Passed with one follow-up refinement.

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

### User-provided log summary

Launcher:

- `chosenPackage=com.sec.android.app.launcher`

Whitelist app:

- `chosenPackage=com.android.chrome`

Attention Coach during focus timer:

- `chosenPackage=com.example.attentioncoach`

Attention Coach without focus timer:

- A transient System UI event appeared first:
  - `eventPackage=com.android.systemui`
  - `rootPackage=com.example.attentioncoach`
  - `chosenPackage=com.android.systemui`
- A correct Attention Coach event followed immediately:
  - `chosenPackage=com.example.attentioncoach`

Non-whitelist app:

- `chosenPackage=com.openrice.android`

### Interpretation

The real device can distinguish Attention Coach, a whitelist app, launcher, and a non-whitelist app. This satisfies the core Layer 1 requirement.

Two refinements were identified from the logs:

1. Accessibility events are event-driven, not a fixed polling cycle. The repeated lines are duplicate window/content events from the system, not a 2-second monitoring loop.
2. System UI can emit transient events while the actual app remains visible in `rootPackage`. For foreground classification, `rootPackage` should be preferred when `eventPackage` is `com.android.systemui`.

### Follow-up change

After this test, duplicate foreground observations were throttled to one record per package every 5 seconds, while package changes are still recorded immediately. This reduces noisy logs and repeated SharedPreferences writes without delaying launcher / whitelist / non-whitelist transitions.
