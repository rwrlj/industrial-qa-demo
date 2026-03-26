package com.demo.industrial.util;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class LogUtils {

    private static final Logger BUSINESS_LOGGER = LoggerFactory.getLogger("BUSINESS_LOGGER");
    private static final Logger API_LOGGER = LoggerFactory.getLogger("API_LOGGER");

    private static final Map<String, Long> statsCache = new ConcurrentHashMap<>();


    /**
     * 异步记录用户问题（不阻塞主线程）
     */
    @Async("logExecutor")
    public CompletableFuture<Void> logQuestionAsync(String question, String sessionId) {
        return CompletableFuture.runAsync(() -> {
            BUSINESS_LOGGER.info("[QUESTION_ASYNC] sessionId={}, question={}", sessionId, question);
            statsCache.merge("total_questions_async", 1L, Long::sum);
        });
    }


    /**
     * 异步记录检索结果（不阻塞主线程）
     */
    @Async("logExecutor")
    public CompletableFuture<Void> logSearchResultAsync(String question, Object results, long costMs) {
        return CompletableFuture.runAsync(() -> {
            BUSINESS_LOGGER.info("[SEARCH_ASYNC] question={}, resultsCount={}, costMs={}",
                    question, results != null ? getSize(results) : 0, costMs);
            statsCache.merge("total_searches_async", 1L, Long::sum);
        });
    }

    /**
     * 异步记录 AI 调用
     */
    @Async("logExecutor")
    public CompletableFuture<Void> logAICallAsync(String prompt, String response, long costMs, boolean success) {
        return CompletableFuture.runAsync(() -> {
            if (success) {
                API_LOGGER.info("[AI_CALL_ASYNC] costMs={}, responseLength={}", costMs,
                        response != null ? response.length() : 0);
            } else {
                API_LOGGER.error("[AI_CALL_FAILED_ASYNC] costMs={}", costMs);
            }
        });
    }


    /**
     * 获取统计信息
     */
    public static Map<String, Long> getStats() {
        return new ConcurrentHashMap<>(statsCache);
    }

    /**
     * 打印统计信息到日志
     */
    public static void printStats() {
        BUSINESS_LOGGER.info("===== 系统统计信息 =====");

        // 打印各项统计
        statsCache.forEach((key, value) -> {
            if (!key.equals("total_cost_ms") && !key.contains("cost")) {
                BUSINESS_LOGGER.info("  {}: {}", key, value);
            }
        });

        // 计算平均响应时间
        Long totalCost = statsCache.getOrDefault("total_cost_ms", 0L);
        Long totalDiagnosis = statsCache.getOrDefault("total_diagnosis", 1L);
        if (totalDiagnosis > 0 && totalCost > 0) {
            long avgCost = totalCost / totalDiagnosis;
            BUSINESS_LOGGER.info("  average_response_time: {}ms", avgCost);
        }

        // 计算成功率
        Long totalCalls = statsCache.getOrDefault("ai_calls_total", 0L);
        Long successCalls = statsCache.getOrDefault("ai_calls_success", 0L);
        if (totalCalls > 0) {
            double successRate = (double) successCalls / totalCalls * 100;
            BUSINESS_LOGGER.info("  ai_success_rate: {:.2f}%", successRate);
        }

        // 计算检索命中率
        Long totalSearches = statsCache.getOrDefault("total_searches", 0L);
        Long searchesWithResult = statsCache.getOrDefault("searches_with_result", 0L);
        if (totalSearches > 0) {
            double hitRate = (double) searchesWithResult / totalSearches * 100;
            BUSINESS_LOGGER.info("  search_hit_rate: {:.2f}%", hitRate);
        }

        BUSINESS_LOGGER.info("========================");
    }

    /**
     * 重置统计信息
     */
    public static void resetStats() {
        statsCache.clear();
        BUSINESS_LOGGER.info("[STATS_RESET] 统计信息已重置");
    }

    /**
     * 记录诊断完成（包含耗时统计）
     */
    public static void logDiagnosisComplete(String question, String answer, long totalCostMs) {
        BUSINESS_LOGGER.info("[DIAGNOSIS_COMPLETE] question={}, answerLength={}, totalCostMs={}",
                question, answer != null ? answer.length() : 0, totalCostMs);

        // 更新统计
        statsCache.merge("total_diagnosis", 1L, Long::sum);
        statsCache.merge("total_cost_ms", totalCostMs, Long::sum);
    }

    /**
     * 记录 AI 调用
     */
    public static void logAICall(String prompt, String response, long costMs, boolean success) {
        if (success) {
            API_LOGGER.info("[AI_CALL] costMs={}, responseLength={}", costMs,
                    response != null ? response.length() : 0);
            statsCache.merge("ai_calls_success", 1L, Long::sum);
        } else {
            API_LOGGER.error("[AI_CALL_FAILED] costMs={}", costMs);
            statsCache.merge("ai_calls_failed", 1L, Long::sum);
        }
        statsCache.merge("ai_calls_total", 1L, Long::sum);
    }

    /**
     * 记录检索结果
     */
    public static void logSearchResult(String question, Object results, long costMs) {
        int resultCount = results != null ? getSize(results) : 0;
        BUSINESS_LOGGER.info("[SEARCH] question={}, resultsCount={}, costMs={}",
                question, resultCount, costMs);

        statsCache.merge("total_searches", 1L, Long::sum);
        if (resultCount > 0) {
            statsCache.merge("searches_with_result", 1L, Long::sum);
        }
    }

    /**
     * 记录用户问题
     */
    public static void logQuestion(String question, String sessionId) {
        BUSINESS_LOGGER.info("[QUESTION] sessionId={}, question={}", sessionId, question);
        statsCache.merge("total_questions", 1L, Long::sum);
    }

    /**
     * 记录会话开始
     */
    public static void logSessionStart(String sessionId) {
        BUSINESS_LOGGER.info("[SESSION_START] sessionId={}", sessionId);
        statsCache.merge("sessions_started", 1L, Long::sum);
    }

    /**
     * 记录会话结束
     */
    public static void logSessionEnd(String sessionId) {
        BUSINESS_LOGGER.info("[SESSION_END] sessionId={}", sessionId);
        statsCache.merge("sessions_ended", 1L, Long::sum);
    }

    /**
     * 记录 API 错误
     */
    public static void logApiError(String apiName, Exception e, String params) {
        API_LOGGER.error("[API_ERROR] api={}, params={}, error={}", apiName, params, e.getMessage(), e);
        statsCache.merge("api_errors_" + apiName, 1L, Long::sum);
    }

    private static int getSize(Object obj) {
        if (obj instanceof java.util.Collection) {
            return ((java.util.Collection<?>) obj).size();
        }
        if (obj instanceof Map) {
            return ((Map<?, ?>) obj).size();
        }
        return 0;
    }



}