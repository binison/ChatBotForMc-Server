# ChatBotForMc

一个基于 Paper 的 Minecraft 插件，让玩家通过 `/ai` 命令在游戏内和大语言模型聊天。

## 功能概览
- `/ai <message>` 发送提问并获取 AI 回复
- `/ai reload` 重载插件配置
- `/ai reset [player]` 清空会话上下文
- 每位玩家独立上下文会话
- 基础冷却与进行中请求保护
- 异步 HTTP 调用，不阻塞主线程
- 支持 OpenAI Chat Completions 兼容协议
- 支持通过配置切换不同服务商
- 当前已验证接入方向：OpenAI-compatible / 阿里百炼（DashScope 兼容模式）

## 项目结构
- 主插件入口：`src/main/java/com/binison/chatbot/ChatBotPlugin.java`
- 命令处理：`src/main/java/com/binison/chatbot/command/AiCommand.java`
- 聊天服务：`src/main/java/com/binison/chatbot/service/ChatService.java`
- HTTP LLM 客户端：`src/main/java/com/binison/chatbot/llm/HttpLlmClient.java`
- 配置映射：`src/main/java/com/binison/chatbot/config/PluginConfig.java`
- 默认配置：`src/main/resources/config.yml`
- 插件描述：`src/main/resources/plugin.yml`

## 运行环境
- Java 21
- Paper 1.21.x
- Maven（用于本地构建）

## 当前接入协议说明
当前插件使用的是 **OpenAI Chat Completions 兼容协议**。

也就是说，目标服务需要尽量兼容：
- 请求路径：`/v1/chat/completions`
- 请求体中包含：`model`、`messages`
- 响应体中包含：`choices[0].message.content`

因此目前适合接入：
- OpenAI
- 阿里百炼 / DashScope 兼容模式
- 其他兼容 OpenAI Chat Completions 的网关或中转服务

> 注意：如果某个服务只提供 Anthropic、Gemini 或私有协议，而不兼容 `chat/completions`，当前版本不能直接使用，需要继续扩展客户端适配。

## 配置项说明
运行时实际读取的是服务器目录下的：

```text
plugins/ChatBotForMc/config.yml
```

核心配置如下：

```yaml
enabled: true
debug: true

api:
  provider: "bailian"
  base-url: "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
  api-key: "你的Key"
  auth-header: "Authorization"
  auth-prefix: "Bearer "
  model: "qwen-plus"
  timeout-ms: 20000
  max-tokens: 300
  temperature: 0.7
```

### 配置字段解释
- `enabled`：是否启用插件
- `debug`：是否输出更详细的错误日志
- `api.provider`：服务商标识，仅用于提供默认值和便于理解
- `api.base-url`：Chat Completions 接口地址
- `api.api-key`：服务商 API Key
- `api.auth-header`：认证请求头名称，默认 `Authorization`
- `api.auth-prefix`：认证前缀，默认 `Bearer `
- `api.model`：模型名称
- `api.timeout-ms`：HTTP 请求超时时间（毫秒）
- `api.max-tokens`：最大输出 token 数
- `api.temperature`：采样温度
- `chat.reply-prefix`：回复消息前缀
- `chat.max-input-length`：单次输入最大长度
- `chat.max-output-length`：单次输出最大长度
- `context.enabled`：是否启用上下文
- `context.max-rounds`：最多保留多少轮上下文
- `context.expire-minutes`：上下文过期时间（分钟）
- `context.system-prompt`：系统提示词
- `rate-limit.enabled`：是否启用限流
- `rate-limit.cooldown-seconds`：冷却时间（秒）

## 配置示例

### 1. OpenAI-compatible 示例
```yaml
api:
  provider: "openai-compatible"
  base-url: "https://api.openai.com/v1/chat/completions"
  api-key: "你的OpenAIKey"
  auth-header: "Authorization"
  auth-prefix: "Bearer "
  model: "gpt-4o-mini"
```

### 2. 阿里百炼（推荐）
```yaml
api:
  provider: "bailian"
  base-url: "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
  api-key: "你的百炼Key"
  auth-header: "Authorization"
  auth-prefix: "Bearer "
  model: "qwen-plus"
```

百炼常见模型示例：
- `qwen-turbo`
- `qwen-plus`
- `qwen-max`

> 具体可用模型请以阿里云百炼 / DashScope 控制台的最新文档为准。

### 3. 自定义 Header 的第三方服务
```yaml
api:
  provider: "openai-compatible"
  base-url: "https://example.com/v1/chat/completions"
  api-key: "你的Key"
  auth-header: "X-API-Key"
  auth-prefix: ""
  model: "provider-model-name"
```

## 构建与打包
在项目根目录执行：

```powershell
mvn clean package
```

构建完成后，通常使用下面这个产物部署：

```text
target/ChatBotForMc-1.0-SNAPSHOT.jar
```

> 如果 `target` 目录中同时存在 `original-*.jar`，不要部署那个；优先使用最终生成的插件 jar。

## 部署步骤
1. 停止 Paper 服务器
2. 删除旧的 `ChatBotForMc` 插件 jar
3. 将新构建的 jar 放入服务器 `plugins` 目录
4. 启动服务器
5. 编辑 `plugins/ChatBotForMc/config.yml`
6. 根据你的服务商填写 API 配置
7. 使用 `/ai reload` 应用新配置，或直接重启服务器

## 命令说明
### `/ai <message>`
向 AI 发送消息。

示例：

```text
/ai 今天天气怎么样？
```

### `/ai reload`
重载插件配置，并重建底层 HTTP 客户端。

适用于以下场景：
- 修改了 `api.base-url`
- 修改了 `api.api-key`
- 修改了 `api.model`
- 切换了不同服务商

### `/ai reset [player]`
清空自己的会话上下文，或管理员清空指定玩家上下文。

## 权限节点
来自 `plugin.yml` 的当前权限设计：

- `chatbot.use`：允许使用聊天命令，默认 `true`
- `chatbot.admin`：允许执行管理命令，默认 `op`
- `chatbot.bypass.ratelimit`：绕过限流，默认 `op`

## 快速测试清单
插件部署完成后，建议按下面顺序做冒烟测试。

### 1. 插件加载
确认控制台没有报错，并看到插件启用日志。

### 2. 基础问答
```text
/ai 你好
```

### 3. 重载配置
```text
/ai reload
```

### 4. 重置上下文
```text
/ai reset
```

### 5. 冷却 / busy 测试
连续快速发送两次：

```text
/ai 你好
```

期望第二次收到冷却或 busy 提示。

### 6. 上下文测试
```text
/ai 我叫 Steve
/ai 我刚才叫什么？
```

## 常见问题排查

### 1. 玩家看到 `AI is currently unavailable`
这只是统一兜底提示，不是根因。

排查方法：
1. 打开调试
2. 执行 `/ai reload`
3. 再次提问
4. 查看服务端日志

建议配置：

```yaml
debug: true
```

当前日志会输出：
- HTTP 状态码
- 部分响应 body
- 详细堆栈（debug 开启时）

### 2. 返回 401
通常表示认证失败，常见原因：
- API Key 错误
- 服务地址不对
- 模型无权限
- 使用了错误的接口协议

如果你接的是阿里百炼，建议优先确认：
- 地址是否为 `https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions`
- 模型是否为 `qwen-plus` / `qwen-turbo` / `qwen-max`
- 认证是否为 `Authorization: Bearer <Key>`

### 3. 修改了配置但没有生效
请确认修改的是运行时配置：

```text
plugins/ChatBotForMc/config.yml
```

而不是源码目录下的：

```text
src/main/resources/config.yml
```

修改后执行：

```text
/ai reload
```

### 4. 服务端启动时报缺少依赖类
如果你看到类似 `NoClassDefFoundError`，通常说明部署的不是正确打包产物，或者部署了旧 jar。

请重新执行：

```powershell
mvn clean package
```

然后重新替换服务器里的插件 jar。

## 安全建议
- **不要把真实 API Key 提交到 Git 仓库**
- 建议将 `config.yml` 里的 key 替换为你自己的运行时配置
- 如果 key 已经在聊天、截图或仓库历史中暴露，请立即去服务商后台轮换
- 对公开服务器建议额外配置权限和限流策略

## 已知限制
- 当前仅支持 Chat Completions 兼容协议
- 当前默认解析 `choices[0].message.content`
- 暂不支持流式输出（stream）
- 暂不支持函数调用 / tools
- 暂不支持各服务商私有扩展参数
- 当前主要是命令式交互，不支持 `@bot` 聊天监听模式

## 后续可扩展方向
- 增加多服务商工厂模式
- 支持 Anthropic / Gemini 等非 OpenAI 协议
- 支持流式输出
- 支持环境变量读取 API Key
- 支持聊天监听模式和频道隔离
