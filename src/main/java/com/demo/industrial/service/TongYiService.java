package com.demo.industrial.service;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.demo.industrial.config.TongYiConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TongYiService {

    private final TongYiConfig config;

    public String chat(String prompt) throws NoApiKeyException, ApiException, InputRequiredException {
        // 设置 API Key
        System.setProperty("dashscope.api.key", config.getApiKey());

        List<Message> messages = new ArrayList<>();
        messages.add(Message.builder()
                .role(Role.USER.getValue())
                .content(prompt)
                .build());

        GenerationParam param = GenerationParam.builder()
                .model(config.getModel())
                .messages(messages)
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .maxTokens(config.getMaxTokens())
                .temperature(config.getTemperature().floatValue())
                //重置apiKey否则报错com.alibaba.dashscope.exception.NoApiKeyException: Can not find api-key。
                .apiKey(config.getApiKey())
                .build();

        Generation generation = new Generation();
        GenerationResult result = generation.call(param);

        if (result.getOutput() != null && result.getOutput().getChoices() != null
                && !result.getOutput().getChoices().isEmpty()) {
            return result.getOutput().getChoices().get(0).getMessage().getContent();
        }

        return "抱歉，未能获取到诊断结果";
    }
}