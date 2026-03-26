package com.demo.industrial;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class IndustrialQaApplication {
    public static void main(String[] args) {
        SpringApplication.run(IndustrialQaApplication.class, args);
        System.out.println("\n╔════════════════════════════════════════════════════╗");
        System.out.println("║    工业设备故障诊断问答系统已启动              ║");
        System.out.println("║    访问地址: http://localhost:8080              ║");
        System.out.println("╚════════════════════════════════════════════════════╝\n");
    }
}