package com.demo.optionpricing.model;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
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
}
