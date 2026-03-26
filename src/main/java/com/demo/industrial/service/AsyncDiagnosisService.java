package com.demo.industrial.service;

import com.demo.industrial.dto.AnswerResponse;
import com.demo.industrial.service.KnowledgeBaseService.FaultKnowledge;
import com.demo.industrial.util.LogUtils;
import com.demo.industrial.util.PromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncDiagnosisService {

    private final KnowledgeBaseService knowledgeBaseService;
    private final TongYiService tongYiService;
    private final PromptBuilder promptBuilder;

    /**
     * 异步执行诊断（核心异步方法）
     */
    @Async("aiExecutor")
    public CompletableFuture<AnswerResponse> diagnoseAsync(String question, int topK, String sessionId) {
        long startTime = System.currentTimeMillis();

        try {
            // 1. 异步检索知识库（内部同步，但整体异步）
            long searchStart = System.currentTimeMillis();
            List<FaultKnowledge> relevantFaults = knowledgeBaseService.search(question, topK);
            long searchCost = System.currentTimeMillis() - searchStart;

            // 异步记录日志（不阻塞主流程）
            CompletableFuture.runAsync(() ->
                    LogUtils.logSearchResult(question, relevantFaults, searchCost)
            );

            // 2. 构建上下文和 Prompt
            String context = knowledgeBaseService.buildContext(relevantFaults);
            String prompt = promptBuilder.buildDiagnosisPrompt(context, question);

            // 3. 异步调用 AI（耗时操作）
            long aiStart = System.currentTimeMillis();
            String diagnosis = tongYiService.chat(prompt);
            long aiCost = System.currentTimeMillis() - aiStart;

            // 4. 构建响应
            long totalCost = System.currentTimeMillis() - startTime;

            AnswerResponse response = AnswerResponse.builder()
                    .question(question)
                    .answer(diagnosis)
                    .references(relevantFaults.stream()
                            .map(f -> AnswerResponse.Reference.builder()
                                    .id(f.getId())
                                    .deviceType(f.getDeviceType())
                                    .faultPhenomenon(f.getFaultPhenomenon())
                                    .build())
                            .collect(Collectors.toList()))
                    .success(true)
                    .build();

            // 异步记录完成日志
            CompletableFuture.runAsync(() -> {
                LogUtils.logDiagnosisComplete(question, diagnosis, totalCost);
                log.info("异步诊断完成: sessionId={}, totalCost={}ms, aiCost={}ms, searchCost={}ms",
                        sessionId, totalCost, aiCost, searchCost);
            });

            return CompletableFuture.completedFuture(response);

        } catch (Exception e) {
            log.error("异步诊断失败", e);
            return CompletableFuture.completedFuture(
                    AnswerResponse.builder()
                            .question(question)
                            .answer("诊断失败：" + e.getMessage())
                            .success(false)
                            .errorMessage(e.getMessage())
                            .build()
            );
        }
    }
}