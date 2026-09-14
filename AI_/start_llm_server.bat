@echo off
chcp 65001 >nul
rem ============================================================
rem  start_llm_server.bat —— 启动大模型服务（"主机/组长"专用）
rem
rem  作用：以「监听所有网卡」的方式启动 Ollama，
rem        让同一 WiFi 或本机热点下的组员也能连进来。
rem
rem  重要：
rem    1) 启动后请【保持此窗口开启】，关掉窗口组员就断线了；
rem    2) 组员需要你电脑的 IP，请另开窗口执行  ipconfig  查看；
rem       连热点时通常是 192.168.137.1，连 WiFi 时看"无线局域网适配器"的 IPv4 地址。
rem    3) 首次使用需放行防火墙（管理员 PowerShell 执行一次）：
rem       New-NetFirewallRule -DisplayName "Ollama 11434" -Direction Inbound -Protocol TCP -LocalPort 11434 -Action Allow
rem ============================================================

set OLLAMA_MODELS=D:\Ollama\models
set OLLAMA_HOST=0.0.0.0:11434

echo ============================================================
echo  正在启动 Ollama 大模型服务
echo  模型目录 : D:\Ollama\models
echo  监听地址 : 0.0.0.0:11434（允许同一网络内的组员连接）
echo ------------------------------------------------------------
echo  组员在本机设置环境变量即可连接（IP 用 ipconfig 查询）：
echo      setx LLM_SERVER_URL "http://192.168.137.1:11434"
echo  然后重开终端运行 demo_chat.py / demo_voice.py 即可。
echo ============================================================
echo.

start "Ollama Server" "D:\Ollama\ollama\ollama.exe" serve

echo 服务已在新窗口启动，请保持那个窗口开启，不要关闭。
pause
