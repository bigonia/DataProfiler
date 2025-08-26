### 1\. 方案目标

将已在Dify平台完成的**AI工作流**无缝集成到`DBCrawlerV3`后端系统中，通过新增的API端点，为前端提供由“数据画像”驱动的、流式AI分析能力。

### 2\. 核心设计思路

Spring Boot后端将扮演\*\*“智能调度与服务编排者”\*\*的角色。它负责：

1.  **接收前端请求：** 处理来自用户的分析请求。
2.  **准备上下文：** 从自有数据库中，精准、高效地加载`DBCrawlerV3`已生成的“数据画像”。
3.  **安全调用AI：** 作为唯一的出口，安全地调用外部Dify工作流API。
4.  **传递结果：** 将Dify返回的AI分析结果，以流式（Streaming）的方式安全地传递给前端。

### 3\. 模块设计与开发任务

在现有的分层架构中，新增或扩展相应的组件。

#### 3.1. 配置层 (`application.properties` 或 `application.yml`)

为了系统的灵活性和安全性，首先需要将Dify的配置外部化。

```yaml
# Dify AI Service Configuration
dify:
  api:
    base-url: "https://api.dify.ai/v1" # Dify API的地址
    key: "${DIFY_API_KEY}"            # 从环境变量或配置中心读取API Key
```

**任务：** 在配置文件中添加以上配置，并准备好通过`@Value`或`@ConfigurationProperties`注解在代码中注入。

-----

#### 3.2. 服务层 (Service Layer)

这是本次开发的核心，我们将创建一个新的`AIService`和`DifyApiClient`。

**任务1：创建 `DifyApiClient` - Dify API的专业客户端**

  * **目的：** 封装所有与Dify API交互的细节，让业务代码保持整洁。

  * **实现建议：** 创建一个`@Component`或`@Service`，名为`DifyApiClient`。强烈建议使用`WebClient`（Spring WebFlux的一部分）代替`RestTemplate`，因为它原生支持非阻塞和流式处理，与Dify的流式API是绝配。

    ```java
    @Component
    public class DifyApiClient {

        private final WebClient webClient;

        public DifyApiClient(@Value("${dify.api.base-url}") String baseUrl, 
                             @Value("${dify.api.key}") String apiKey) {
            this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
        }

        // Dify工作流调用方法
        public Flux<String> invokeWorkflowStream(DifyWorkflowRequest request) {
            return webClient.post()
                .uri("/workflows/run")
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class); // 返回一个字符串流
        }
    }
    ```

      * `DifyWorkflowRequest`将是一个POJO，用于匹配Dify工作流API的请求体（包含`inputs`, `user`, `stream=true`等字段）。

**任务2：创建 `AIService` - 智能分析业务的核心**

  * **目的：** 编排整个AI分析流程：获取数据画像 -\> 准备上下文 -\> 调用Dify -\> 返回结果。

  * **实现建议：** 创建`AIService`接口及其实现`AIServiceImpl`。

    ```java
    public interface AIService {
        SseEmitter streamAnalysis(AnalysisRequest request);
    }

    @Service
    public class AIServiceImpl implements AIService {

        @Autowired
        private DifyApiClient difyApiClient;

        @Autowired
        private StructuredReportService structuredReportService; // 您已有的服务
        
        @Autowired
        private ObjectMapper objectMapper; // 用于JSON序列化

        @Override
        @Async // 必须异步执行，以释放处理请求的HTTP线程
        public SseEmitter streamAnalysis(AnalysisRequest request) {
            SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // 设置一个较长的超时时间

            try {
                // 1. 获取数据画像作为上下文
                StructuredReportDto report = structuredReportService.getLatestReportForDataSource(request.getDataSourceId());
                
                // 2. 将画像对象转换为紧凑的JSON字符串
                String contextJson = objectMapper.writeValueAsString(report); // 可优化为更紧凑的格式

                // 3. 构建Dify API请求
                Map<String, String> inputs = Map.of(
                    "profile_context", contextJson,
                    "user_question", request.getQuestion()
                );
                DifyWorkflowRequest difyRequest = new DifyWorkflowRequest(inputs, request.getUserId(), true);

                // 4. 调用Dify并订阅流式结果
                difyApiClient.invokeWorkflowStream(difyRequest)
                    .doOnNext(chunk -> { // 每接收到一个数据块
                        try {
                            emitter.send(SseEmitter.event().data(chunk));
                        } catch (IOException e) {
                            // 处理发送异常
                        }
                    })
                    .doOnComplete(emitter::complete) // 流结束时，关闭SSE连接
                    .doOnError(emitter::completeWithError) // 出现错误时，关闭SSE连接
                    .subscribe();

            } catch (Exception e) {
                emitter.completeWithError(e);
            }

            return emitter;
        }
    }
    ```

      * `AnalysisRequest`是需要创建的一个DTO，用于接收前端的请求参数（如`question`, `dataSourceId`, `userId`）。

-----

#### 3.3. 控制层 (Controller Layer)

这是连接前端和后端的桥梁。

**任务3：创建 `AIController`**

  * **目的：** 提供一个安全的、流式的API端点给前端调用。

  * **实现建议：**

    ```java
    @RestController
    @RequestMapping("/api/v1/ai")
    public class AIController {

        @Autowired
        private AIService aiService;

        @PostMapping("/analyze")
        public SseEmitter analyze(@RequestBody AnalysisRequest request) {
            // 直接调用Service层并返回SseEmitter对象
            return aiService.streamAnalysis(request);
        }
    }
    ```

      * 为了使`@Async`生效，请确保在主应用配置中启用了异步支持 (`@EnableAsync`)。

-----

### 4\. 数据流转图

```mermaid
sequenceDiagram
    participant Frontend as 前端 (Vue)
    participant Controller as AIController
    participant Service as AIService
    participant Profiler as Report Services
    participant DifyClient as DifyApiClient
    participant Dify as Dify API

    Frontend->>+Controller: POST /api/v1/ai/analyze (请求: question, dataSourceId)
    Controller->>+Service: streamAnalysis(request)
    Service->>+Profiler: getLatestReportForDataSource(dataSourceId)
    Profiler-->>-Service: 返回 StructuredReportDto
    
    Note right of Service: 异步执行
    Service->>+DifyClient: invokeWorkflowStream(difyRequest)
    DifyClient->>+Dify: POST /workflows/run (stream=true)

    Dify-->>-DifyClient: (开始返回数据流 chunk 1)
    DifyClient-->>-Service: Flux.onNext(chunk 1)
    Service-->>Controller: SseEmitter.send(chunk 1)
    Controller-->>-Frontend: (SSE event: chunk 1)
    
    Dify-->>-DifyClient: (返回数据流 chunk 2)
    DifyClient-->>-Service: Flux.onNext(chunk 2)
    Service-->>Controller: SseEmitter.send(chunk 2)
    Controller-->>-Frontend: (SSE event: chunk 2)

    Note over Dify, Frontend: ...持续推送...
    
    Dify-->>-DifyClient: (流结束)
    DifyClient-->>-Service: Flux.onComplete()
    Service-->>Controller: SseEmitter.complete()
    Controller-->>-Frontend: (SSE 连接关闭)
```

### 5\. 建议的开发步骤

1.  **配置先行：** 在`application.yml`中添加dify的配置。
2.  **自底向上：**
      * 先开发`DifyApiClient`，并编写单元测试或集成测试，确保能成功调用Dify API。
      * 接着开发`AIService`，重点关注数据画像的获取和上下文的构建逻辑。
      * 最后开发`AIController`，暴露API端点。
3.  **后端测试：** 使用支持SSE的API工具（如 Postman 或 curl 命令 `curl -N http://...`）测试后端流式接口是否工作正常。
4.  **前后端联调：** 当后端接口稳定后，再与前端进行集成开发。

