package com.nova.portfolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PortfolioQaRequest {

    @NotBlank(message = "question is required")
    @Size(max = 500, message = "question must be <= 500 chars")
    private String question;

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }
}
