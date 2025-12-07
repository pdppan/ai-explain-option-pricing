package com.demo.optionpricing.service;

import com.demo.optionpricing.model.OptionRequest;
import com.demo.optionpricing.model.OptionResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LlmExplanationService {

    private static final Logger log = LoggerFactory.getLogger(LlmExplanationService.class);

    // Local instances – no Spring bean / constructor dependency
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate;

    // Single source of truth for model name – controlled via application.properties
    // e.g. openai.model=gpt-4.1
    @Value("${openai.model}")
    private String modelName;

    // Optional logging toggle if you already have this property
    @Value("${openai.enable-logging}")
    private boolean enableLogging;

    // Fixed URL for the Chat Completions style endpoint
    // Adjust if you are using a different base URL.
    private static final String OPENAI_CHAT_URL = "https://api.openai.com/v1/chat/completions";
    public LlmExplanationService() {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000); // 10 seconds
        factory.setReadTimeout(15_000);   // 15 seconds
        this.restTemplate = new RestTemplate(factory);
    }
    /**
     * Build a human-readable explanation for the given priced option.
     */
    public String buildExplanation(OptionRequest request, OptionResponse result) {
        String apiKey = System.getenv("OPENAI_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("OPENAI_API_KEY not set. Returning placeholder explanation.");
            return "LLM explanation is not available because OPENAI_API_KEY is not configured. "
                    + "You can still use the numerical price and Greeks for analysis.";
        }

        try {
            String systemPrompt = buildSystemPrompt();
            String userPrompt = buildUserPrompt(request, result);

            log.info("Sending explanation request to LLM. Model: {}, Payload: {}", modelName, userPrompt);
            String explanation = callOpenAi(apiKey, systemPrompt, userPrompt);
            log.info("LLM explanation (first 200 chars): {}",
                    explanation != null && explanation.length() > 200
                            ? explanation.substring(0, 200)
                            : explanation);
            return explanation;
        } catch (ResourceAccessException e) {
                log.error("LLM call timed out or could not connect", e);
                return "The explanation service could not be reached. "
                        + "This is usually caused by network, firewall, or proxy restrictions. "
                        + "Pricing results are still valid.";

        } catch (Exception e) {
            log.error("Error while calling LLM for explanation", e);
            return "An error occurred while generating the explanation. "
                    + "Please review the numeric results (price and Greeks) above.";
        }
    }

    /**
     * Build the JSON payload we send as the user message content.
     */
    private String buildUserPrompt(OptionRequest req, OptionResponse resp) throws JsonProcessingException {
        Map<String, Object> root = new HashMap<>();

        // Request-side inputs
        root.put("instrumentType", req.getInstrumentType() != null ? req.getInstrumentType().name() : null);
        root.put("optionType", req.getOptionType() != null ? req.getOptionType().name() : null);
        root.put("symbol", req.getSymbol());

        root.put("spotPrice", req.getSpotPrice());
        root.put("strikePrice", req.getStrikePrice());
        root.put("volatility", req.getVolatility());
        root.put("riskFreeRate", req.getRiskFreeRate());
        root.put("timeToMaturityYears", req.getTimeToMaturityYears());
        root.put("numberOfContracts", req.getNumberOfContracts());

        // Model outputs (full OptionResponse)
        if (resp != null) {
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("modelPrice", resp.getPrice());
            outputs.put("delta", resp.getDelta());
            outputs.put("gamma", resp.getGamma());
            outputs.put("theta", resp.getTheta());
            outputs.put("vega", resp.getVega());
            outputs.put("rho", resp.getRho());

            outputs.put("instrumentType", resp.getInstrumentType() != null ? resp.getInstrumentType().name() : null);
            outputs.put("optionType", resp.getOptionType() != null ? resp.getOptionType().name() : null);
            outputs.put("symbol", resp.getSymbol());

            root.put("modelOutputs", outputs);
        }

        return objectMapper.writeValueAsString(root);
    }

    /**
     * Low-level call to OpenAI's chat completions endpoint.
     */
    @SuppressWarnings("unchecked")
    private String callOpenAi(String apiKey, String systemPrompt, String userPrompt) throws JsonProcessingException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> requestBody = new HashMap<>();

        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);

        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", userPrompt);

        requestBody.put("model", modelName);
        requestBody.put("messages", List.of(systemMessage, userMessage));
        requestBody.put("temperature", 0.2);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        log.info("Calling OpenAI endpoint {}", OPENAI_CHAT_URL);
        ResponseEntity<Object> responseEntity =
                restTemplate.exchange(OPENAI_CHAT_URL, HttpMethod.POST, entity, Object.class);
        log.info("Returned from OpenAI call");
        Object body = responseEntity.getBody();
        if (!(body instanceof Map<?, ?> map)) {
            return "Unexpected LLM response format.";
        }

        Object choicesObj = map.get("choices");
        if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) {
            return "LLM returned no choices in the response.";
        }

        Object first = choices.get(0);
        if (!(first instanceof Map<?, ?> firstMap)) {
            return "Unexpected LLM response format (choice not an object).";
        }

        Object messageObj = firstMap.get("message");
        if (!(messageObj instanceof Map<?, ?> msgMap)) {
            return "Unexpected LLM response format (no message object).";
        }

        Object contentObj = msgMap.get("content");
        if (contentObj instanceof String contentStr) {
            return contentStr;
        }

        return "LLM returned a response but no textual content could be parsed.";
    }

    /**
     * System prompt explaining how to interpret the JSON and how to format the answer.
     */
    private String buildSystemPrompt() {
        return "You are an options risk and pricing explainer for a junior quant or risk analyst.\n\n"
                + "You are given JSON describing a single option trade. The JSON includes:\n"
                + "- instrumentType: EQUITY_OPTION, BOND_FUTURE_OPTION, or SOFR_FUTURE_OPTION.\n"
                + "- optionType: CALL or PUT.\n"
                + "- symbol: a human-readable label for the underlying.\n"
                + "- spotPrice: the underlying price used in the model.\n"
                + "- strikePrice.\n"
                + "- volatility (annualised).\n"
                + "- riskFreeRate.\n"
                + "- timeToMaturityYears.\n"
                + "- contractSize.\n"
                + "- modelOutputs: modelPrice, delta, gamma, theta, vega, rho.\n\n"
                + "Interpretation guidelines:\n"
                + "- For EQUITY_OPTION, the underlying is a stock spot price and the option is priced with a Black–Scholes-style approach.\n"
                + "- For BOND_FUTURE_OPTION, the underlying is a bond futures price and the option is priced with a Black-76-style approach.\n"
                + "- For SOFR_FUTURE_OPTION, the underlying is a SOFR futures price and the option is priced with a Black-76-style approach.\n\n"
                + "Tasks:\n"
                + "1) Explain in intuitive language why the option has roughly this theoretical price, given moneyness, volatility, time to expiry, and contract size.\n"
                + "2) Explain what the Greeks (delta, gamma, theta, vega, rho) mean for this specific option and how they relate to the inputs.\n"
                + "3) Give the user 2–3 concrete risk management or scenario suggestions in bullet points (e.g., how the P&L or risk would change if the underlying moves or volatility changes).\n\n"
                + "IMPORTANT FORMAT INSTRUCTIONS:\n"
                + "- Write the answer in plain text, not Markdown.\n"
                + "- Do NOT use **bold**, headings (#), or any asterisk-based formatting.\n"
                + "- You may use simple numbered sections (1., 2., 3.) and hyphen bullets (-) only.\n";
    }
}
