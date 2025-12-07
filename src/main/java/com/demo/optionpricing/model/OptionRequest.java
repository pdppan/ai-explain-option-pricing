package com.demo.optionpricing.model;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OptionRequest {
    @NotNull
    private InstrumentType instrumentType;   // EQUITY_OPTION, BOND_FUTURE_OPTION, SOFR_FUTURE_OPTION
    private OptionType optionType;               // CALL / PUT
    private double spotPrice;                // see mapping below
    private double strikePrice;
    private double volatility;
    private double riskFreeRate;
    private double timeToMaturityYears;
    private double numberOfContracts;             // scaling, esp. for futures
    private String symbol;
}
