@echo off
start chrome.exe "%~dp0paystub_calculator.html"
if errorlevel 1 (
    start "" "%~dp0paystub_calculator.html"
)

