package com.demo.aiexplainoptionpricing.service;

import com.demo.aiexplainoptionpricing.model.OptionPricingResult;
import com.demo.aiexplainoptionpricing.model.OptionRequest;
import com.demo.aiexplainoptionpricing.model.OptionType;
import org.springframework.stereotype.Service;

@Service
public class OptionPricingService {

    public OptionPricingResult price(OptionRequest req) {
        double S = req.getSpotPrice();
        double K = req.getStrikePrice();
        double r = req.getRiskFreeRate();
        double sigma = req.getVolatility();
        double T = req.getTimeToMaturity();

        double sqrtT = Math.sqrt(T);
        double d1 = (Math.log(S / K) + (r + 0.5 * sigma * sigma) * T) / (sigma * sqrtT);
        double d2 = d1 - sigma * sqrtT;

        double Nd1 = normCdf(d1);
        double Nd2 = normCdf(d2);
        double NminusD1 = normCdf(-d1);
        double NminusD2 = normCdf(-d2);

        double price;
        double delta;
        if (req.getOptionType() == OptionType.CALL) {
            price = S * Nd1 - K * Math.exp(-r * T) * Nd2;
            delta = Nd1;
        } else {
            price = K * Math.exp(-r * T) * NminusD2 - S * NminusD1;
            delta = Nd1 - 1.0;
        }

        double gamma = normPdf(d1) / (S * sigma * sqrtT);
        double vega = S * normPdf(d1) * sqrtT / 100.0;
        double theta = theta(S, K, r, sigma, T, d1, d2, req.getOptionType()) / 365.0;

        OptionPricingResult result = new OptionPricingResult();
        result.setSymbol(req.getSymbol());
        result.setOptionType(req.getOptionType());
        result.setTheoreticalPrice(price);
        result.setDelta(delta);
        result.setGamma(gamma);
        result.setVega(vega);
        result.setTheta(theta);

        if (req.getMarketPrice() != null) {
            result.setMarketPrice(req.getMarketPrice());
            result.setMispricing(req.getMarketPrice() - price);
        }

        return result;
    }

    private double normPdf(double x) {
        return Math.exp(-0.5 * x * x) / Math.sqrt(2.0 * Math.PI);
    }

    private double normCdf(double x) {
        return 0.5 * (1.0 + erf(x / Math.sqrt(2.0)));
    }

    private double erf(double x) {
        double sign = Math.signum(x);
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
