分布式链路跟踪(Sleuth)

当我们进行微服务架构开发时，通常会根据业务来划分微服务，各业务之间通过REST进行调用。一个用户操作，可能需要很多微服务的协同才能完成，如果在业务调用链路上任何一个微服务出现问题或者网络超时，都会导致功能失败。随着业务越来越多，对于微服务之间的调用链的分析会越来越复杂。

Spring Cloud Sleuth为服务之间调用提供链路追踪。通过Sleuth可以很清楚的了解到一个服务请求经过了哪些服务，每个服务处理花费了多长。从而让我们可以很方便的理清各微服务间的调用关系。此外Sleuth可以帮助我们：

* 耗时分析: 通过Sleuth可以很方便的了解到每个采样请求的耗时，从而分析出哪些服务调用比较耗时;
* 可视化错误: 对于程序未捕捉的异常，可以通过集成Zipkin服务界面上看到;
* 链路优化: 对于调用比较频繁的服务，可以针对这些服务实施一些优化措施。

> Sleuth+Log 示例代码

我们先用最简单的方式集成Sleuth，把Sleuth所跟踪到的信息输出到日志中。基础代码采用之前所构建的商城项目。

1. 首先改造bootstrap.properties文件。

为了能够让日志文件可以获取到服务名称，我们需要将原来配置在application.properties中的部分内容移入到bootstrap.properties配置文件中，这是因为SpringBoot在启动时会优先扫描bootstrap配置源，从而能够让日志可以获取到服务名称。

bootstrap.properties

```
server.port=8080
spring.application.name=MALL-WEB
```
application.properties

```
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8081/eureka
logging:
  level:
    org.springframework: INFO
    org.springframework.web.servlet.DispatcherServlet: DEBUG
```

在resources目录中增加一个名称为: logback-spring.xml的文件，内容如下：

```
<?xml version="1.0" encoding="UTF-8" ?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    ​
    <springProperty scope="context" name="springAppName" source="spring.application.name"/>

    <!-- Example for logging into the build folder of your project -->
    <property name="LOG_FILE" value="${BUILD_FOLDER:-build}/${springAppName}"/>​

    <property name="CONSOLE_LOG_PATTERN"
              value="%clr(%d{yyyy-MM-dd HH:mm:ss.SSS}){faint} %clr(${LOG_LEVEL_PATTERN:-%5p})
            %clr(${PID:- }){magenta} %clr(---){faint} %clr([%15.15t]){faint} %clr(%-40.40logger{39}){cyan} %clr(:){faint} %m%n${LOG_EXCEPTION_CONVERSION_WORD:-%wEx}"/>

    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <layout class="ch.qos.logback.classic.PatternLayout">
            <Pattern>
                %d{yyyy-MM-dd HH:mm:ss SSS} [%thread] %-5level %logger{36} - %msg%n
            </Pattern>
        </layout>
    </appender>

    <!-- Appender to log to console -->
    <appender name="console" class="ch.qos.logback.core.ConsoleAppender">
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <!-- Minimum logging level to be presented in the console logs-->
            <level>DEBUG</level>
        </filter>
        <encoder>
            <pattern>${CONSOLE_LOG_PATTERN}</pattern>
            <charset>utf8</charset>
        </encoder>
    </appender>

    <!-- Appender to log to file -->​
    <appender name="flatfile" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_FILE}</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_FILE}.%d{yyyy-MM-dd}.gz</fileNamePattern>
            <maxHistory>7</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>${CONSOLE_LOG_PATTERN}</pattern>
            <charset>utf8</charset>
        </encoder>
    </appender>
    ​    ​
    <root level="INFO">
        <appender-ref ref="console"/>
        <!-- uncomment this to have also JSON logs -->
        <!--<appender-ref ref="logstash"/>-->
        <!--<appender-ref ref="flatfile"/>-->
    </root>
</configuration>
```

以同样的方式来设置USER-SERVICE服务。

然后启动`USER-SERVICE`和`USER-CONSUMER`，然后就可以看到如此的日志。

```
2018-11-06 21:12:11.556 DEBUG [USER-CONSUMER,59c0af14a3a2cb00,59c0af14a3a2cb00,false]             76393 --- [nio-8080-exec-4] o.s.web.servlet.DispatcherServlet        : GET "/favicon.ico", parameters={}
2018-11-06 21:12:11.573 DEBUG [USER-CONSUMER,59c0af14a3a2cb00,59c0af14a3a2cb00,false]             76393 --- [nio-8080-exec-4] o.s.web.servlet.DispatcherServlet        : Completed 200 OK
```

日志中类似 [MALL-WEB,e23abdb6268af95d,e23abdb6268af95d,false]、[PRODUCT-SERVICE,e23abdb6268af95d,c68a9b1c2ab8a025,false] 的日志内容它们的格式为： [appname,traceId,spanId,exportable]，也就是Sleuth的跟踪数据。其中:

* appname: 为微服务的服务名称;
* traceId\spanId: 为Sleuth链路追踪的两个术语，后面我们再仔细介绍;
* exportable 是否是发送给Zipkin。

> Sleuth术语

* Span: 最基本的工作单元。例如: 发送一个RPC就是一个新的span，同样一次RPC的应答也是。Span通过一个唯一的，长度为64位的ID来作为标识，另外，再使用一个64位ID用于服务调用跟踪。Span也可以带有其他数据，例如：描述，时间戳，键值对标签，起始Span的ID，以及处理ID（通常使用IP地址）等等。 Span有起始和结束，它们用于跟踪时间信息。Span应该都是成对出现的，有始必有终，所以一旦创建了一个span，那就必须在未来某个时间点结束它。
	
	提示： 起始的Span通常被称为:root span。它的id通常也被作为一个跟踪记录的id。
* Trace: 一个树结构的Span集合。例如：在分布式大数据存储中，可能每一次请求都是一次跟踪记录。
* annotation: 用于记录一个事件的时间信息。一些基础核心的Annotation用于记录请求的起始和结束时间，例如:
	* cs: 客户端发送(Client Sent的缩写)。这个annotation表示一个span的起始;
	* sr: 服务端接收(Server Received的缩写)。表示服务端接收到请求，并开始处理。
	* ss: 服务端完成请求处理，应答信息被发回客户端(Server Sent的缩写)。如果减去sr的时间戳，则可以计算出服务端处理请求的耗时。
	* cr: 客户端接收(Client Received的缩写)。标志着Span的结束。客户端成功的接收到服务端的应答信息。如果减去cs的时间戳，则可以计算出请求的响应耗时。

下图，通过可视化的方式描述了Span和Trace的概念：

![](https://upload-images.jianshu.io/upload_images/1488771-22b36e750150eae9.png?imageMogr2/auto-orient/)

图中每一个颜色都表示着一个span（总共7个span，从A到G）。它们都有以下这些数据信息：

```
Trace Id = X
Span Id = D
Client Sent
```
表示该Span的Trace-Id为X，Span-Id为D。相应的事件为Client Sent。

这些Span的上下级关系可以通过下图来表示：

![](https://upload-images.jianshu.io/upload_images/1488771-28b66c5976c5164b.png?imageMogr2/auto-orient/)

> 整合Zipkin服务

Zipkin是一个致力于收集分布式服务的时间数据的分布式跟踪系统。其主要涉及以下四个组件：

* collector: 数据采集;
* storage: 数据存储;	
* search: 数据查询;
* UI: 数据展示.

Zipkin提供了可插拔数据存储方式：In-Memory、MySql、Cassandra以及Elasticsearch。接下来的测试为方便直接采用In-Memory方式进行存储，个人推荐Elasticsearch，特别是后续当我们需要整合ELK时。

在本篇中我们仅通过Http的方式向Zipkin提供跟踪数据，关于使用stream的方式后续讲到Spring Cloud Bus的时候再说明。我们所要搭建的系统架构如下(做了精简)：

![](https://upload-images.jianshu.io/upload_images/1488771-ea5e54367889a837.png?imageMogr2/auto-orient/)

构建Zipkin-Server

```
<dependency>
	<groupId>io.zipkin.java</groupId>
	<artifactId>zipkin-server</artifactId>
	<version>2.7.5</version>
</dependency>
<dependency>
	<groupId>io.zipkin.java</groupId>
	<artifactId>zipkin-autoconfigure-ui</artifactId>
	<version>2.11.8</version>
</dependency>
```		
然后使用`@zipkin2.internal.xxx.EnableZipkinServer`开启ZIPKIN服务。

然后编写`bootstrap.properties`文件。
```
server.port=8240
spring.application.name=ZIPKIN-SERVER

```
然后修改`USER-CONSUMER`中添加`ZIPKIN-STARTER`。

```
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-zipkin</artifactId>
</dependency>
```
同时可以**删除**之前的：

```
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-sleuth</artifactId>
</dependency>
```

修改application.properties配置文件.

```
spring.zipkin.base-url=http://localhost:8240
spring.sleuth.sampler.percentage=1.0
```
然后同样方式修改`USER-SERVICE`。

然后依次启动`服务发现`和`ZIPKIN SERVER`和`服务提供商`和`服务消费方`。

然后访问`http://localhost:8181/zipkin/`。

如果有报错，那么就使用。`management.metrics.web.server.auto-time-requests=false`来关闭错误。

springboot 2.x之后提供了一键脚本。

```
curl -ssl https://zipkin.io/quickstart.sh | bash -s
java -jar zipkin.jar --server.port=8181
```

启动之后如果不能追踪的话还需要自行配置`spanCollector`和`brave`等。

```
@Configuration
public class ZipkinConfig {

    private static final Logger logger = LoggerFactory.getLogger(ZipkinConfig.class);

    //span（一次请求信息或者一次链路调用）信息收集器
    @Bean(name = "spanCollector")
    public SpanCollector spanCollector() {
        logger.info("==> {} has been initialized. <==", "spanCollector");
        HttpSpanCollector.Config config = HttpSpanCollector.Config.builder()
                .compressionEnabled(false) //默认false，span在transport之前是否会被gzipped
                .connectTimeout(5000)
                .flushInterval(1)
                .readTimeout(6000)
                .build();
        return HttpSpanCollector.create("http://localhost:8181", config, new EmptySpanCollectorMetricsHandler());
    }

    //作为各调用链路，只需要负责将指定格式的数据发送给zipkin
    @Bean(name = "brave")
    public Brave brave(@Qualifier("spanCollector") SpanCollector spanCollector) {
        logger.info("==> {} has been initialized. <==", "brave");
        Brave.Builder builder = new Brave.Builder("USER-SERVICE");  //指定service-name
        builder.spanCollector(spanCollector);
        builder.traceSampler(Sampler.create(1)); //采集率
        return builder.build();
    }

    //设置server的过滤器，服务端收到请求和服务端完成处理，并将结果发送给客户端。
    @Bean
    public BraveServletFilter braveServletFilter(Brave brave) {
        logger.info("==> {} has been initialized. <==", "braveServletFilter");
        BraveServletFilter filter = new BraveServletFilter(brave.serverRequestInterceptor(),
                brave.serverResponseInterceptor(), new DefaultSpanNameProvider());
        return filter;
    }

    //设置client的拦截器。
    @Bean
    public OkHttpClient okHttpClient(@Qualifier("brave") Brave brave) {
        logger.info("==> {} has been initialized. <==", "okHttpClient");
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addInterceptor(new BraveOkHttpRequestResponseInterceptor(brave.clientRequestInterceptor(),
                        brave.clientResponseInterceptor(), new DefaultSpanNameProvider())).build();
        return httpClient;
    }
}
```

然后依次启动服务发现、zipkit和各个微服务。

然后调用几次微服务你就可以看到页面上会有变化。点开会有惊喜。
	![](https://upload-images.jianshu.io/upload_images/1488771-3dec10a8dc33a21d.png?imageMogr2/auto-orient/)


采样率。0.0-1.0

可以通过配置采样率来达到采样效果。

```
@Bean public Sampler defaultSampler() {
    return new AlwaysSampler();
}
```		