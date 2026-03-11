# ChatBotForMc

> 一个面向 Paper 服务器的 Minecraft AI 聊天插件，支持 OpenAI-compatible 与阿里百炼（DashScope 兼容模式）接入。

![Java](https://img.shields.io/badge/Java-21-blue)
![Paper](https://img.shields.io/badge/Paper-1.21.x-brightgreen)
![Maven](https://img.shields.io/badge/Build-Maven-C71A36)
![License](https://img.shields.io/badge/License-MIT-yellow)

## ✨ 项目简介
`ChatBotForMc` 是一个 Minecraft 服务端插件，目标是让玩家直接在游戏内通过 `/ai` 命令与大语言模型对话。

它适合以下场景：
- 生存服 / RPG 服中的 AI 助手、答疑 NPC、服务器百科
- 管理员快速搭建一个可用的服内 LLM 对话入口
- 想接入 OpenAI-compatible 或阿里百炼模型的 Paper 插件项目

当前版本聚焦于：
- 命令式 AI 对话
- 多轮上下文记忆
- 限流与 busy 保护
- 异步 HTTP 调用
- 可切换不同 LLM 服务商

---

## 🚀 功能亮点
- `/ai <message>` 直接发起 AI 对话
- `/ai reload` 热重载配置并重建底层客户端
- `/ai reset [player]` 清空会话上下文
- 玩家独立会话，不串线
- 基础冷却控制，避免滥用
- 请求异步执行，不阻塞主线程
- 支持 OpenAI Chat Completions 兼容协议
- 已适配阿里百炼（DashScope 兼容模式）
- 调试模式下输出更清晰的 HTTP 错误与响应片段

---

## 🧱 技术栈
- Java 21
- Paper API 1.21.x
- Maven
- Java `HttpClient`
- Jackson JSON

---

## 🗂 项目结构
```text
src/main/java/com/binison/chatbot/
├─ ChatBotPlugin.java        # 插件入口
├─ command/
│  └─ AiCommand.java         # /ai 命令
├─ config/
│  └─ PluginConfig.java      # 配置映射
├─ llm/
│  ├─ LlmClient.java         # LLM 抽象
│  └─ HttpLlmClient.java     # HTTP Chat Completions 客户端
├─ model/
│  ├─ ChatMessage.java
│  └─ LlmResponse.java
├─ service/
│  ├─ ChatService.java       # 主业务逻辑
│  ├─ RateLimitService.java
│  └─ SessionManager.java
└─ util/
   └─ MessageFormatter.java
```

---

## ⚙️ 运行环境
- Java 21
- Paper 1.21.x
- Maven 3.9+

---

## 🔌 支持的模型服务
当前插件使用的是 **OpenAI Chat Completions 兼容协议**。

也就是说，目标服务最好兼容：
- 请求地址：`/v1/chat/completions`
- 请求字段：`model`、`messages`
- 响应字段：`choices[0].message.content`

### 当前适合接入的服务
- OpenAI
- 阿里百炼 / DashScope 兼容模式
- 其他 OpenAI-compatible 网关或中转服务

> 如果某个服务只提供 Anthropic / Gemini / 私有协议接口，而不兼容 Chat Completions，当前版本不能直接接入，需要继续扩展客户端。

---

## 🧩 配置说明
插件运行时读取的是：

```text
plugins/ChatBotForMc/config.yml
```

### 推荐的阿里百炼配置
```yaml
enabled: true
debug: true

api:
  provider: "bailian"
  base-url: "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
  api-key: "你的百炼Key"
  auth-header: "Authorization"
  auth-prefix: "Bearer "
  model: "qwen-plus"
  timeout-ms: 20000
  max-tokens: 300
  temperature: 0.7

chat:
  reply-prefix: "&b[AI]&r "
  max-input-length: 300
  max-output-length: 1200

context:
  enabled: true
  max-rounds: 6
  expire-minutes: 30
  system-prompt: "You are a helpful Minecraft server assistant. Keep replies concise and friendly."

rate-limit:
  enabled: true
  cooldown-seconds: 5
```

### OpenAI-compatible 示例
```yaml
api:
  provider: "openai-compatible"
  base-url: "https://api.openai.com/v1/chat/completions"
  api-key: "你的OpenAIKey"
  auth-header: "Authorization"
  auth-prefix: "Bearer "
  model: "gpt-4o-mini"
```

### 自定义 Header 的第三方服务
```yaml
api:
  provider: "openai-compatible"
  base-url: "https://example.com/v1/chat/completions"
  api-key: "你的Key"
  auth-header: "X-API-Key"
  auth-prefix: ""
  model: "provider-model-name"
```

### 关键配置项
- `enabled`：是否启用插件
- `debug`：是否输出详细异常日志
- `api.provider`：服务商标识
- `api.base-url`：Chat Completions 接口地址
- `api.api-key`：API Key
- `api.auth-header`：认证请求头名称
- `api.auth-prefix`：认证前缀
- `api.model`：模型名
- `api.timeout-ms`：请求超时毫秒数
- `api.max-tokens`：最大输出 token 数
- `api.temperature`：采样温度
- `context.*`：上下文相关配置
- `rate-limit.*`：限流相关配置

---

## 📦 构建与部署
### 1. 本地打包
```powershell
mvn clean package
```

成功后通常使用：

```text
target/ChatBotForMc-1.0-SNAPSHOT.jar
```

> 如果 `target` 下同时存在 `original-*.jar`，不要部署那个。

### 2. 部署到服务器
1. 停止 Paper 服务器
2. 删除旧版 `ChatBotForMc` 插件 jar
3. 将新的 jar 放入 `plugins` 目录
4. 启动服务器
5. 编辑 `plugins/ChatBotForMc/config.yml`
6. 使用 `/ai reload`，或直接重启服务器

---

## 🎮 命令与权限
### 命令
- `/ai <message>`：向 AI 发送消息
- `/ai reload`：重载配置
- `/ai reset [player]`：清空会话上下文

### 权限
- `chatbot.use`：允许使用聊天命令，默认 `true`
- `chatbot.admin`：允许执行管理命令，默认 `op`
- `chatbot.bypass.ratelimit`：绕过限流，默认 `op`

---

## ✅ 快速测试清单
### 基础测试
```text
/ai 你好
/ai reload
/ai reset
```

### 上下文测试
```text
/ai 我叫 Steve
/ai 我刚才叫什么？
```

### 冷却测试
连续快速发送两次：

```text
/ai 你好
```

预期第二次返回 cooldown 或 busy 提示。

---

## 🛠 常见问题排查
### 1. 玩家看到 `AI is currently unavailable`
这是统一兜底提示，不是真正根因。

请这样排查：
1. 把 `debug` 改为 `true`
2. 执行 `/ai reload`
3. 再次发送消息
4. 查看服务端日志

当前日志会输出：
- HTTP 状态码
- 部分响应 body
- 详细堆栈（debug 开启时）

### 2. 返回 401
通常表示鉴权失败，常见原因：
- API Key 错误
- 接口地址错误
- 模型无权限
- 使用了错误的服务协议

如果你接的是阿里百炼，优先确认：
- 地址是否为 `https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions`
- 模型是否为 `qwen-plus` / `qwen-turbo` / `qwen-max`
- 认证是否为 `Authorization: Bearer <Key>`

### 3. 配置修改后没生效
请确认改的是：

```text
plugins/ChatBotForMc/config.yml
```

而不是：

```text
src/main/resources/config.yml
```

然后执行：

```text
/ai reload
```

### 4. 启动时报 `NoClassDefFoundError`
通常说明部署了错误的 jar 或旧产物。

重新执行：

```powershell
mvn clean package
```

再替换服务器里的插件文件。

---

## 🔒 安全建议
- 不要把真实 API Key 提交到 Git 仓库
- 如果 key 已经暴露，请立刻去服务商后台轮换
- 对公开服务器建议启用权限管理和限流
- 建议未来改成环境变量读取 API Key

---

## 🧭 Roadmap
- [ ] 支持环境变量读取 API Key
- [ ] 增加多服务商工厂模式
- [ ] 支持 Anthropic / Gemini 等非 OpenAI 协议
- [ ] 支持流式输出（stream）
- [ ] 支持函数调用 / tools
- [ ] 支持聊天监听模式（如 `@bot`）

---

## 🤝 贡献
欢迎提交 Issue 和 Pull Request。

如果你准备继续扩展这个项目，建议优先从以下方向入手：
1. 把 provider 逻辑拆成工厂模式
2. 为 `ChatService` / `HttpLlmClient` 增加更多自动化测试
3. 支持环境变量读取敏感配置

---

## 📄 License
本项目采用 MIT License。详见 `LICENSE` 文件。
