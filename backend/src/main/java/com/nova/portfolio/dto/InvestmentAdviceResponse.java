package com.nova.portfolio.dto;

import java.util.List;

public class InvestmentAdviceResponse {

    private Long portfolioId;
    private String advice;
    private List<String> signals;
    private AiResponseSource source;
    private String disclaimer;

    public Long getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(Long portfolioId) {
        this.portfolioId = portfolioId;
    }

    public String getAdvice() {
        return advice;
    }

    public void setAdvice(String advice) {
        this.advice = advice;
    }

    public List<String> getSignals() {
        return signals;
    }

    public void setSignals(List<String> signals) {
        this.signals = signals;
    }

    public AiResponseSource getSource() {
        return source;
    }

    public void setSource(AiResponseSource source) {
        this.source = source;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }
}
