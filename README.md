# Misclick Guard

Protects specific buttons being from misclicked.

Each supported button can use one of three modes:

- **Normal:** No change. Left clicks enabled and available in the right click menu
- **Left click off:** Direct left clicks are blocked, but the protected action remains available in the right click menu
- **Disabled:** Left clicks disabled and unavailable in the right click menu

**Left click off** can optionally **deprioritize the menu entry**, letting an underlying action such as Walk here become the default left click.

When the menu entry is deprioritized, hover tooltips are hidden. **Disabled** always suppresses the hover tooltip.

Supported buttons:

- Auto Retaliate Toggle
- XP Drops Button (Show/Hide/Setup)
- Always Set (Bank) Placeholders Toggle