@echo off
title EnglishLearn 前端 (5173)
cd /d "%~dp0frontend"

echo 前端启动中, 启动完成前请不要关闭本窗口。
echo.

where npm >nul 2>nul
if errorlevel 1 (
  echo [错误] 未找到 npm, 请先安装 Node.js 并确保已加入 PATH
  pause
  exit /b 1
)

if not exist "node_modules" (
  echo 首次运行, 正在安装前端依赖, 约 1-3 分钟...
  call npm install
  if errorlevel 1 (
    echo [错误] 依赖安装失败, 请查看上方报错
    pause
    exit /b 1
  )
)

call npm run dev

echo.
echo 前端已退出。若非你手动关闭, 请查看上方报错。
pause
