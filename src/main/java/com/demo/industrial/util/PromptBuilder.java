package com.demo.industrial.util;

import org.springframework.stereotype.Component;

@Component
public class PromptBuilder {

    /**
     * 构建工业故障诊断 Prompt
     */
    public String buildDiagnosisPrompt(String context, String question) {
        return """
            你是一位经验丰富的工业设备维修专家，请根据以下故障知识库回答用户的问题。

            %s

            【回答要求】
            1. 优先使用知识库中的信息进行诊断
            2. 如果知识库中没有相关信息，请给出通用的排查思路
            3. 回答格式：
               - 故障现象分析
               - 可能原因（按可能性排序）
               - 排查步骤（按顺序）
               - 解决方案
               - 预防建议
            4. 语气专业、清晰、有条理

            【用户问题】
            %s

            【诊断回答】
            """.formatted(context, question);
    }

    /**
     * 无知识库时的 Prompt
     */
    public String buildFallbackPrompt(String question) {
        return """
            你是一位工业设备维修专家，请回答以下设备故障问题。

            问题：%s

            请给出：
            1. 故障现象分析
            2. 可能的故障原因
            3. 排查建议
            4. 临时处理方案

            """.formatted(question);
    }
}