package com.demo.optionpricing.service;

import com.demo.optionpricing.model.OptionResponse;
import com.demo.optionpricing.model.OptionRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LlmExplanationService {

    private static final Logger log = LoggerFactory.getLogger(LlmExplanationService.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${openai.model:gpt-4.1}")
    private String modelName;

    @Value("${openai.enable-logging:true}")
    private boolean enableLogging;

    public String buildExplanation(OptionRequest request, OptionResponse result) {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("OPENAI_API_KEY not set. Returning placeholder explanation.");
            return "LLM explanation is not available because OPENAI_API_KEY is not configured. " +
                   "Pricing and Greeks are still computed deterministically.";
        }
        try {
            String contextJson = buildContextJson(request, result);
            String prompt = buildPrompt(contextJson);

            if (enableLogging) {
                log.info("LLM request context JSON: {}", contextJson);
                log.info("LLM prompt: {}", prompt);
            }

            String url = "https://api.openai.com/v1/chat/completions";

            Map<String, Object> body = new HashMap<>();
            body.put("model", modelName);
            body.put("messages", List.of(
                Map.of("role", "system",
                       "content", "You are a senior derivatives risk manager. " +
                                  "Explain option pricing and risk in clear, actionable language for a " +
                                  "retail investor with some trading experience. Be concise but specific."),
                Map.of("role", "user", "content", prompt)
            ));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, Map.class);

            if (enableLogging) {
                log.info("LLM raw HTTP status: {}", response.getStatusCode());
                log.info("LLM raw response body: {}", response.getBody());
            }

            Map<?,?> respBody = response.getBody();
            if (respBody == null) {
                return "No response body from LLM.";
            }
            Object choicesObj = respBody.get("choices");
            if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) {
                return "No choices returned from LLM.";
            }
            Object first = choices.get(0);
            if (!(first instanceof Map<?,?> firstMap)) {
                return "Unexpected LLM response format.";
            }
            Object messageObj = firstMap.get("message");
            if (!(messageObj instanceof Map<?,?> msgMap)) {
                return "Unexpected LLM response format (no message).";
            }
            Object contentObj = msgMap.get("content");
            if (contentObj instanceof String contentStr) {
                return contentStr;
            }
            return "LLM returned response but no textual content field could be parsed.";
        } catch (Exception e) {
            log.error("Error while calling LLM for explanation", e);
            return "Unable to generate explanation due to an internal error. Please try again.";
        }
    }
    private String buildContextJson(OptionRequest req, OptionResponse res) throws JsonProcessingException {
        Map<String, Object> payload = new java.util.LinkedHashMap<>();

        payload.put("symbol", req.getSymbol());
        payload.put("optionType", req.getOptionType() != null ? req.getOptionType().name() : null);
        payload.put("instrumentType", res.getInstrumentType() != null
                ? res.getInstrumentType().name()
                : "EQUITY_OPTION");

        Map<String, Object> inputs = new java.util.LinkedHashMap<>();
        inputs.put("spotOrFuturesPrice", req.getSpotPrice());
        inputs.put("strikePrice", req.getStrikePrice());
        inputs.put("riskFreeRate", req.getRiskFreeRate());
        inputs.put("volatility", req.getVolatility());
        inputs.put("timeToMaturityYears", req.getTimeToMaturity());

        if (req.getMarketPrice() != null) {
            inputs.put("marketPrice", req.getMarketPrice());
        }

        Map<String, Object> blackScholes = new java.util.LinkedHashMap<>();
        blackScholes.put("theoreticalPrice", res.getTheoreticalPrice());
        blackScholes.put("delta", res.getDelta());
        blackScholes.put("gamma", res.getGamma());
        blackScholes.put("thetaPerDay", res.getTheta());
        blackScholes.put("vegaPer1PctVol", res.getVega());
        if (res.getMispricing() != null) {
            blackScholes.put("mispricing", res.getMispricing());
        }

        payload.put("inputs", inputs);
        payload.put("modelMetrics", blackScholes);

        return objectMapper.writeValueAsString(payload);
    }


    private String buildPrompt(String contextJson) {
        return "Here is the JSON describing an option and its pricing metrics:\n\n"
                + contextJson + "\n\n"
                + "The field 'instrumentType' is either 'EQUITY_OPTION' or 'BOND_FUTURE_OPTION'.\n"
                + "- For EQUITY_OPTION, the underlying is a stock price.\n"
                + "- For BOND_FUTURE_OPTION, the underlying is a bond futures price and the option is priced with a Black-76 style approach.\n\n"
                + "Tasks:\n"
                + "1) Explain why the option is priced at this level, focusing on volatility, time to expiry, and moneyness relative to the correct underlying.\n"
                + "2) Explain what the Greeks (delta, gamma, theta, vega) mean for this specific option.\n"
                + "3) If marketPrice and mispricing are present, comment on whether the option looks rich or cheap.\n"
                + "4) Give the user 2–3 concrete risk management suggestions in bullet points.\n\n"
                + "IMPORTANT FORMAT INSTRUCTIONS:\n"
                + "- Write the answer in plain text, not Markdown.\n"
                + "- Do NOT use **bold**, headings (#), or any asterisk-based formatting.\n"
                + "- You may use simple numbered sections (1., 2., 3.) and hyphen bullets (-) only.\n";
    }


}
