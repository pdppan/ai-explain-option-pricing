# AI Explain Option Pricing – Explainable Options Pricing Engine

## Executive Pitch

### Problem

Options traders – especially advanced retail and junior professionals – can see the option price and the Greeks, but they rarely understand **why** the contract is priced the way it is, or what is really driving risk.

Most tools:

- Dump Black–Scholes outputs
- Show Δ, Γ, Θ, Vega
- Leave the human to interpret everything alone

This creates **hidden risk**, poor sizing, and emotionally driven trades.

### Solution

**AI Explain Option Pricing** is a Spring Boot microservice that combines:

1. **Deterministic Black–Scholes pricing and Greeks**
2. A **Large Language Model “quant mentor”** (via OpenAI Chat Completions API) that reads the pricing context and returns a plain-English, trader-grade explanation.

Given any single option (symbol, strikes, maturity, volatility, market price), the engine returns:

- Theoretical fair value
- Greeks (Δ, Γ, Θ, Vega)
- Mispricing vs current market premium
- A **natural-language explanation** of:
  - What is driving the price
  - How sensitive the option is to moves and volatility
  - Whether it looks rich or cheap
  - 2–3 concrete risk-management suggestions

The LLM is **not** used as a calculator. It is a **reasoning and explanation layer** on top of a transparent quant engine.

---

## High-Level Architecture

```text
Client (UI / Postman / index.html)
      |
      v
Spring Boot REST API  (/api/options/explain)
      |
      v
OptionPricingService  (Black–Scholes + Greeks)
      |
      v
JSON Pricing Context
      |
      v
LlmExplanationService  (calls OpenAI Chat Completions API)
      |
      v
Combined Response: Numbers + Narrative
```

---

## API Summary

### POST http://localhost:8080/api/options/explain

**Request body**

```json
{
  "symbol": "AAPL",
  "optionType": "CALL",
  "spotPrice": 195.0,
  "strikePrice": 200.0,
  "riskFreeRate": 0.045,
  "volatility": 0.25,
  "timeToMaturity": 0.25,
  "marketPrice": 7.80
}
```

### GET `/api/options/batch-explain-sample`

Runs explanations against the bundled `sample-options.csv` file and returns a list.

---

## Logging and Observability

The service logs **every LLM call** at INFO level (without API keys):

- JSON context passed to the model
- Natural-language prompt string
- HTTP status from OpenAI
- Raw JSON body from OpenAI
- Extracted explanation text (if any)

Example log excerpt:

```text
2025-12-06 10:15:32 INFO  LlmExplanationService - LLM request context JSON: {...}
2025-12-06 10:15:32 INFO  LlmExplanationService - LLM prompt: Here is the JSON describing...
2025-12-06 10:15:33 INFO  LlmExplanationService - LLM raw HTTP status: 200 OK
2025-12-06 10:15:33 INFO  LlmExplanationService - LLM raw response body: {"choices": ...}
2025-12-06 10:15:33 INFO  LlmExplanationService - LLM explanation text: This call option is priced...
```

If `OPENAI_API_KEY` is not set, the service returns a clear placeholder explanation and still logs a warning.

---

## Technology Stack

- **Language**: Java 17
- **Framework**: Spring Boot 3 (spring-boot-starter-web)
- **AI Client**: Direct HTTPS call to OpenAI Chat Completions (`/v1/chat/completions`) using `RestTemplate`
- **Pricing**: Black–Scholes for European calls/puts + Greeks (Δ, Γ, Θ, Vega)
- **Logging**: SLF4J + Logback via Spring Boot

---

## Getting Started

### 1. Prerequisites

- Java 17+
- Maven 3.9+
- An OpenAI API key with access to GPT-4.1 family

### 2. Build

```bash
mvn clean package
```

### 3. Configure OpenAI credentials

The service reads the key from the `OPENAI_API_KEY` environment variable:

```bash
export OPENAI_API_KEY=sk-...
```

You can change the model via `openai.model` in `application.properties` (default `gpt-4.1`).

### 4. Run

```bash
mvn spring-boot:run
```

The service runs on `http://localhost:8080`.

### 5. Test the main endpoint

```bash
curl -X POST http://localhost:8080/api/options/explain   -H "Content-Type: application/json"   -d '{
    "symbol": "AAPL",
    "optionType": "CALL",
    "spotPrice": 195.0,
    "strikePrice": 200.0,
    "riskFreeRate": 0.045,
    "volatility": 0.25,
    "timeToMaturity": 0.25,
    "marketPrice": 7.80
  }'
```

### 6. Run sample batch

```bash
curl http://localhost:8080/api/options/batch-explain-sample
```

---

## Included Frontend for demo

This repository includes a lightweight frontend for demos:

```text
.. springboot resources/static index.html
```

### Usage

1. Start the backend:
   ```bash
   mvn spring-boot:run
   ```
2. Open `http://localhost:8080/index.html` in your browser.
3. Click **Load Demo Scenario** to auto-fill a realistic call option.
4. Click **Explain Option** to see:
   - Pricing & Greeks
   - LLM explanation
   - Payoff chart
5. Use the **spot move slider** to show “why the price changed”.

The frontend is intentionally framework-free (HTML + JS only) so judges can
focus on the AI and pricing logic rather than tooling.

---

## 60-Second Pitch

> “We built **AI Explain Option Pricing**, an explainable options pricing engine.  
> Under the hood it runs a transparent Black–Scholes model for any option you send it.  
> On top of that, a GPT-4.1 model reads the full pricing context – including Greeks and mispricing – and returns a **trader-grade explanation** of what’s driving the price and risk, plus concrete risk-management suggestions.  
> Instead of staring at Greeks, a trader or PM clicks once and gets a narrative they can act on.  
> It’s auditable math plus an AI quant mentor, exposed as a simple REST microservice that any trading or education platform can plug into.”

---
