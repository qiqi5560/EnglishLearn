@echo off
title EnglishLearn 一键启动
cd /d "%~dp0"
setlocal enabledelayedexpansion

echo ============================================
echo   EnglishLearn 一键启动
echo.
echo   前端地址 : http://localhost:5173
echo   后端地址 : http://localhost:8080/api
echo.
echo   所有下载依赖都在:
echo   D:\EnglishLearn-运行依赖（可一键删除）
echo   不用时直接删除上面这个文件夹即可
echo ============================================
echo.
echo 正在检测服务状态...

set NEED_BACKEND=0
set NEED_FRONTEND=0

netstat -ano | findstr ":8080 " | findstr "LISTENING" >nul
if errorlevel 1 set NEED_BACKEND=1
netstat -ano | findstr ":5173 " | findstr "LISTENING" >nul
if errorlevel 1 set NEED_FRONTEND=1

if "!NEED_BACKEND!"=="1" (
  echo   - 后端未运行, 正在启动...
  start "后端" /min "%~dp0启动后端.bat"
) else (
  echo   - 后端已在运行 (8080)
)

if "!NEED_FRONTEND!"=="1" (
  echo   - 前端未运行, 正在启动...
  start "前端" /min "%~dp0启动前端.bat"
) else (
  echo   - 前端已在运行 (5173)
)

if "!NEED_BACKEND!!NEED_FRONTEND!"=="00" goto OPEN

echo.
echo 等待服务就绪, 最长约 2 分钟...
set CNT=0

:WAIT
timeout /t 3 >nul
set /a CNT+=1

if "!NEED_BACKEND!"=="1" (
  netstat -ano | findstr ":8080 " | findstr "LISTENING" >nul
  if not errorlevel 1 (
    set NEED_BACKEND=0
    echo   - 后端已就绪 (8080)
  )
)
if "!NEED_FRONTEND!"=="1" (
  netstat -ano | findstr ":5173 " | findstr "LISTENING" >nul
  if not errorlevel 1 (
    set NEED_FRONTEND=0
    echo   - 前端已就绪 (5173)
  )
)

if "!NEED_BACKEND!!NEED_FRONTEND!"=="00" goto OPEN
echo   还在启动中... (!CNT!)
if !CNT! GEQ 40 goto TIMEOUT
goto WAIT

:TIMEOUT
echo.
echo 启动超时, 以下服务仍未就绪:
if "!NEED_BACKEND!"=="1" (
  echo   [后端 8080] 启动失败
  echo   请查看日志: D:\EnglishLearn-运行依赖（可一键删除）\logs\backend.log
  if exist "D:\EnglishLearn-运行依赖（可一键删除）\logs\backend.log" start "" notepad "D:\EnglishLearn-运行依赖（可一键删除）\logs\backend.log"
)
if "!NEED_FRONTEND!"=="1" (
  echo   [前端 5173] 启动失败, 请手动双击 启动前端.bat 查看报错
)
echo.
pause
goto END

:OPEN
echo.
echo 正在打开浏览器...
set "EDGE=C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
if exist "%EDGE%" goto LAUNCH
set "EDGE=C:\Program Files\Microsoft\Edge\Application\msedge.exe"
if exist "%EDGE%" goto LAUNCH
echo 未找到 Edge, 改用默认浏览器打开...
start "" http://localhost:5173
goto DONE

:LAUNCH
start "" "%EDGE%" http://localhost:5173

:DONE
echo.
echo   已打开 http://localhost:5173
echo   如果没自动弹出, 请手动访问上面的地址
echo.
timeout /t 6

:END
