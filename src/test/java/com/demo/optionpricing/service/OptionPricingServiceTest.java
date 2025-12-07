package com.demo.optionpricing.service;

import com.demo.optionpricing.model.InstrumentType;
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
        req.setTimeToMaturityYears(1.0);
        req.setInstrumentType(InstrumentType.SOFR_FUTURE_OPTION);
        req.setNumberOfContracts(100);

        var res = svc.price(req);
        double unitDelta = res.getDelta() / req.getNumberOfContracts();
        assertTrue(unitDelta > 0.0 && unitDelta < 1.0);
        assertTrue(res.getPrice() > 0.0);
    }
}
