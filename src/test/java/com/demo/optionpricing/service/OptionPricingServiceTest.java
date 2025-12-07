package com.demo.optionpricing.service;

import com.demo.optionpricing.model.OptionRequest;
import com.demo.optionpricing.model.OptionType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class OptionPricingServiceTest {

    @Test
    void testCallOptionHasPositiveDelta() {
        OptionPricingService svc = new OptionPricingService();
        OptionRequest req = new OptionRequest();
        req.setSymbol("TEST");
        req.setOptionType(OptionType.CALL);
        req.setSpotPrice(100.0);
        req.setStrikePrice(100.0);
        req.setRiskFreeRate(0.05);
        req.setVolatility(0.2);
        req.setTimeToMaturity(1.0);

        var res = svc.price(req);
        assertTrue(res.getDelta() > 0.0 && res.getDelta() < 1.0);
        assertTrue(res.getTheoreticalPrice() > 0.0);
    }
}
