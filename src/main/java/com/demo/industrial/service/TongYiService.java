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
import com.demo.industrial.util.LogUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TongYiService {

    private final TongYiConfig config;

    public String chat(String prompt) throws NoApiKeyException, ApiException, InputRequiredException {
        String apiKey = config.getApiKey();

        // 检查 API Key 是否配置
        if (!StringUtils.hasText(apiKey) || apiKey.equals("your-api-key-here") || apiKey.isEmpty()) {
            log.error("API Key 未配置！请在 application.yml 中设置 tongyi.api-key");
            throw new RuntimeException("API Key 未配置。请在 application.yml 中设置 tongyi.api-key，或设置环境变量 TONGYI_API_KEY");
        }

        // 设置 API Key
        System.setProperty("dashscope.api.key", apiKey);

        log.debug("调用通义千问，模型: {}, prompt长度: {}", config.getModel(), prompt.length());

        try {
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
                String answer = result.getOutput().getChoices().get(0).getMessage().getContent();
                log.info("通义千问调用成功，返回内容长度: {}", answer != null ? answer.length() : 0);
                return answer;
            }

            log.warn("通义千问返回结果为空");
            return "抱歉，未能获取到诊断结果";

        } catch (NoApiKeyException e) {
            log.error("API Key 无效", e);
            LogUtils.logApiError("tongyi", e, "apiKey=" + apiKey.substring(0, Math.min(10, apiKey.length())) + "...");
            throw e;
        } catch (ApiException | InputRequiredException e) {
            log.error("通义千问API调用失败", e);
            LogUtils.logApiError("tongyi", e, "model=" + config.getModel());
            throw e;
        } catch (Exception e) {
            log.error("通义千问调用异常", e);
            LogUtils.logApiError("tongyi", e, "unknown error");
            throw new RuntimeException("通义千问调用失败: " + e.getMessage(), e);
        }
    }
}