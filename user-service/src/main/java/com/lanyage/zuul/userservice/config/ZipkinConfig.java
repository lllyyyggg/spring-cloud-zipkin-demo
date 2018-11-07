package com.lanyage.zuul.userservice.config;

import com.github.kristofa.brave.Brave;
import com.github.kristofa.brave.EmptySpanCollectorMetricsHandler;
import com.github.kristofa.brave.Sampler;
import com.github.kristofa.brave.SpanCollector;
import com.github.kristofa.brave.http.DefaultSpanNameProvider;
import com.github.kristofa.brave.http.HttpSpanCollector;
import com.github.kristofa.brave.okhttp.BraveOkHttpRequestResponseInterceptor;
import com.github.kristofa.brave.servlet.BraveServletFilter;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
