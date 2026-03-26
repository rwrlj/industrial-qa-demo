package com.demo.industrial.controller;

import com.demo.industrial.dto.AnswerResponse;
import com.demo.industrial.dto.QuestionRequest;
import com.demo.industrial.service.AsyncDiagnosisService;
import com.demo.industrial.util.LogUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequiredArgsConstructor
public class QaController {

    private final AsyncDiagnosisService asyncDiagnosisService;

    /**
     * 同步诊断接口（保持兼容）
     */
    @PostMapping("/api/diagnose")
    public AnswerResponse diagnose(@RequestBody QuestionRequest request) {
        String sessionId = UUID.randomUUID().toString();
        LogUtils.logSessionStart(sessionId);

        try {
            // 直接调用同步方法（如果有的话）
            // 这里为了演示，调用异步方法并阻塞等待
            CompletableFuture<AnswerResponse> future = asyncDiagnosisService.diagnoseAsync(
                    request.getQuestion(), request.getTopK(), sessionId
            );
            return future.get(); // 阻塞等待
        } catch (Exception e) {
            log.error("诊断失败", e);
            return AnswerResponse.builder()
                    .question(request.getQuestion())
                    .answer("诊断失败：" + e.getMessage())
                    .success(false)
                    .build();
        }
    }

    /**
     * 异步诊断接口（真正非阻塞）
     * 使用 DeferredResult 实现 Servlet 3.0 异步
     */
    @PostMapping("/api/diagnose/async")
    public DeferredResult<ResponseEntity<AnswerResponse>> diagnoseAsync(@RequestBody QuestionRequest request) {
        // 设置超时时间 60 秒
        DeferredResult<ResponseEntity<AnswerResponse>> deferredResult = new DeferredResult<>(60000L);

        String sessionId = UUID.randomUUID().toString();
        LogUtils.logSessionStart(sessionId);

        // 异步执行诊断
        CompletableFuture<AnswerResponse> future = asyncDiagnosisService.diagnoseAsync(
                request.getQuestion(), request.getTopK(), sessionId
        );

        // 异步回调：完成时设置结果
        future.whenComplete((response, throwable) -> {
            if (throwable != null) {
                log.error("异步诊断异常", throwable);
                deferredResult.setErrorResult(throwable);
            } else {
                deferredResult.setResult(ResponseEntity.ok(response));
            }
        });

        // 超时处理
        deferredResult.onTimeout(() -> {
            log.warn("异步诊断超时: sessionId={}", sessionId);
            deferredResult.setErrorResult(ResponseEntity.status(504).body(
                    AnswerResponse.builder()
                            .question(request.getQuestion())
                            .answer("诊断超时，请稍后重试")
                            .success(false)
                            .build()
            ));
        });

        return deferredResult;
    }
}