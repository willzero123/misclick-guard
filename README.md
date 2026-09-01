# Misclick Guard

Protects specific buttons being from misclicked.

Supported buttons:

- Auto Retaliate
- XP Drops (Show/Hide/Setup)
- Always Set (Bank) Placeholders

Each supported button can use one of three modes:

- **Normal:** No change. Left clicks enabled and available in the right click menu
- **Left click off:** Direct left clicks are blocked, but the protected action remains available in the right click menu
- **Disabled:** Left clicks disabled and unavailable in the right click menu

**Left click off** can optionally **deprioritize the menu entry**, letting an underlying action such as Walk here become the default left click. When this is enabled, hover tooltips are hidden.

**Disabled** removes the menu entry and suppresses the hover tooltip.