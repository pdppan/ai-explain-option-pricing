package com.demo.aiexplainoptionpricing.model;

import jakarta.validation.constraints.NotNull;
import com.demo.aiexplainoptionpricing.model.InstrumentType;
public class OptionRequest {
    private InstrumentType instrumentType;
    @NotNull
    private String symbol;

    @NotNull
    private OptionType optionType;

    @NotNull
    private Double spotPrice;
    @NotNull
    private Double strikePrice;
    @NotNull
    private Double riskFreeRate;
    @NotNull
    private Double volatility;
    @NotNull
    private Double timeToMaturity;

    private Double marketPrice;

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

    public Double getSpotPrice() {
        return spotPrice;
    }

    public void setSpotPrice(Double spotPrice) {
        this.spotPrice = spotPrice;
    }

    public Double getStrikePrice() {
        return strikePrice;
    }

    public void setStrikePrice(Double strikePrice) {
        this.strikePrice = strikePrice;
    }

    public Double getRiskFreeRate() {
        return riskFreeRate;
    }

    public void setRiskFreeRate(Double riskFreeRate) {
        this.riskFreeRate = riskFreeRate;
    }

    public Double getVolatility() {
        return volatility;
    }

    public void setVolatility(Double volatility) {
        this.volatility = volatility;
    }

    public Double getTimeToMaturity() {
        return timeToMaturity;
    }

    public void setTimeToMaturity(Double timeToMaturity) {
        this.timeToMaturity = timeToMaturity;
    }

    public Double getMarketPrice() {
        return marketPrice;
    }

    public void setMarketPrice(Double marketPrice) {
        this.marketPrice = marketPrice;
    }
    public InstrumentType getInstrumentType() {
        return instrumentType;
    }

    public void setInstrumentType(InstrumentType instrumentType) {
        this.instrumentType = instrumentType;
    }

}
