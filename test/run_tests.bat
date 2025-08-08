@echo off
REM Batch script to run integration tests for Data Profiler Platform
REM Usage: run_tests.bat [module] [config]

echo ========================================
echo Data Profiler Platform Integration Tests
echo ========================================

REM Check if Python is available
python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Python is not installed or not in PATH
    pause
    exit /b 1
)

REM Check if requests library is installed
python -c "import requests" >nul 2>&1
if %errorlevel% neq 0 (
    echo [INFO] Installing required dependencies...
    pip install -r requirements.txt
    if %errorlevel% neq 0 (
        echo [ERROR] Failed to install dependencies
        pause
        exit /b 1
    )
)

REM Set default values
set MODULE=%1
set CONFIG=%2

if "%MODULE%"=="" set MODULE=all
if "%CONFIG%"=="" set CONFIG=test_config.json

echo [INFO] Running tests with module: %MODULE%
echo [INFO] Using config file: %CONFIG%
echo.

REM Run the test
python test_runner.py --module %MODULE% --config %CONFIG%

REM Check result
if %errorlevel% equ 0 (
    echo.
    echo [SUCCESS] All tests completed successfully!
) else (
    echo.
    echo [FAILURE] Some tests failed. Please check the output above.
)

pause