package com.demo.industrial.util;

import com.demo.industrial.service.KnowledgeBaseService.FaultKnowledge;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 日志工具类 - 专门用于记录业务日志
 */
@Component
@Slf4j
public class LogUtils {

    // 专用日志记录器
    private static final Logger BUSINESS_LOGGER = LoggerFactory.getLogger("BUSINESS_LOGGER");
    private static final Logger API_LOGGER = LoggerFactory.getLogger("API_LOGGER");

    // 统计信息缓存（简单计数）
    private static final Map<String, Long> statsCache = new ConcurrentHashMap<>();

    /**
     * 记录用户问题
     */
    public static void logQuestion(String question, String sessionId) {
        BUSINESS_LOGGER.info("[QUESTION] sessionId={}, question={}", sessionId, question);

        // 更新统计
        statsCache.merge("total_questions", 1L, Long::sum);
    }

    /**
     * 记录知识库检索结果
     */
    public static void logSearchResult(String question, List<FaultKnowledge> results, long costMs) {
        BUSINESS_LOGGER.info("[SEARCH] question={}, resultsCount={}, costMs={}",
                question, results != null ? results.size() : 0, costMs);

        if (results != null && !results.isEmpty()) {
            for (FaultKnowledge fault : results) {
                BUSINESS_LOGGER.debug("[SEARCH_DETAIL] matched: device={}, phenomenon={}",
                        fault.getDeviceType(), fault.getFaultPhenomenon());
            }
        }

        // 更新统计
        statsCache.merge("total_searches", 1L, Long::sum);
        if (results != null && !results.isEmpty()) {
            statsCache.merge("searches_with_result", 1L, Long::sum);
        }
    }

    /**
     * 记录AI调用
     */
    public static void logAICall(String prompt, String response, long costMs, boolean success) {
        if (success) {
            API_LOGGER.info("[AI_CALL] costMs={}, responseLength={}", costMs,
                    response != null ? response.length() : 0);
            API_LOGGER.debug("[AI_CALL_DETAIL] prompt={}, response={}",
                    prompt.substring(0, Math.min(200, prompt.length())),
                    response != null ? response.substring(0, Math.min(500, response.length())) : "");
        } else {
            API_LOGGER.error("[AI_CALL_FAILED] costMs={}, prompt={}", costMs,
                    prompt.substring(0, Math.min(200, prompt.length())));
        }

        // 更新统计
        if (success) {
            statsCache.merge("ai_calls_success", 1L, Long::sum);
        } else {
            statsCache.merge("ai_calls_failed", 1L, Long::sum);
        }
        statsCache.merge("ai_calls_total", 1L, Long::sum);
    }

    /**
     * 记录API调用异常
     */
    public static void logApiError(String apiName, Exception e, String params) {
        API_LOGGER.error("[API_ERROR] api={}, params={}, error={}", apiName, params, e.getMessage(), e);
        statsCache.merge("api_errors_" + apiName, 1L, Long::sum);
    }

    /**
     * 记录诊断完成
     */
    public static void logDiagnosisComplete(String question, String answer, long totalCostMs) {
        BUSINESS_LOGGER.info("[DIAGNOSIS_COMPLETE] question={}, answerLength={}, totalCostMs={}",
                question, answer != null ? answer.length() : 0, totalCostMs);

        // 更新统计
        statsCache.merge("total_diagnosis", 1L, Long::sum);
        statsCache.merge("total_cost_ms", totalCostMs, Long::sum);
    }

    /**
     * 记录会话开始/结束
     */
    public static void logSessionStart(String sessionId) {
        BUSINESS_LOGGER.info("[SESSION_START] sessionId={}", sessionId);
        statsCache.merge("sessions_started", 1L, Long::sum);
    }

    public static void logSessionEnd(String sessionId) {
        BUSINESS_LOGGER.info("[SESSION_END] sessionId={}, stats={}", sessionId, getStats());
        statsCache.merge("sessions_ended", 1L, Long::sum);
    }

    /**
     * 获取统计信息
     */
    public static Map<String, Long> getStats() {
        return new ConcurrentHashMap<>(statsCache);
    }

    /**
     * 打印统计信息
     */
    public static void printStats() {
        BUSINESS_LOGGER.info("===== 系统统计 =====");
        statsCache.forEach((key, value) -> {
            if (!key.equals("total_cost_ms")) {
                BUSINESS_LOGGER.info("{}: {}", key, value);
            }
        });
        Long totalCost = statsCache.getOrDefault("total_cost_ms", 0L);
        Long totalDiagnosis = statsCache.getOrDefault("total_diagnosis", 1L);
        if (totalDiagnosis > 0) {
            BUSINESS_LOGGER.info("average_response_time: {}ms", totalCost / totalDiagnosis);
        }
        BUSINESS_LOGGER.info("=================");
    }
}