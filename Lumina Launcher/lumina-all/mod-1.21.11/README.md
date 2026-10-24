# Lumina Client for Minecraft 1.21.11 (Fabric)

Right Shift: menu. Hold C: Zoom. Config: config/lumina.json.

- Glass menu with tabs HUD / INFO / VISUAL / UTILITY / SETTINGS, animated cards, moving background.
- SETTINGS: two accent colors (10 presets each) and Dark / Light background.
- Custom main menu: Singleplayer / Multiplayer, Settings, Skin, red Exit.
- "Lumina Client" tag in the corner of every inventory.
- 30 modules: FPS, CPS, Coordinates, Ping, Speed, Keystrokes, Armor Status, Potion Status, Clock, Direction, Memory,
  Server Address, Session Timer, Light Level, Totem Counter, Day Counter, Fullbright, Zoom, No Fire, No Smoke, No Fog,
  Low Shield, No Hurt Cam, No View Bobbing, Reduce Particles, No Weather, Crosshair, Toggle Sprint, Toggle Sneak, Auto Respawn.

Build: JDK 21 + Gradle 9.2 -> `gradle build` -> build/libs/lumina-1.0.0+mc1.21.11.jar

Note: No Fire, No Smoke, No Fog, Low Shield and No Hurt Cam are mixins into rendering code. They are optional:
if a target does not match this exact version the mixin is skipped (see latest.log, search "lumina") and the rest works.
