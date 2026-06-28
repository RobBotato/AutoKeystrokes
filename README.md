# AutoKeystrokes

AutoKeystrokes is a lunar keystrokes mod that shows the keys you press (W, A, S, D, Space, mouse clicks). It also tracks your Clicks Per Second (CPS). Customizing features are similar to Lunar Client.

On top of standard keystrokes, this mod contains two main features: **FastPlace** and **NoJumpDelay**, with real-time visual display on the screen overlay. These addons are toggleable with keybinds.

HUD Settings - `P`, FastJump - `B`, FastPlace - `Q`

---

## Previews

[Insert screenshots or GIFs here]

---

## Main Features

### FastPlace
Normally, Minecraft has a delay of 4 ticks (0.2 seconds) when you place blocks. FastPlace removes this delay. This lets you place blocks as fast as possible every tick you hold down with the mouse button.

- **CPS & Key Sync**: The CPS counter on your screen will count the blocks you place per one second. The right-click key on the overlay will also flicker quickly to show how fast you are placing blocks to mimic high CPS.
- **Left-Click Autoclicker**: You can also turn on an autoclicker for left clicks. The left-click key on the overlay will flicker to match your click speed.
- **Prevent Air Clicks**: The mod contains a configuration that stops you from accidentally placing blocks or clicking when you are looking at the sky (missing a block). Completely optional.

### FastJump (NoJumpDelay)
Normally, when you land on the ground, Minecraft makes you wait 10 ticks (0.5 seconds) before you can jump again. NoJumpDelay removes this wait time. You can hold down the spacebar to jump instantly and continuously.

- **Ground Detection**: The spacebar key on your screen overlay tracks when you touch the ground. When you land, the spacebar key on the screen quickly unpresses and presses again. This shows the exact timing of your jumps to mimic legitimacy.

---

## Technical Details

### FastPlace
The mod resets the game's internal `rightClickDelay` (and `missTime` for left clicks) to 0 every tick. It also stops item usage if your crosshair is pointing at the air.

### NoJumpDelay
The mod resets the jumping delay to 0 on every tick. This lets your player jump immediately upon touching the ground.