# 数据库可视化工具

一个基于Java Swing的数据库操作可视化工具，类似于DBeaver、DataGrip等数据库管理软件。

## 功能特性

- 支持多种数据库连接（MySQL, PostgreSQL, Oracle, SQL Server, SQLite）
- 数据库连接管理（添加、编辑、删除）
- 数据库结构浏览（表、视图等）
- SQL编辑器及执行
- 查询结果展示
- 数据库对象CRUD操作

## 技术栈

- Java 8+
- Swing GUI
- HikariCP 连接池
- JDBC驱动

## 如何运行

### 环境要求

- JDK 8或更高版本
- Maven 3.6+

### 构建和运行

1. 克隆项目到本地
2. 使用Maven构建项目：

```bash
mvn clean package
```

3. 运行生成的jar包：

```bash
java -jar target/datab-1.0-SNAPSHOT-jar-with-dependencies.jar
```

### 生成可执行文件(EXE)

如果需要生成Windows可执行文件，请执行以下操作：

1. 确保已安装Java 8+和Maven 3.6+
2. 运行项目目录下的`build_exe.bat`批处理文件
3. 生成的EXE文件将位于`target/my-datagrip.exe`

注意：生成的EXE文件需要用户系统中安装有JRE 8+环境才能运行，或者在环境变量中配置了`JAVA_HOME`指向正确的JDK/JRE安装目录。

## 使用说明

1. 启动应用后，可以通过"文件" -> "新建连接"菜单添加数据库连接
2. 在左侧数据库树中选择连接，并点击"刷新"按钮加载数据库结构
3. 选择表可以自动生成查询语句
4. 在SQL编辑器中编写和执行SQL语句
5. 查询结果将显示在下方的结果表格中

## 注意事项

- 保存的连接信息存储在应用程序目录下的`connections.json`文件中
- 密码以明文形式存储，生产环境使用时请注意数据安全

![ef29961aed985a8ff3244daf1f254d0](https://github.com/user-attachments/assets/e3ca1238-8aff-4c2f-bae6-174454738f45)
