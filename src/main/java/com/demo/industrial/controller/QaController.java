package com.demo.industrial.controller;

import com.demo.industrial.dto.AnswerResponse;
import com.demo.industrial.dto.QuestionRequest;
import com.demo.industrial.service.KnowledgeBaseService;
import com.demo.industrial.service.KnowledgeBaseService.FaultKnowledge;
import com.demo.industrial.service.TongYiService;
import com.demo.industrial.util.PromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
public class QaController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final TongYiService tongYiService;
    private final PromptBuilder promptBuilder;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/api/diagnose")
    @ResponseBody
    public AnswerResponse diagnose(@RequestBody QuestionRequest request) {
        String question = request.getQuestion();
        int topK = request.getTopK();

        try {
            // 1. 检索相关知识
            List<FaultKnowledge> relevantFaults = knowledgeBaseService.search(question, topK);

            // 2. 构建上下文
            String context = knowledgeBaseService.buildContext(relevantFaults);

            // 3. 构建 Prompt
            String prompt = promptBuilder.buildDiagnosisPrompt(context, question);

            // 4. 调用通义千问
            String diagnosis = tongYiService.chat(prompt);

            // 5. 构建响应
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
            log.error("诊断失败", e);
            return AnswerResponse.builder()
                    .question(question)
                    .answer("诊断服务暂时不可用：" + e.getMessage())
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }
}