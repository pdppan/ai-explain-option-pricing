package com.demo.optionpricing.controller;

import com.demo.optionpricing.model.OptionResponse;
import com.demo.optionpricing.model.OptionRequest;
import com.demo.optionpricing.model.OptionType;
import com.demo.optionpricing.service.LlmExplanationService;
import com.demo.optionpricing.service.OptionPricingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/api/options")
public class OptionController {
    @Autowired
    private HttpServletRequest httpRequest;

    private final OptionPricingService pricingService;
    private final LlmExplanationService llmService;
    private HashMap<OptionRequest,OptionResponse> resultCache = new HashMap<OptionRequest,OptionResponse>();
    private static final Logger log = LoggerFactory.getLogger(OptionController.class);
    public OptionController(OptionPricingService pricingService,
                            LlmExplanationService llmService) {
        this.pricingService = pricingService;
        this.llmService = llmService;
    }

    @PostMapping("/explain")
    public OptionResponse explain(@Valid @RequestBody OptionRequest request) {
        logCaller(httpRequest);
        if(!resultCache.containsKey(request)){
            OptionResponse response = pricingService.price(request);
            String explanation = llmService.buildExplanation(request, response);
            response.setExplanation(explanation);
            if (explanation != null && explanation.startsWith("Sorry")) {
                return response;
            }
            resultCache.put(request, response);
        }
        return resultCache.get(request);
    }

    @GetMapping("/batch-explain-sample")
    public List<OptionResponse> batchExplainSample() throws IOException {
        List<OptionResponse> results = new ArrayList<>();
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
                req.setTimeToMaturityYears(Double.parseDouble(parts[6]));
                //req.setPrice(Double.parseDouble(parts[7]));

                OptionResponse res = pricingService.price(req);
                res.setExplanation(llmService.buildExplanation(req, res));
                results.add(res);
            }
        }
        return results;
    }

    public void logCaller(HttpServletRequest req) {
        //for usage audit purpose
        String ip = req.getRemoteAddr();
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
           ip = xff.split(",")[0].trim();
        }
        String agent = httpRequest.getHeader("User-Agent");
        log.info("Explain API called from IP={} UserAgent={}", ip, agent);
    }
}
