# ChatBotForMc

> 一个面向 Paper 服务器的 Minecraft AI 聊天插件，支持 OpenAI-compatible 与阿里百炼（DashScope 兼容模式）接入。

![Java](https://img.shields.io/badge/Java-21-blue)
![Paper](https://img.shields.io/badge/Paper-1.21.x-brightgreen)
![Maven](https://img.shields.io/badge/Build-Maven-C71A36)
![License](https://img.shields.io/badge/License-MIT-yellow)

## ✨ 项目简介
`ChatBotForMc` 是一个 Paper 服务端插件，目标是让玩家直接在游戏内通过 `/ai` 命令与大语言模型对话。

适合场景：
- 生存服 / RPG 服中的 AI 助手、服务器百科、答疑入口
- 管理员快速搭建一个可用的服内 LLM 对话入口
- 希望接入 OpenAI-compatible 或阿里百炼模型的 Paper 插件项目

当前版本聚焦于：
- `/ai <message>` 命令式 AI 对话
- 多轮上下文记忆
- 限流与 busy 保护
- 异步 HTTP 调用，不阻塞主线程
- 管理员统一配置系统提示词
- OpenAI-compatible / 阿里百炼兼容模式接入

---

## 🚀 功能亮点
- `/ai <message>` 直接发起 AI 对话
- `/ai reload` 热重载配置并重建底层客户端
- `/ai reset [player]` 清空会话上下文
- `/ai prompt view|set|reset` 统一管理全局系统提示词
- 玩家独立会话，不串线
- 基础冷却控制，避免滥用
- 请求异步执行，不阻塞主线程
- 支持 OpenAI Chat Completions 兼容协议
- 已适配阿里百炼（DashScope 兼容模式）
- debug 模式下输出更清晰的异常日志

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
├─ ChatBotPlugin.java        # 插件入口 / 配置重载 / 全局提示词写回
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
│  ├─ RateLimitService.java  # cooldown / busy 控制
│  └─ SessionManager.java    # 会话上下文
└─ util/
   └─ MessageFormatter.java
```

---

## ⚙️ 运行环境
- Java 21
- Paper 1.21.x
- Maven 3.9+

> 当前 `pom.xml` 使用 `maven.compiler.release=21`，并依赖 Paper API `1.21.1-R0.1-SNAPSHOT`。

---

## 🔌 支持的模型服务
当前插件使用 **OpenAI Chat Completions 兼容协议**。

目标服务最好兼容：
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
debug: false

api:
  provider: "bailian"
  base-url: "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
  api-key: "你的百炼Key"
  auth-header: "Authorization"
  auth-prefix: "Bearer "
  model: "qwen-plus"
  timeout-ms: 20000
  max-tokens: 0
  temperature: 0.7

chat:
  reply-prefix: "&d[米糯]&r "
  max-input-length: 300
  max-output-length: 0

context:
  enabled: true
  max-rounds: 6
  expire-minutes: 30
  system-prompt: "You are a helpful Minecraft server assistant. Keep replies concise and friendly."
  max-prompt-length: 2000

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
- `api.max-tokens`：最大输出 token 数，`0` 表示不主动传 `max_tokens`
- `api.temperature`：采样温度
- `chat.max-output-length`：最大输出字符数，`0` 表示插件侧不截断；当前默认整段发送，不再按两段/多段拆分
- `context.system-prompt`：全局系统提示词
- `context.max-prompt-length`：管理员设置提示词时的最大长度
- `rate-limit.*`：限流相关配置

---

## 🎮 命令与权限
### 命令
- `/ai <message>`：向 AI 发送消息
- `/ai reload`：重载配置
- `/ai reset [player]`：清空会话上下文
- `/ai prompt view`：查看当前全局系统提示词
- `/ai prompt set <content>`：设置并保存全局系统提示词
- `/ai prompt reset`：恢复默认系统提示词并保存

### 权限
- `chatbot.use`：允许使用聊天命令，默认 `true`
- `chatbot.admin`：允许执行管理命令与系统提示词管理，默认 `op`
- `chatbot.bypass.ratelimit`：绕过限流，默认 `op`

---

## 🧠 管理员统一提示词
当前版本使用 **管理员统一配置的全局系统提示词**，而不是“每个玩家独立提示词”。

### 查看当前提示词
```text
/ai prompt view
```

### 设置新的提示词
```text
/ai prompt set 你是本服务器的AI助手，请优先回答Minecraft玩法、服务器规则和本服相关内容。
```

### 重置为默认提示词
```text
/ai prompt reset
```

### 生效方式
- `set` / `reset` 会直接写回 `plugins/ChatBotForMc/config.yml`
- 写回后会立即 reload
- 后续新的 AI 请求会使用新的系统提示词

---

## 📦 构建与部署
### 1. 本地打包
```powershell
mvn clean package
```

成功后通常使用：

```text
target/ChatBotForMc-1.1.jar
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

## ✅ 快速测试清单
### 基础测试
```text
/ai 你好
/ai reload
/ai reset
```

### 提示词测试
```text
/ai prompt view
/ai prompt set 你是本服AI助手，请优先回答Minecraft和本服规则问题。
/ai prompt view
/ai prompt reset
```

### 上下文测试
```text
/ai 我叫 Steve
/ai 我刚才叫什么？
```

### 冷却 / busy 测试
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

### 5. 管理员设置提示词后看起来没变
请确认：
- 执行的是 `/ai prompt set <内容>`
- 执行者拥有 `chatbot.admin`
- 看的是真实运行目录下的 `plugins/ChatBotForMc/config.yml`
- 提示词长度没有超过 `context.max-prompt-length`

---

## 🔒 安全建议
- **不要把真实 API Key 提交到 Git 仓库**
- 如果 key 已经暴露，请立刻去服务商后台轮换
- 对公开服务器建议启用权限管理和限流
- 建议未来改成环境变量读取 API Key
- 你当前本地 `config.yml` 如果已经写入真实 key，只应保留在服务器运行目录，不要直接推送到公共仓库

---

## 🧭 Roadmap
- [ ] 支持环境变量读取 API Key
- [ ] 增加多服务商工厂模式
- [ ] 支持 Anthropic / Gemini 等非 OpenAI 协议
- [ ] 支持流式输出（stream）
- [ ] 支持函数调用 / tools
- [ ] 支持聊天监听模式（如 `@bot`）
- [ ] 为命令与 HTTP 客户端补更多自动化测试

---

## 🤝 贡献
欢迎提交 Issue 和 Pull Request。

如果你准备继续扩展这个项目，建议优先从以下方向入手：
1. 把 provider 逻辑拆成工厂模式
2. 为 `ChatService` / `HttpLlmClient` / `AiCommand` 增加更多自动化测试
3. 支持环境变量读取敏感配置
4. 进一步收紧会话与生命周期语义

