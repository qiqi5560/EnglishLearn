@echo off
title EnglishLearn 一键启动
cd /d "%~dp0"

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

netstat -ano | findstr ":5173" | findstr "LISTENING" >nul
if not errorlevel 1 goto ALREADY

echo 正在启动后端和前端...
start "后端" /min "%~dp0启动后端.bat"
start "前端" /min "%~dp0启动前端.bat"

echo 等待服务就绪, 最长约 2 分钟...
set CNT=0

:WAIT
timeout /t 3 >nul
set /a CNT+=1
netstat -ano | findstr ":5173" | findstr "LISTENING" >nul
if errorlevel 1 goto CHECK
netstat -ano | findstr ":8080" | findstr "LISTENING" >nul
if errorlevel 1 goto CHECK
goto OPEN

:CHECK
echo   还在启动中... (%CNT%)
if %CNT% GEQ 40 goto OPEN
goto WAIT

:ALREADY
echo 检测到服务已在运行, 直接用 Edge 打开.

:OPEN
set "EDGE=C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
if exist "%EDGE%" goto LAUNCH
set "EDGE=C:\Program Files\Microsoft\Edge\Application\msedge.exe"
if exist "%EDGE%" goto LAUNCH
echo 未找到 Edge, 改用默认浏览器打开...
start "" http://localhost:5173
goto DONE

:LAUNCH
echo 正在用 Edge 打开...
start "" "%EDGE%" http://localhost:5173

:DONE
echo.
echo   已打开 http://localhost:5173
echo   如果没自动弹出, 请手动访问上面的地址
echo.
timeout /t 6
