# ChatBotForMc

Paper 服务器的 AI 聊天插件：玩家用 `@ai` / `@米糯` 直接聊天；`/ai` 仅用于管理（reload/reset/prompt）。

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-blue" alt="Java 21" />
  <img src="https://img.shields.io/badge/Paper-1.21.x-brightgreen" alt="Paper 1.21.x" />
  <img src="https://img.shields.io/badge/Maven-Build-C71A36" alt="Maven" />
  <img src="https://img.shields.io/badge/License-MIT-yellow" alt="MIT License" />
</p>

## 功能

- 聊天 mention 触发：`@ai <内容>` / `@ai: <内容>` / `@米糯 <内容>`
- 流式输出（SSE）：边生成边显示
- 上下文记忆：玩家独立会话
- 事件触发（可选）：join / death / advancement
- 限流保护：busy（同一玩家并发）+ cooldown（配置项）
- 指令执行（可选）：**白名单 +（可选）确认按钮**，仅私聊路径生效

## 预览

![img_2.png](img_2.png)

## 快速开始（1 分钟）

### 1) 构建

```powershell
mvn clean package
```

生成：`target/ChatBotForMc-1.3.jar`

### 2) 安装

把 jar 放到 `plugins/`，启动一次生成配置：

- `plugins/ChatBotForMc/config.yml`

### 3) 配置 API

编辑 `plugins/ChatBotForMc/config.yml`，填写：

- `api.base-url`
- `api.api-key`
- `api.model`

完成后执行：

```text
/ai reload
```

## 使用方式

### 玩家聊天（推荐）

```text
@ai 你好
@ai: 来讲个笑话
@米糯 现在该干什么
```

### 管理命令（/ai）

```text
/ai reload
/ai reset [player]
/ai prompt view
/ai prompt set <content>
/ai prompt reset
```

> 说明：`/ai confirm <token>` 是内部确认命令（由按钮触发），通常不需要手动输入。

## 权限

```text
chatbot.use
chatbot.admin
chatbot.bypass.ratelimit
```

## 配置说明（核心）

> 提示：`chat.max-output-length: 0` 表示不在插件侧截断输出。

```yaml
api:
  provider: "openai-compatible"
  base-url: "https://api.openai.com/v1/chat/completions"
  api-key: ""
  auth-header: "Authorization"
  auth-prefix: "Bearer "
  model: "gpt-4o-mini"
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

chat-mention:
  enabled: true
  prefixes: ["@ai", "@米糯"]
  cancel-original-message: true

context:
  enabled: true
  max-rounds: 6
  expire-minutes: 30
  system-prompt: "You are a helpful Minecraft server assistant."
  max-prompt-length: 2000

rate-limit:
  enabled: true
  cooldown-seconds: 5
```

## 可选功能：指令执行（command-execution）

当开启 `command-execution.enabled: true` 时，AI 可能输出 `{{EXECUTE:/xxx}}` 标签。

行为规则：
- **仅私聊路径生效**：玩家自己触发的 `@ai` / `/ai` 才会处理；事件触发（join/death/advancement）不会执行命令
- 必须命中 `allowed-commands` 白名单
- 可选确认模式：`require-confirm: true` 时，玩家只会收到一个 **`[确定]` 按钮**（不展示命令文本，也不会额外回显“已执行”）

示例配置：

```yaml
command-execution:
  enabled: false
  require-confirm: true
  max-commands-per-response: 1
  max-command-length: 200
  allowed-commands:
    - "spawn"
    - "home"
    - "tpa"
    - "time set day"
    - "weather clear"
```

安全建议：
- 强烈建议保持 `require-confirm: true`
- 白名单务必保守，只允许低风险命令
- 不要把权限/管理类指令加入白名单（例如 `op`、权限插件、`stop/restart` 等）

## 事件触发（event-triggers）

- `event-triggers.enabled: true` 后才会生效
- `event-triggers.broadcast` 决定是私发还是全服广播
- 支持 `join` / `death` / `advancement`，每个事件支持独立 cooldown

示例：

```yaml
event-triggers:
  enabled: true
  broadcast: true

  join:
    enabled: true
    first-join-only: false
    cooldown-seconds: 300
    prompt: "玩家%player%进入服务器。简短欢迎并给一条建议。"

  death:
    enabled: true
    cooldown-seconds: 180
    prompt: "玩家%player%死亡，死因：%death_message%。先安慰再给建议。"

  advancement:
    enabled: true
    cooldown-seconds: 180
    prompt: "玩家%player%完成进度%advancement%。夸奖并推荐下一目标。"
    ignore-prefixes:
      - "minecraft:recipes/"
```

可用变量：
- `%player%`
- `%death_message%` `%death_world%` `%death_x%` `%death_y%` `%death_z%`
- `%advancement%`

## FAQ

### AI 返回 unavailable / 失败
- 开启 `debug: true`
- `/ai reload` 后重试，看控制台日志

### 返回 401
常见原因：API Key 错误、base-url 不对、模型无权限、上游不兼容。

### 流式输出异常
把 `api.stream-enabled: false` 暂时关掉，先验证非流式链路。

### @ai 没触发
检查：
- `chat-mention.enabled: true`
- 触发词在消息开头：`@ai 内容` / `@ai: 内容`
- `chatbot.use` 权限
- 配置变更后已 `/ai reload`

## License

MIT
