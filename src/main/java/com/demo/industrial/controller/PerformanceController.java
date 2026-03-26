package com.demo.industrial.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/performance")
@RequiredArgsConstructor
public class PerformanceController {

    /**
     * 获取线程池状态
     */
    @GetMapping("/thread-pool")
    public Map<String, Object> getThreadPoolStatus() {
        Map<String, Object> status = new HashMap<>();

        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        status.put("total_threads", threadMXBean.getThreadCount());
        status.put("peak_threads", threadMXBean.getPeakThreadCount());
        status.put("daemon_threads", threadMXBean.getDaemonThreadCount());

        return status;
    }

    /**
     * 获取系统性能指标
     */
    @GetMapping("/metrics")
    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        Runtime runtime = Runtime.getRuntime();
        metrics.put("available_processors", runtime.availableProcessors());
        metrics.put("total_memory_mb", runtime.totalMemory() / 1024 / 1024);
        metrics.put("free_memory_mb", runtime.freeMemory() / 1024 / 1024);
        metrics.put("max_memory_mb", runtime.maxMemory() / 1024 / 1024);
        metrics.put("used_memory_mb", (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024);

        return metrics;
    }
}