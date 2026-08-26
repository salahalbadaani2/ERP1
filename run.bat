@echo off
chcp 65001 > nul
title ERP Factory System - نظام ERP المصنعي
echo ===================================================================
echo   تشغيل نظام ERP المصنعي المتكامل (Sales, Returns, GL, Accounts)
echo ===================================================================

javac -encoding UTF-8 -cp ".;lib/*;mysql-connector-j-8.3.0.jar" *.java

if %ERRORLEVEL% EQU 0 (
    echo [OK] تم التجميع بنجاح، جاري تشغيل لوحة التحكم الرئيسية...
    java -Dfile.encoding=UTF-8 -cp ".;lib/*;mysql-connector-j-8.3.0.jar" MainWindow
) else (
    echo [ERROR] حدث خطأ أثناء التجميع! يرجى فحص الكود.
    pause
)