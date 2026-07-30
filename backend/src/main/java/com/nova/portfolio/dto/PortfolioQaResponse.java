package com.nova.portfolio.dto;

public class PortfolioQaResponse {

    private Long portfolioId;
    private String question;
    private String answer;
    private AiResponseSource source;

    public Long getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(Long portfolioId) {
        this.portfolioId = portfolioId;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public AiResponseSource getSource() {
        return source;
    }

    public void setSource(AiResponseSource source) {
        this.source = source;
    }
}
