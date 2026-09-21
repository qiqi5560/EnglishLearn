@echo off
title EnglishLearn 后端 (8080)
cd /d "%~dp0backend-spring"

set "DEPS=D:\EnglishLearn-运行依赖（可一键删除）"
set "MVN=%DEPS%\maven\apache-maven-3.9.16\bin\mvn.cmd"
set "LOG=%DEPS%\logs\backend.log"

echo 后端启动中, 输出同时写入: %LOG%
echo 启动完成前请不要关闭本窗口。
echo.

if not exist "%MVN%" (
  echo [错误] 找不到 Maven: %MVN%
  echo 运行依赖缺失, 请重新解压/安装 D:\EnglishLearn-运行依赖（可一键删除）
  pause
  exit /b 1
)

netstat -ano | findstr ":8080 " | findstr "LISTENING" >nul
if not errorlevel 1 (
  echo [提示] 8080 端口已被占用, 后端可能已在运行。
  echo 若确定要重启, 请先双击 一键停止.bat
  pause
)

call "%MVN%" -s "%DEPS%\settings.xml" -o -l "%LOG%" spring-boot:run

echo.
echo 后端已退出。若非你手动关闭, 请查看上面的日志: %LOG%
pause
