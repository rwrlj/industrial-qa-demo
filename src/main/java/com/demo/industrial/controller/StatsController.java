package com.demo.industrial.controller;

import com.demo.industrial.util.LogUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    @GetMapping
    public Map<String, Long> getStats() {
        Map<String, Long> stats = LogUtils.getStats();
        log.info("获取系统统计: {}", stats);
        return stats;
    }

    @PostMapping("/print")
    public String printStats() {
        LogUtils.printStats();
        return "统计信息已打印到日志";
    }
}