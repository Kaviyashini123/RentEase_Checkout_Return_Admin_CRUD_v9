# Checkout & Return Processing — CRUD UI update

Changes in this patch:

- Added an **Edit handover** action to active rental cards.
- Added a confirmed **Delete record** action for completed checkout records; the action explains that the completed record and linked damage reports are permanently removed.
- Replaced the unrestricted generic record edit form with a focused handover correction form. Reservation/vehicle identity, stage, return fields, and late fees are not editable from this form.
- Tightened service-side updates to validate odometer, fuel, and expected return date, while keeping completed return records read-only.
- Added success/error feedback to the operations page and responsive styling for the new actions.
- Kept the existing design system and checkout/return workflow.

## Verification note

The source files were reviewed after editing, but a Maven build could not be run in this environment because Maven and a Maven wrapper are not installed. Please open the project in IntelliJ, reload Maven, and run the application before using it with your database.
