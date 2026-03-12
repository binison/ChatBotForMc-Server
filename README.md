# ChatBotForMc

> 为 Paper 服务器打造的 AI 聊天插件。支持 OpenAI-compatible 与阿里百炼（DashScope 兼容模式），让玩家直接在游戏内通过 `@ai`、`@ai:`、`@米糯` 与大模型对话，而 `/ai` 则保留为管理命令入口。

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-blue" alt="Java 21" />
  <img src="https://img.shields.io/badge/Paper-1.21.x-brightgreen" alt="Paper 1.21.x" />
  <img src="https://img.shields.io/badge/Maven-Build-C71A36" alt="Maven" />
  <img src="https://img.shields.io/badge/Version-1.2-success" alt="Version 1.2" />
  <img src="https://img.shields.io/badge/License-MIT-yellow" alt="MIT License" />
</p>

## ✨ Why ChatBotForMc

- **真正开箱即用**：安装、填入 key、执行 `/ai reload` 后，玩家就能直接用 `@ai` 聊天
- **服内直接对话**：支持在聊天中使用 `@ai <message>`、`@ai: <message>`、`@米糯 <message>`
- **流式输出**：回复可边生成边显示，等待感更低
- **上下文记忆**：玩家独立会话，不串线
- **管理员统一提示词**：快速定制服务器世界观、规则和人设
- **事件触发 AI**：可在首次进服、死亡、达成进度时自动触发 AI，并支持按配置选择私发或全服广播
- **异步请求**：不阻塞主线程，适合线上服
- **限流保护**：cooldown + busy，避免滥用
- **兼容面广**：OpenAI-compatible / 阿里百炼都可接入
- **默认中文人设友好**：粉色前缀 `米糯`

---

## 🌟 Highlights

### For Server Owners
- 快速给服务器接入 AI 助手
- 用统一提示词固定人设、规则和答复风格
- 支持服内答疑、玩法说明、新手引导
- 支持通过事件自动触发欢迎、安慰和进度反馈，并可配置广播或私发

### For Players
- 直接在游戏内用 `@ai 你好`、`@ai: 你好` 或 `@米糯 你好` 提问，更接近日常聊天
- `/ai` 不再作为普通聊天入口，而是保留给管理功能
- 支持上下文对话，不是一次性问答
- 支持流式回复，等待体验更自然
- 默认回复前缀清晰，辨识度高
- 普通 `@ai` / `/ai` 回复默认仅自己可见，事件触发回复可配置为全服可见或仅自己可见

### For Developers
- 结构清晰：命令、配置、服务、监听器、LLM 客户端分层明确
- 使用标准 Java `HttpClient` + Jackson
- 基于 OpenAI-compatible 协议，便于扩展更多服务商
- 事件触发走配置驱动，便于继续扩展更多 Bukkit 事件

---

## 🎮 Preview

![img_2.png](img_2.png)

---

## 🚀 1 Minute Setup

### 1. 构建插件
```powershell
mvn clean package
```

生成产物通常为：

```text
target/ChatBotForMc-1.2.jar
```

### 2. 放入服务器
把 jar 放到：

```text
plugins/
```

### 3. 配置 API
编辑运行目录：

```text
plugins/ChatBotForMc/config.yml
```

最小可用配置：

```yaml
enabled: true

api:
  provider: "bailian"
  base-url: "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
  api-key: "你的Key"
  auth-header: "Authorization"
  auth-prefix: "Bearer "
  model: "qwen-plus"
  stream-enabled: true

chat:
  reply-prefix: "&d[米糯]&r "
  stream-flush-chars: 24
  stream-flush-interval-ms: 400

context:
  system-prompt: "You are a helpful Minecraft server assistant."
```

### 4. 启动使用
```text
/ai reload
@ai 你好
@ai: 来讲个笑话
@米糯 现在该干什么
```

---

## 🎯 Core Commands

```text
@ai <message>                  在聊天中直接向 AI 发送消息（推荐）
@ai: <message>                 聊天中使用冒号形式触发 AI
@米糯 <message>                中文别名触发 AI
/ai reload                     重载配置
/ai reset [player]             清空会话上下文
/ai prompt view                查看当前系统提示词
/ai prompt set <content>       设置并保存系统提示词
/ai prompt reset               重置系统提示词
```

说明：
- 普通玩家聊天请使用 `@ai` / `@ai:` / `@米糯`
- `/ai` 当前仅用于管理与维护，不再作为常规聊天入口

### Permissions

```text
chatbot.use
chatbot.admin
chatbot.bypass.ratelimit
```

### Chat Mention Config

```yaml
chat-mention:
  enabled: true
  prefixes:
    - "@ai"
    - "@米糯"
  cancel-original-message: true
```

说明：
- `chat-mention.enabled`：是否启用聊天中的 mention 触发
- `chat-mention.prefixes`：可配置多个触发词，例如 `@ai`、`@米糯`、`@bot`
- 当前同时支持 `@ai 内容` 与 `@ai: 内容` 两种格式
- `chat-mention.cancel-original-message`：触发后是否拦截玩家原始聊天消息，默认 `true`

---

## ⚙️ Key Config

```yaml
api:
  provider: "bailian"
  base-url: "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
  api-key: ""
  model: "qwen-plus"
  timeout-ms: 20000
  max-tokens: 0
  temperature: 0.7
  stream-enabled: true

chat:
  reply-prefix: "&d[米糯]&r "
  max-input-length: 300
  max-output-length: 0
  stream-flush-chars: 24
  stream-flush-interval-ms: 400

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

### 配置说明
- `api.max-tokens: 0`：不主动限制上游输出 token
- `api.stream-enabled`：是否优先尝试流式输出
- `chat.max-output-length: 0`：插件侧不截断输出
- `chat.stream-flush-chars`：流式输出累计多少字符后优先刷出一段
- `chat.stream-flush-interval-ms`：流式输出两次刷新的最小间隔
- `reply-prefix`：默认使用粉色 `米糯`
- `context.system-prompt`：全局系统提示词
- `context.max-prompt-length`：管理员设置提示词的最大长度

---

## ⚡ Streaming Output

当前版本支持 **OpenAI-compatible SSE 流式输出**。

特点：
- 回复会按片段逐步显示，而不是等整段生成完再发
- 适合降低长回答的等待感
- 会自动按照字符数 / 时间窗口分批发送，避免过度刷屏

相关配置：

```yaml
api:
  stream-enabled: true

chat:
  stream-flush-chars: 24
  stream-flush-interval-ms: 400
```

如果你的上游平台对流式兼容较差，可以先把：

```yaml
api:
  stream-enabled: false
```

改为非流式模式验证基础连通性。

---

## 🎬 Event Triggers

当前版本支持通过事件自动触发 AI 回复。默认关闭，需要手动启用。

### 当前支持的事件
- `join`：玩家进服时触发，默认配置下每次进入服务器都可触发
- `death`：玩家死亡时触发，可在 prompt 中引用死亡坐标
- `advancement`：玩家完成进度时触发

### 推荐用途
- 新手欢迎
- 回归欢迎
- 死亡安慰 / 小提示
- 进度达成后的下一步建议

### 可见性说明
- 普通 `/ai` 聊天回复：默认仅触发玩家本人可见
- 事件触发 AI 回复：通过 `event-triggers.broadcast` 配置控制
  - `true`：广播给全服在线玩家
  - `false`：仅触发玩家本人可见

### 示例配置

```yaml
event-triggers:
  enabled: true
  broadcast: true
  join:
    enabled: true
    first-join-only: false
    cooldown-seconds: 300
    prompt: "A player named %player% has joined the server. Give a short in-character welcome and one helpful server tip."
  death:
    enabled: true
    cooldown-seconds: 180
    prompt: "Player %player% died in Minecraft. Cause: %death_message%. Death location: %death_world% %death_x% %death_y% %death_z%. Offer a short comforting reaction and one practical suggestion."
  advancement:
    enabled: true
    cooldown-seconds: 180
    prompt: "Player %player% has just completed the advancement '%advancement%'. React briefly and suggest a natural next goal."
    ignore-prefixes:
      - "minecraft:recipes/"
```

### 可用模板变量
- `%player%`：玩家名
- `%death_message%`：死亡消息
- `%death_x%` / `%death_y%` / `%death_z%`：死亡坐标
- `%death_world%`：死亡所在世界名
- `%death_location%`：组合后的死亡位置文本
- `%advancement%`：进度 key

### 注意事项
- `event-triggers.broadcast: true` 时，事件触发回复会广播给全服所有在线玩家
- `event-triggers.broadcast: false` 时，事件触发回复仅触发玩家可见
- `event-triggers.join.first-join-only: false` 时，每次玩家进入服务器都可以触发欢迎
- 如果你只想保留首次进入欢迎，把它改回 `true`
- 建议先只开启 1~2 个低频事件观察效果
- `advancement.ignore-prefixes` 推荐保留 `minecraft:recipes/`，否则配方类进度会非常频繁
- 如果你启用了流式输出，事件回复也会按片段逐步显示
- 修改配置后请执行 `/ai reload`，当前版本会立即应用新的事件触发配置

---

## 🧠 Prompt Management

当前版本采用 **管理员统一提示词** 设计，适合：

- 服务器规则说明
- 世界观设定
- 新手引导
- 固定角色人设

### 示例
```text
/ai prompt set 你是本服务器的AI助手，请优先回答Minecraft玩法、服务器规则和本服相关内容。
```

系统提示词会：
- 写回 `plugins/ChatBotForMc/config.yml`
- 立即 reload
- 对后续 AI 请求与事件触发都生效

---

## 🛠 FAQ

### AI 返回 unavailable
把 `debug` 改成 `true`，然后执行：

```text
/ai reload
```

再重试并查看服务端日志。

### 返回 401
通常表示：
- API Key 错误
- 接口地址错误
- 模型无权限
- 服务协议不兼容

如果你接的是阿里百炼，优先检查：
- `base-url` 是否为兼容模式地址
- `Authorization: Bearer <Key>` 是否正确
- 模型是否可用

### 流式输出没有正常工作怎么办？
优先检查：
- 当前服务商是否兼容 OpenAI SSE 流式输出
- `api.stream-enabled` 是否开启
- 服务端日志里是否有解析异常

如果怀疑是上游流式兼容问题，可先切换到：

```yaml
api:
  stream-enabled: false
```

确认普通非流式请求能否工作。

### @ai 没有触发 AI 怎么办？
请确认：
- `chat-mention.enabled: true`
- 触发格式是否为消息开头的 `@ai 内容`、`@ai: 内容`、`@米糯 内容`
- `chat-mention.prefixes` 中是否包含你使用的前缀
- 已执行 `/ai reload`
- 玩家拥有 `chatbot.use` 权限

如果你希望保留原始聊天消息不被拦截，可以设置：

```yaml
chat-mention:
  cancel-original-message: false
```

### 为什么 /ai 不能直接聊天了？
当前版本把产品形态收敛成了：
- 玩家聊天统一走 `@ai` 这种更自然的入口
- `/ai` 专注于 `reload`、`reset`、`prompt` 这类管理功能

这样玩家体验和管理员操作会更清晰，也更贴近正常聊天习惯。

---

## 🗺 Roadmap

- [ ] 支持环境变量读取 API Key
- [ ] 多服务商工厂模式
- [ ] 支持 Anthropic / Gemini 等非 OpenAI 协议
- [x] 支持流式输出
- [x] 支持事件触发 AI
- [x] 让事件配置在 reload 后即时刷新
- [ ] 增加更多自动化测试
- [ ] 支持 `/ai stop`
