package com.demo.aiexplainoptionpricing.model;

public class OptionPricingResult {

    private String symbol;
    private OptionType optionType;

    private double theoreticalPrice;
    private double delta;
    private double gamma;
    private double theta;
    private double vega;

    private Double marketPrice;
    private Double mispricing;

    private String llmExplanation;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public OptionType getOptionType() {
        return optionType;
    }

    public void setOptionType(OptionType optionType) {
        this.optionType = optionType;
    }

    public double getTheoreticalPrice() {
        return theoreticalPrice;
    }

    public void setTheoreticalPrice(double theoreticalPrice) {
        this.theoreticalPrice = theoreticalPrice;
    }

    public double getDelta() {
        return delta;
    }

    public void setDelta(double delta) {
        this.delta = delta;
    }

    public double getGamma() {
        return gamma;
    }

    public void setGamma(double gamma) {
        this.gamma = gamma;
    }

    public double getTheta() {
        return theta;
    }

    public void setTheta(double theta) {
        this.theta = theta;
    }

    public double getVega() {
        return vega;
    }

    public void setVega(double vega) {
        this.vega = vega;
    }

    public Double getMarketPrice() {
        return marketPrice;
    }

    public void setMarketPrice(Double marketPrice) {
        this.marketPrice = marketPrice;
    }

    public Double getMispricing() {
        return mispricing;
    }

    public void setMispricing(Double mispricing) {
        this.mispricing = mispricing;
    }

    public String getLlmExplanation() {
        return llmExplanation;
    }

    public void setLlmExplanation(String llmExplanation) {
        this.llmExplanation = llmExplanation;
    }
}
