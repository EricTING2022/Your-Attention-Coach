# Set Plan Schedule UI References

These generated references freeze the agreed direction for the Plan page and Schedule editor before implementation.

## Files

- `generated_plan_page_reference_source.png`
  - Use the left phone screen as the Plan page target.
  - Ignore the old dark Schedule editor shown on the right side of this generated image.
  - Key Plan page decisions:
    - light Material-style sheet;
    - Duration and Priority stay in the top row;
    - Targets stays below;
    - schedule summary must fit without clipping.

- `generated_schedule_editor_reference.png`
  - Use this as the final Schedule editor target.
  - Key Schedule editor decisions:
    - light theme matching the Plan page;
    - top-right green check is the only save schedule control;
    - no bottom save button;
    - two-column hour/minute wheel;
    - fixed 2 x 3 duration controls with custom `min` input;
    - floating feedback text: `Schedule is saved`.

## Related Spec

- Implementation plan: `../../superpowers/plans/2026-05-10-set-plan-schedule-ui.md`
- User notes and original reference screenshots: `../../../internal_proposal/set_plan/`
