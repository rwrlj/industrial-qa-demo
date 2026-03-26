# 工业设备故障诊断系统

基于 Spring Boot + 通义千问 API 的工业设备故障诊断问答系统。

## 功能特性

- 🔍 **智能故障诊断**：基于通义千问大模型，提供专业的设备故障诊断
- 📚 **知识库检索**：内置工业设备故障知识库，支持 RAG（检索增强生成）
- 📊 **多设备支持**：支持数控机床、PLC控制器、伺服系统、传感器、工业机器人等
- 📝 **完整日志记录**：详细的业务日志、API调用日志、错误日志
- 🎨 **美观的Web界面**：现代化响应式界面，支持移动端访问
- ⚡ **高性能异步处理**：异步日志记录，不阻塞主业务

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.2.5 | 应用框架 |
| Java | 22 | 运行环境 |
| Thymeleaf | - | 模板引擎 |
| DashScope SDK | 2.12.0 | 通义千问调用 |
| Lombok | 1.18.32 | 代码简化 |
| Logback | - | 日志框架 |

## 快速开始

### 1. 环境要求

- JDK 17 或更高版本
- Maven 3.6+
- 阿里云账号（获取通义千问 API Key）

### 2. 获取 API Key

1. 访问 [阿里云百炼平台](https://bailian.console.aliyun.com/)
2. 开通通义千问服务
3. 创建 API Key（格式：`sk-xxx`）

### 3. 配置 API Key

方式一：复制配置文件模板

```bash
cp src/main/resources/application-example.yml src/main/resources/application.yml