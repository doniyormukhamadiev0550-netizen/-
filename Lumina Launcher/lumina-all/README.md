# Lumina

    launcher/       лаунчер (Python, tkinter)
    mod-1.21.11/    Lumina Client для 1.21.11 (Fabric)
    mod-1.8.9/      Lumina Client для 1.8.9 (Legacy Fabric)

## Самый простой путь (ничего не ставить)
1. Создай репозиторий на GitHub и залей туда эту папку.
2. Вкладка Actions -> Build Lumina -> дождись зелёной галочки.
3. Скачай artifact **Lumina** (внутри LuminaLauncher.exe и папка client с обоими jar).
4. Запусти LuminaLauncher.exe -> введи ник -> нажми «Lumina 1.21.11» или «Lumina 1.8.9».

## Что нового
Стеклянное меню с анимациями, выбор двух цветов и тёмного/светлого фона, новое главное меню (Singleplayer / Multiplayer,
углы: Settings, Skin, красный Exit), метка Lumina Client в инвентаре. 30 модулей на 1.21.11, 17 на 1.8.9.

## Локально
Лаунчер: `cd launcher && pip install -r requirements.txt && python launcher.py` (или build_exe.bat для exe).
Моды: mod-1.21.11 нужны JDK 21 + Gradle 9.2; mod-1.8.9 нужны JDK 17 + Gradle 8.10 (`gradle build`).
Собранные jar положи в launcher/client/.
