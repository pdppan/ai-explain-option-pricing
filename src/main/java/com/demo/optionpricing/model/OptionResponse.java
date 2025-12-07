package com.demo.optionpricing.model;
import lombok.Data;
@Data
public class OptionResponse {
    private InstrumentType instrumentType;
    private String symbol;
    private OptionType optionType;
    private double price;
    private double delta;
    private double gamma;
    private double theta;
    private double vega;
    private double rho;
    private String explanation;
}
