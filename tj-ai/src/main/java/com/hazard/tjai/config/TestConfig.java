package com.hazard.tjai.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author hazard
 * @version 1.0
 * @description
 * @date 2026/1/27 14:06
 */
@Configuration
public class TestConfig {
    @Resource
    private ChatModel chatModel;

    /**
     * 创建一个DashScopeApi实例，并使用环境变量中的API密钥进行初始化,用于获取到saa的api。
     *
     * @return 创建的DashScopeApi实例
     */
    @Bean
    public DashScopeApi dashScopeApi()
    {
        return DashScopeApi.builder()
                .apiKey(System.getenv("tjxt_Aliapikey"))
                .build();
    }

    /**
     * 创建一个ChatClient实例，并使用默认的ChatModel。
     * @return 创建的ChatClient实例
     */
    @Bean
    public ChatClient chatClient() {
        return ChatClient.builder(chatModel).build();
    }
}
