# Lumina Launcher
Все версии Minecraft, Vanilla / Fabric / Legacy Fabric (1.8.9–1.13.2), Lumina Client, offline-аккаунты, редактор скинов.

## Запуск
    pip install -r requirements.txt
    python launcher.py

## exe (Windows)
Двойной клик по `build_exe.bat` -> `dist\LuminaLauncher.exe` (папка `client` копируется рядом).
Или залей проект на GitHub: Actions соберёт exe сам (artifact LuminaLauncher).

## Скины (offline)
Кнопка «Скин…» у аккаунта: рисование на текстуре 64x64, превью спереди и сзади, импорт PNG (в т.ч. старые 64x32),
скин по нику с Mojang, экспорт. При запуске лаунчер кладёт скин в ресурспак `LuminaSkin.zip` и включает его в options.txt.
Скин виден только тебе. Модель (Steve/Alex) игра выбирает по UUID ника, выбор в редакторе влияет на превью и рисование.

## Lumina Client
Jar-файлы кладутся в `client/`, имена и диапазоны версий смотри в `client/README.txt`.
