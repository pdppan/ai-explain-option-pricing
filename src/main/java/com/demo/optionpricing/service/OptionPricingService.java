package com.demo.optionpricing.service;

import com.demo.optionpricing.model.OptionResponse;
import com.demo.optionpricing.model.OptionRequest;
import com.demo.optionpricing.model.OptionType;
import org.springframework.stereotype.Service;

@Service
public class OptionPricingService {
    public OptionResponse price(OptionRequest req) {
        if (req.getInstrumentType() == null) {
            throw new IllegalArgumentException("InstrumentType is required on OptionRequest");
        }
        switch (req.getInstrumentType()) {
            case EQUITY_OPTION:
                return priceEquityOption(req);       // Black–Scholes on spot
            case BOND_FUTURE_OPTION:
                return priceBondFutureOption(req);   // Black-76 on bond future
            case SOFR_FUTURE_OPTION:
                return priceSofrFutureOption(req);   // Black-76 on SOFR future
            default:
                return priceEquityOption(req);
        }
    }

    /**
     * SOFR future option using Black-76.
     * Treat spotPrice as the quoted futures price (F0).
     */
    private OptionResponse priceSofrFutureOption(OptionRequest r) {

        double F0    = r.getSpotPrice();          // futures price for SOFR
        double K     = r.getStrikePrice();
        double sigma = r.getVolatility();
        double T     = r.getTimeToMaturityYears();
        double rRate = r.getRiskFreeRate();
        boolean isCall = r.getOptionType() == OptionType.CALL;

        double contractSize = r.getNumberOfContracts() > 0 ? r.getNumberOfContracts() : 1.0;

        double sigmaSqrtT = sigma * Math.sqrt(T);
        double d1 = (Math.log(F0 / K) + 0.5 * sigma * sigma * T) / sigmaSqrtT;
        double d2 = d1 - sigmaSqrtT;

        double Nd1   = N(d1);
        double Nd2   = N(d2);
        double Nminusd1 = N(-d1);
        double Nminusd2 = N(-d2);
        double nd1   = n(d1);
        double df    = Math.exp(-rRate * T);

        double unitPrice;
        double unitDelta;
        double unitGamma;
        double unitVega;
        double unitTheta;
        double unitRho;

        if (isCall) {
            unitPrice = df * (F0 * Nd1 - K * Nd2);

            unitDelta = df * Nd1;                                 // dC/dF
            unitGamma = df * nd1 / (F0 * sigmaSqrtT);             // d2C/dF2
            unitVega  = df * F0 * nd1 * Math.sqrt(T);

            unitTheta = df * ( - (F0 * nd1 * sigma) / (2.0 * Math.sqrt(T))
                    + rRate * (F0 * Nd1 - K * Nd2));

        } else {
            unitPrice = df * (K * Nminusd2 - F0 * Nminusd1);

            unitDelta = -df * Nminusd1;
            unitGamma = df * nd1 / (F0 * sigmaSqrtT);
            unitVega  = df * F0 * nd1 * Math.sqrt(T);

            unitTheta = df * ( - (F0 * nd1 * sigma) / (2.0 * Math.sqrt(T))
                    + rRate * (K * Nminusd2 - F0 * Nminusd1));
        }

        // Simple rho for futures options: sensitivity of price to risk-free rate
        // For demo purposes, a consistent approximation:
        unitRho = -T * unitPrice;

        double price = unitPrice * contractSize;
        double delta = unitDelta * contractSize;
        double gamma = unitGamma * contractSize;
        double vega  = unitVega  * contractSize;
        double theta = unitTheta * contractSize;
        double rho   = unitRho   * contractSize;

        OptionResponse response = new OptionResponse();
        response.setInstrumentType(r.getInstrumentType());
        response.setOptionType(r.getOptionType());
        response.setSymbol(r.getSymbol());

        response.setPrice(price);
        response.setDelta(delta);
        response.setGamma(gamma);
        response.setVega(vega);
        response.setTheta(theta);
        response.setRho(rho);

        response.setExplanation("SOFR futures option priced using Black–76 model.");

        return response;
    }


    private double N(double x) {
        // Standard normal CDF using error function approximation
        return 0.5 * (1.0 + erf(x / Math.sqrt(2.0)));
    }

    private double n(double x) {
        // Standard normal PDF
        return (1.0 / Math.sqrt(2.0 * Math.PI)) * Math.exp(-0.5 * x * x);
    }

    private double erf(double x) {
        // Numerical approximation of error function
        // Abramowitz & Stegun formula 7.1.26
        double sign = x < 0 ? -1 : 1;
        x = Math.abs(x);

        double a1 = 0.254829592;
        double a2 = -0.284496736;
        double a3 = 1.421413741;
        double a4 = -1.453152027;
        double a5 = 1.061405429;
        double p = 0.3275911;

        double t = 1.0 / (1.0 + p * x);
        double y = 1.0 - (((((a5 * t + a4) * t) + a3) * t + a2) * t + a1) * t * Math.exp(-x * x);

        return sign * y;
    }


    private OptionResponse priceEquityOption(OptionRequest r) {

        double S     = r.getSpotPrice();
        double K     = r.getStrikePrice();
        double sigma = r.getVolatility();
        double T     = r.getTimeToMaturityYears();
        double rRate = r.getRiskFreeRate();
        boolean isCall = r.getOptionType() == OptionType.CALL;

        double contractSize = r.getNumberOfContracts() > 0 ? r.getNumberOfContracts() : 1.0;

        double sigmaSqrtT = sigma * Math.sqrt(T);
        double d1 = (Math.log(S / K) + (rRate + 0.5 * sigma * sigma) * T) / sigmaSqrtT;
        double d2 = d1 - sigmaSqrtT;

        double Nd1   = N(d1);
        double Nd2   = N(d2);
        double Nminusd1 = N(-d1);
        double Nminusd2 = N(-d2);
        double nd1   = n(d1);
        double df    = Math.exp(-rRate * T);

        // Unit price (per 1 underlying)
        double unitPrice;
        double unitDelta;
        double unitGamma;
        double unitVega;
        double unitTheta;
        double unitRho;

        if (isCall) {
            unitPrice = S * Nd1 - K * df * Nd2;

            unitDelta = Nd1;
            unitGamma = nd1 / (S * sigmaSqrtT);
            unitVega  = S * nd1 * Math.sqrt(T);

            // Theta (per year)
            unitTheta = - (S * nd1 * sigma) / (2.0 * Math.sqrt(T))
                    - rRate * K * df * Nd2;

            // Rho
            unitRho   = T * K * df * Nd2;

        } else {
            unitPrice = K * df * Nminusd2 - S * Nminusd1;

            unitDelta = Nd1 - 1.0; // or -N(-d1)
            unitGamma = nd1 / (S * sigmaSqrtT);
            unitVega  = S * nd1 * Math.sqrt(T);

            unitTheta = - (S * nd1 * sigma) / (2.0 * Math.sqrt(T))
                    + rRate * K * df * Nminusd2;

            unitRho   = - T * K * df * Nminusd2;
        }

        // Scale all by contract size
        double price = unitPrice * contractSize;
        double delta = unitDelta * contractSize;
        double gamma = unitGamma * contractSize;
        double vega  = unitVega  * contractSize;
        double theta = unitTheta * contractSize;
        double rho   = unitRho   * contractSize;

        OptionResponse response = new OptionResponse();
        response.setInstrumentType(r.getInstrumentType());
        response.setOptionType(r.getOptionType());
        response.setSymbol(r.getSymbol());

        response.setPrice(price);
        response.setDelta(delta);
        response.setGamma(gamma);
        response.setVega(vega);
        response.setTheta(theta);
        response.setRho(rho);
        response.setExplanation("Equity option priced using Black–Scholes model.");
        return response;
    }

    private OptionResponse priceBondFutureOption(OptionRequest r) {

        double F0    = r.getSpotPrice();          // bond futures price
        double K     = r.getStrikePrice();
        double sigma = r.getVolatility();
        double T     = r.getTimeToMaturityYears();
        double rRate = r.getRiskFreeRate();
        boolean isCall = r.getOptionType() == OptionType.CALL;

        double contractSize = r.getNumberOfContracts() > 0 ? r.getNumberOfContracts() : 1.0;

        double sigmaSqrtT = sigma * Math.sqrt(T);
        double d1 = (Math.log(F0 / K) + 0.5 * sigma * sigma * T) / sigmaSqrtT;
        double d2 = d1 - sigmaSqrtT;

        double Nd1   = N(d1);
        double Nd2   = N(d2);
        double Nminusd1 = N(-d1);
        double Nminusd2 = N(-d2);
        double nd1   = n(d1);
        double df    = Math.exp(-rRate * T);

        double unitPrice;
        double unitDelta;
        double unitGamma;
        double unitVega;
        double unitTheta;
        double unitRho;

        if (isCall) {
            unitPrice = df * (F0 * Nd1 - K * Nd2);

            unitDelta = df * Nd1;
            unitGamma = df * nd1 / (F0 * sigmaSqrtT);
            unitVega  = df * F0 * nd1 * Math.sqrt(T);

            unitTheta = df * ( - (F0 * nd1 * sigma) / (2.0 * Math.sqrt(T))
                    + rRate * (F0 * Nd1 - K * Nd2));
        } else {
            unitPrice = df * (K * Nminusd2 - F0 * Nminusd1);

            unitDelta = -df * Nminusd1;
            unitGamma = df * nd1 / (F0 * sigmaSqrtT);
            unitVega  = df * F0 * nd1 * Math.sqrt(T);

            unitTheta = df * ( - (F0 * nd1 * sigma) / (2.0 * Math.sqrt(T))
                    + rRate * (K * Nminusd2 - F0 * Nminusd1));
        }

        unitRho = -T * unitPrice;

        double price = unitPrice * contractSize;
        double delta = unitDelta * contractSize;
        double gamma = unitGamma * contractSize;
        double vega  = unitVega  * contractSize;
        double theta = unitTheta * contractSize;
        double rho   = unitRho   * contractSize;

        OptionResponse response = new OptionResponse();
        response.setInstrumentType(r.getInstrumentType());
        response.setOptionType(r.getOptionType());
        response.setSymbol(r.getSymbol());

        response.setPrice(price);
        response.setDelta(delta);
        response.setGamma(gamma);
        response.setVega(vega);
        response.setTheta(theta);
        response.setRho(rho);

        response.setExplanation("Bond futures option priced using Black–76 model.");

        return response;
    }


    private double normPdf(double x) {
        return Math.exp(-0.5 * x * x) / Math.sqrt(2.0 * Math.PI);
    }

    private double normCdf(double x) {
        return 0.5 * (1.0 + erf(x / Math.sqrt(2.0)));
    }

    private double theta(double S, double K, double r, double sigma, double T,
                         double d1, double d2, OptionType type) {

        double firstTerm = -(S * normPdf(d1) * sigma) / (2 * Math.sqrt(T));
        double secondTermCall = -r * K * Math.exp(-r * T) * normCdf(d2);
        double secondTermPut = r * K * Math.exp(-r * T) * normCdf(-d2);

        if (type == OptionType.CALL) {
            return firstTerm + secondTermCall;
        } else {
            return firstTerm + secondTermPut;
        }
    }
}
