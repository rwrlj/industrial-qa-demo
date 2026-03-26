package com.demo.industrial.controller;

import com.demo.industrial.dto.AnswerResponse;
import com.demo.industrial.dto.QuestionRequest;
import com.demo.industrial.service.KnowledgeBaseService;
import com.demo.industrial.service.KnowledgeBaseService.FaultKnowledge;
import com.demo.industrial.service.TongYiService;
import com.demo.industrial.util.LogUtils;
import com.demo.industrial.util.PromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
public class QaController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final TongYiService tongYiService;
    private final PromptBuilder promptBuilder;

    // 使用ThreadLocal存储当前会话ID（简化示例，实际可用Session）
    private static final ThreadLocal<String> sessionHolder = new ThreadLocal<>();

    @GetMapping("/")
    public String index() {
        // 新会话开始
        String sessionId = UUID.randomUUID().toString();
        sessionHolder.set(sessionId);
        LogUtils.logSessionStart(sessionId);
        return "index";
    }

    @PostMapping("/api/diagnose")
    @ResponseBody
    public AnswerResponse diagnose(@RequestBody QuestionRequest request) {
        String sessionId = sessionHolder.get();
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString();
            sessionHolder.set(sessionId);
            LogUtils.logSessionStart(sessionId);
        }

        String question = request.getQuestion();
        int topK = request.getTopK();

        // 记录用户问题
        LogUtils.logQuestion(question, sessionId);

        long startTime = System.currentTimeMillis();

        try {
            // 1. 检索相关知识（记录耗时）
            long searchStart = System.currentTimeMillis();
            List<FaultKnowledge> relevantFaults = knowledgeBaseService.search(question, topK);
            long searchCost = System.currentTimeMillis() - searchStart;

            // 记录检索结果
            LogUtils.logSearchResult(question, relevantFaults, searchCost);

            // 2. 构建上下文
            String context = knowledgeBaseService.buildContext(relevantFaults);

            // 3. 构建 Prompt
            String prompt = promptBuilder.buildDiagnosisPrompt(context, question);

            // 4. 调用通义千问（记录耗时）
            long aiStart = System.currentTimeMillis();
            String diagnosis = tongYiService.chat(prompt);
            long aiCost = System.currentTimeMillis() - aiStart;

            // 记录AI调用成功
            LogUtils.logAICall(prompt, diagnosis, aiCost, true);

            // 5. 构建响应
            long totalCost = System.currentTimeMillis() - startTime;
            LogUtils.logDiagnosisComplete(question, diagnosis, totalCost);

            log.info("诊断完成: sessionId={}, totalCost={}ms, aiCost={}ms, searchCost={}ms",
                    sessionId, totalCost, aiCost, searchCost);

            return AnswerResponse.builder()
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

        } catch (Exception e) {
            long totalCost = System.currentTimeMillis() - startTime;
            log.error("诊断失败: sessionId={}, question={}, costMs={}", sessionId, question, totalCost, e);
            LogUtils.logApiError("diagnose", e, "question=" + question);

            return AnswerResponse.builder()
                    .question(question)
                    .answer("诊断服务暂时不可用：" + e.getMessage())
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }
}