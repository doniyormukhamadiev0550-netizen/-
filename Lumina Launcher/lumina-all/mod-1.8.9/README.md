# Lumina Client for Minecraft 1.8.9 (Legacy Fabric)

Right Shift: menu. Hold C: Zoom. N / M: stopwatch start-stop / reset. Config: config/lumina.json.

- Same glass menu, themes (two colors + Dark/Light), animated background, main menu and inventory tag as the 1.21.11 build.
- 17 modules: FPS, CPS, Coordinates, Speed, Keystrokes, Clock, Direction, Memory, Session Timer, Date, Stopwatch,
  Fullbright, Zoom, Crosshair, No View Bobbing, Reduce Particles, Toggle Sprint.
- This is NOT every Lunar feature. Armor/Potion Status, Freelook, Time Changer, hitbox and similar ones need
  version-specific hooks and are added in batches once this base is confirmed to compile and run.

Build: JDK 17 + Gradle 8.10 -> `gradle build` -> build/libs/lumina-1.0.0+mc1.8.9.jar
If compile fails on a mapped name (player.x, options.gamma, textRenderer.drawWithShadow ...), look it up with
`gradle genSources` or send me the error text.
