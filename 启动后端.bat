@echo off
title EnglishLearn 后端 (8080)
cd /d "%~dp0backend-spring"
call "D:\EnglishLearn-运行依赖（可一键删除）\maven\apache-maven-3.9.16\bin\mvn.cmd" -s "D:\EnglishLearn-运行依赖（可一键删除）\settings.xml" -o -l "D:\EnglishLearn-运行依赖（可一键删除）\logs\backend.log" spring-boot:run
