package com.demo.industrial.dto;

import lombok.Data;

@Data
public class QuestionRequest {
    private String question;
    private Integer topK = 3;
}