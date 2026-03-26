package com.demo.industrial.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class AnswerResponse {
    private String question;
    private String answer;
    private List<Reference> references;
    private boolean success;
    private String errorMessage;

    @Data
    @Builder
    public static class Reference {
        private String deviceType;
        private String faultPhenomenon;
        private String id;
    }
}