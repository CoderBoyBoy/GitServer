# GitServer

基于JGit的Git服务器，支持HTTP和SSH两种协议。

## 功能特性

- ✅ **HTTP协议支持**: 使用JGit HTTP服务器，支持Git的HTTP/HTTPS协议
- ✅ **SSH协议支持**: 使用Apache SSHD，支持Git的SSH协议
- ✅ **自动仓库创建**: 首次访问时自动创建Git仓库
- ✅ **纯Java实现**: 基于JGit 7.5.0最新版本
- ✅ **易于部署**: 单一JAR文件，无需额外配置

## 技术栈

- **JGit 7.5.0**: Eclipse JGit - Java实现的Git
- **Apache SSHD 2.11.0**: SSH服务器实现
- **Eclipse Jetty 11.0.18**: HTTP服务器
- **Maven**: 项目构建工具

## 快速开始

### 编译项目

```bash
mvn clean package
```

编译后会在`target`目录生成`gitserver-1.0.0.jar`文件。

### 启动服务器

使用默认配置启动（HTTP端口8080，SSH端口2222）：

```bash
java -jar target/gitserver-1.0.0.jar
```

使用自定义配置启动：

```bash
java -jar target/gitserver-1.0.0.jar --http-port 9090 --ssh-port 2223 --repo-dir /path/to/repos
```

### 命令行参数

- `--http-port <port>`: HTTP服务器端口（默认：8080）
- `--ssh-port <port>`: SSH服务器端口（默认：2222）
- `--repo-dir <path>`: Git仓库存储目录（默认：repositories）
- `--help`: 显示帮助信息

## 使用方法

### HTTP协议

#### 克隆仓库

```bash
git clone http://localhost:8080/myrepo.git
```

#### 推送到仓库

```bash
cd myrepo
git remote add origin http://localhost:8080/myrepo.git
git push -u origin master
```

### SSH协议

#### 克隆仓库

```bash
git clone ssh://git@localhost:2222/myrepo.git
```

#### 推送到仓库

```bash
cd myrepo
git remote add origin ssh://git@localhost:2222/myrepo.git
git push -u origin master
```

**注意**: 当前SSH认证配置为接受所有用户，用于演示目的。在生产环境中应实现适当的身份验证机制。

## 项目结构

```
GitServer/
├── pom.xml                                    # Maven项目配置
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── gitserver/
│       │           ├── GitServerApplication.java  # 主应用程序
│       │           ├── GitHttpServer.java         # HTTP服务器实现
│       │           └── GitSshServer.java          # SSH服务器实现
│       └── resources/
│           └── simplelogger.properties        # 日志配置
└── repositories/                              # Git仓库存储目录（自动创建）
```

## 开发环境要求

- Java 11 或更高版本
- Maven 3.6 或更高版本

## 构建和运行

### 编译

```bash
mvn clean compile
```

### 打包

```bash
mvn clean package
```

### 运行

```bash
java -jar target/gitserver-1.0.0.jar
```

## 日志配置

日志配置文件位于`src/main/resources/simplelogger.properties`，可以根据需要调整日志级别。

## 安全说明

⚠️ **重要**: 当前实现是一个演示版本，SSH认证配置为接受所有连接。在生产环境中使用前，请务必实现以下安全措施：

1. 配置适当的SSH密钥认证
2. 实现用户权限管理
3. 添加访问控制列表（ACL）
4. 启用HTTPS（而非HTTP）
5. 实施审计日志

## 许可证

本项目基于Apache License 2.0许可证开源。

## 贡献

欢迎提交Issue和Pull Request！

## 联系方式

如有问题或建议，请在GitHub上提交Issue。