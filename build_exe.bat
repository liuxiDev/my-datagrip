@echo off
echo 数据库可视化工具 - EXE打包脚本
echo ==============================

echo 检查Java环境...
java -version 2>nul
if %errorlevel% neq 0 (
    echo 错误: 未找到Java环境，请安装JDK 8或更高版本。
    echo 您可以从 https://www.oracle.com/java/technologies/javase/javase8-archive-downloads.html 下载
    goto :end
)

echo 检查Maven环境...
mvn -version 2>nul
if %errorlevel% neq 0 (
    echo 错误: 未找到Maven环境，请安装Maven 3.6或更高版本。
    echo 您可以从 https://maven.apache.org/download.cgi 下载
    goto :end
)

echo 开始构建可执行文件...
call mvn clean package

if %errorlevel% neq 0 (
    echo 构建失败，请检查错误信息。
    goto :end
)

echo 构建成功！
echo EXE文件位置: target\my-datagrip.exe

:end
echo ==============================
pause 