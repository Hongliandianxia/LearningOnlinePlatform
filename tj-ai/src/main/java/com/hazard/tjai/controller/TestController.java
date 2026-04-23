package com.hazard.tjai.controller;


import com.hazard.tjai.tool.WeatherTool;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.PromptMetadata;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Optional;


/**
 * @author hazard
 * @version 1.0
 * @description 测试controller
 * @date 2026/1/27 13:46
 */
@RestController
public class TestController {
    @Resource
    private ChatModel chatModel;
    @Resource
    private ChatClient chatClient;

    @GetMapping("/test")
    public Flux<String> test() {
        return chatClient
                .prompt("what is the weather in San Francisco")
                .tools(new WeatherTool())
                .stream()
                .content();
    }


    /**
     * 流式返回结果
     *
     * @param msg
     * @return
     */
    @GetMapping("/chat1")
    public Flux<String> chat1(@RequestParam(defaultValue = "java教程,使用json格式化输出") String msg) {
        Flux<String> content = chatClient.prompt()
                .system("你是一名java开发的导师,只回答和java有关方面的内容,其他内容一律无可奉告")
                .user(msg)
                .stream()
                .content();
        return content;
    }


    /**
     * 阻塞式返回结果
     *
     * @param msg
     * @return
     */
    @GetMapping("/chat2")
    public String chat2(String msg) {

        return chatModel.call(msg);
    }

    @GetMapping("/chat3")
    public String chat3(String msg) {

        UserMessage userMessage = new UserMessage(msg);
        ChatResponse response = chatModel.call(new Prompt(userMessage));
        ChatResponseMetadata responseMetadata = response.getMetadata();
        PromptMetadata promptMetadata = responseMetadata.getPromptMetadata();
        Optional<PromptMetadata.PromptFilterMetadata> byPromptIndex = promptMetadata.findByPromptIndex(0);
        String text = response.getResult().getOutput().getText();
        return text;

    }
}

