@echo off
cd /d %~dp0
python -m pip install --upgrade pip requests pillow pyinstaller || goto :err
pyinstaller --noconfirm --clean --onefile --windowed --name LuminaLauncher launcher.py || goto :err
if not exist dist\client mkdir dist\client
xcopy /E /I /Y client dist\client >nul
echo.
echo Готово: dist\LuminaLauncher.exe  (папка client рядом с exe)
pause
exit /b 0
:err
echo Ошибка сборки. Проверь, что установлен Python 3.9+ (с галочкой Add to PATH).
pause
exit /b 1
