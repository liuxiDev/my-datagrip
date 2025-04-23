@echo off
echo 数据库可视化工具 - 构建与运行脚本
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

echo 开始构建项目...
call mvn clean package

if %errorlevel% neq 0 (
    echo 构建失败，请检查错误信息。
    goto :end
)

echo 构建成功，正在启动应用...
java -jar target/datab-1.0-SNAPSHOT-jar-with-dependencies.jar

:end
echo ==============================
pause 