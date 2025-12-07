package com.demo.aiexplainoptionpricing.controller;

import com.demo.aiexplainoptionpricing.model.OptionPricingResult;
import com.demo.aiexplainoptionpricing.model.OptionRequest;
import com.demo.aiexplainoptionpricing.model.OptionType;
import com.demo.aiexplainoptionpricing.service.LlmExplanationService;
import com.demo.aiexplainoptionpricing.service.OptionPricingService;
import jakarta.validation.Valid;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/options")
public class OptionExplainController {

    private final OptionPricingService pricingService;
    private final LlmExplanationService llmService;

    public OptionExplainController(OptionPricingService pricingService,
                                   LlmExplanationService llmService) {
        this.pricingService = pricingService;
        this.llmService = llmService;
    }

    @PostMapping("/explain")
    public OptionPricingResult explain(@Valid @RequestBody OptionRequest request) {
        OptionPricingResult result = pricingService.price(request);
        String explanation = llmService.buildExplanation(request, result);
        result.setLlmExplanation(explanation);
        return result;
    }

    @GetMapping("/batch-explain-sample")
    public List<OptionPricingResult> batchExplainSample() throws IOException {
        List<OptionPricingResult> results = new ArrayList<>();
        ClassPathResource resource = new ClassPathResource("sample-options.csv");
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (first) {
                    first = false;
                    continue; // header
                }
                String[] parts = line.split(",");
                if (parts.length < 8) continue;

                OptionRequest req = new OptionRequest();
                req.setSymbol(parts[0]);
                req.setOptionType("CALL".equalsIgnoreCase(parts[1]) ?
                    OptionType.CALL : OptionType.PUT);
                req.setSpotPrice(Double.parseDouble(parts[2]));
                req.setStrikePrice(Double.parseDouble(parts[3]));
                req.setRiskFreeRate(Double.parseDouble(parts[4]));
                req.setVolatility(Double.parseDouble(parts[5]));
                req.setTimeToMaturity(Double.parseDouble(parts[6]));
                req.setMarketPrice(Double.parseDouble(parts[7]));

                OptionPricingResult res = pricingService.price(req);
                res.setLlmExplanation(llmService.buildExplanation(req, res));
                results.add(res);
            }
        }
        return results;
    }
}
