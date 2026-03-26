# 工业设备故障诊断系统

基于 Spring Boot + 通义千问 API 的工业设备故障诊断问答系统。

## 功能特性

- 🔍 **智能故障诊断**：基于通义千问大模型，提供专业的设备故障诊断建议
- 📚 **知识库检索**：内置工业设备故障知识库，支持故障现象匹配
- 🎯 **RAG 架构**：检索增强生成，让 AI 回答更准确
- 📊 **完整日志**：支持业务日志、API 调用日志、错误日志分离
- 💻 **Web 界面**：简洁美观的对话界面

## 技术栈

- Java 17/22
- Spring Boot 3.2.x
- 通义千问 API (DashScope SDK)
- Thymeleaf
- Maven

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