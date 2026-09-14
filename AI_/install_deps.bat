@echo off
REM ============================================================
REM  install_deps.bat - one-click dependency install
REM  Everything (venv + pip download cache) stays on drive D:
REM  Usage: double click this file, or run it from a terminal.
REM ============================================================
setlocal
cd /d "%~dp0"

REM pip download cache -> <project>\pip_cache  (never C:)
set "PIP_CACHE_DIR=%~dp0pip_cache"
set "PIP_DISABLE_PIP_VERSION_CHECK=1"

echo [1/3] pip cache dir : %PIP_CACHE_DIR%

if exist ".venv\Scripts\python.exe" (
    echo [2/3] virtualenv .venv already exists
) else (
    echo [2/3] creating virtualenv .venv ...
    py -3.11 -m venv .venv || python -m venv .venv
)

echo [3/3] installing dependencies from requirements.txt ...
".venv\Scripts\python.exe" -m pip install -r requirements.txt

echo.
echo Done. pip cache stays in : %PIP_CACHE_DIR%
pause
