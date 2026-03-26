package com.demo.industrial.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;


import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 工业设备故障知识库服务
 */
@Slf4j
@Service
public class KnowledgeBaseService {

    private List<FaultKnowledge> faultDatabase = new ArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 在 init 方法中添加日志
    @PostConstruct
    public void init() {
        loadFaultDatabase();
        log.info("知识库初始化完成，共加载 {} 条故障记录", faultDatabase.size());
    }

    /**
     * 加载故障知识库
     */
    private void loadFaultDatabase() {
        try {
            ClassPathResource resource = new ClassPathResource("knowledge/fault-database.json");
            InputStream inputStream = resource.getInputStream();

            Map<String, List<Map<String, Object>>> data = objectMapper.readValue(
                    inputStream,
                    new TypeReference<Map<String, List<Map<String, Object>>>>() {}
            );

            List<Map<String, Object>> faults = data.get("faults");
            for (Map<String, Object> fault : faults) {
                FaultKnowledge knowledge = new FaultKnowledge();
                knowledge.setId((String) fault.get("id"));
                knowledge.setDeviceType((String) fault.get("deviceType"));
                knowledge.setFaultPhenomenon((String) fault.get("faultPhenomenon"));
                knowledge.setPossibleCauses((List<String>) fault.get("possibleCauses"));
                knowledge.setSolutions((List<String>) fault.get("solutions"));
                knowledge.setPrevention((String) fault.get("prevention"));
                knowledge.setKeywords((List<String>) fault.get("keywords"));
                faultDatabase.add(knowledge);
            }
            log.info("故障知识库加载成功，共 {} 条故障记录", faultDatabase.size());
        } catch (Exception e) {
            log.error("故障知识库加载失败", e);
            loadDefaultFaultDatabase();
        }
    }

    /**
     * 加载默认知识库（文件加载失败时使用）
     */
    private void loadDefaultFaultDatabase() {
        // 数控机床主轴故障
        FaultKnowledge spindleFault = new FaultKnowledge();
        spindleFault.setId("F001");
        spindleFault.setDeviceType("数控机床");
        spindleFault.setFaultPhenomenon("主轴无法启动");
        spindleFault.setPossibleCauses(Arrays.asList("主轴驱动器报警", "主轴电机过热", "主轴编码器故障", "抱闸未释放"));
        spindleFault.setSolutions(Arrays.asList(
                "检查驱动器面板报警代码",
                "测量电机温度，超过80℃需停机冷却",
                "检查编码器连接线是否松动",
                "确认抱闸电源是否正常"
        ));
        spindleFault.setPrevention("定期检查驱动器散热风扇，清洁编码器接口");
        spindleFault.setKeywords(Arrays.asList("主轴", "无法启动", "不转", "报警"));
        faultDatabase.add(spindleFault);

        // PLC通信故障
        FaultKnowledge plcFault = new FaultKnowledge();
        plcFault.setId("F002");
        plcFault.setDeviceType("PLC控制器");
        plcFault.setFaultPhenomenon("通信中断");
        plcFault.setPossibleCauses(Arrays.asList("网线连接故障", "IP地址配置错误", "通信协议不匹配", "交换机端口故障"));
        plcFault.setSolutions(Arrays.asList(
                "检查网线连接，确认指示灯状态",
                "ping PLC的IP地址测试连通性",
                "检查通信配置参数",
                "重启PLC和交换机"
        ));
        plcFault.setPrevention("定期备份PLC程序，使用UPS电源");
        plcFault.setKeywords(Arrays.asList("PLC", "通信", "连接不上", "断线"));
        faultDatabase.add(plcFault);

        // 伺服电机抖动
        FaultKnowledge servoFault = new FaultKnowledge();
        servoFault.setId("F003");
        servoFault.setDeviceType("伺服系统");
        servoFault.setFaultPhenomenon("电机运行抖动");
        servoFault.setPossibleCauses(Arrays.asList("增益参数不当", "机械共振", "编码器故障", "负载连接松动"));
        servoFault.setSolutions(Arrays.asList(
                "执行自动增益调整",
                "开启共振抑制功能",
                "检查编码器线缆屏蔽接地",
                "紧固联轴器螺丝"
        ));
        servoFault.setPrevention("定期校准伺服参数，检查机械连接");
        servoFault.setKeywords(Arrays.asList("伺服", "抖动", "震动", "不稳定"));
        faultDatabase.add(servoFault);

        log.info("使用默认故障知识库，共 {} 条记录", faultDatabase.size());
    }

    /**
     * 检索相关故障知识
     * @param question 用户问题
     * @param topK 返回最相关的K条
     * @return 相关故障列表
     */
    public List<FaultKnowledge> search(String question, int topK) {
        if (question == null || question.trim().isEmpty()) {
            log.debug("搜索问题为空，返回空结果");
            return Collections.emptyList();
        }

        long start = System.currentTimeMillis();
        List<ScoredFault> scored = new ArrayList<>();

        for (FaultKnowledge fault : faultDatabase) {
            int score = calculateRelevanceScore(question, fault);
            if (score > 0) {
                scored.add(new ScoredFault(fault, score));
                log.debug("匹配故障: device={}, score={}", fault.getDeviceType(), score);
            }
        }

        scored.sort((a, b) -> Integer.compare(b.score, a.score));
        List<FaultKnowledge> result = scored.stream()
                .limit(topK)
                .map(sf -> sf.fault)
                .collect(Collectors.toList());

        long cost = System.currentTimeMillis() - start;
        log.debug("搜索完成: question={}, results={}, cost={}ms", question, result.size(), cost);

        return result;
    }


    /**
     * 计算相关度分数
     */
    private int calculateRelevanceScore(String question, FaultKnowledge fault) {
        int score = 0;
        String lowerQuestion = question.toLowerCase();

        // 设备类型匹配
        if (lowerQuestion.contains(fault.getDeviceType().toLowerCase())) {
            score += 10;
        }

        // 关键词匹配
        for (String keyword : fault.getKeywords()) {
            if (lowerQuestion.contains(keyword.toLowerCase())) {
                score += 5;
            }
        }

        // 故障现象匹配
        if (lowerQuestion.contains(fault.getFaultPhenomenon().toLowerCase())) {
            score += 8;
        }

        return score;
    }

    /**
     * 构建Prompt上下文
     */
    public String buildContext(List<FaultKnowledge> faults) {
        if (faults.isEmpty()) {
            return "（知识库中暂无直接相关的故障记录）";
        }

        StringBuilder context = new StringBuilder();
        context.append("【相关故障知识库】\n\n");

        for (int i = 0; i < faults.size(); i++) {
            FaultKnowledge f = faults.get(i);
            context.append("--- 故障").append(i + 1).append(" ---\n");
            context.append("设备类型：").append(f.getDeviceType()).append("\n");
            context.append("故障现象：").append(f.getFaultPhenomenon()).append("\n");
            context.append("可能原因：\n");
            for (String cause : f.getPossibleCauses()) {
                context.append("  • ").append(cause).append("\n");
            }
            context.append("解决方案：\n");
            for (String solution : f.getSolutions()) {
                context.append("  • ").append(solution).append("\n");
            }
            if (f.getPrevention() != null) {
                context.append("预防措施：").append(f.getPrevention()).append("\n");
            }
            context.append("\n");
        }
        return context.toString();
    }

    @Data
    public static class FaultKnowledge {
        private String id;
        private String deviceType;
        private String faultPhenomenon;
        private List<String> possibleCauses;
        private List<String> solutions;
        private String prevention;
        private List<String> keywords;
    }

    private static class ScoredFault {
        FaultKnowledge fault;
        int score;
        ScoredFault(FaultKnowledge fault, int score) {
            this.fault = fault;
            this.score = score;
        }
    }
}