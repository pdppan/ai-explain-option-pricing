package com.demo.optionpricing.controller;

import com.demo.optionpricing.model.SliderProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class UiConfigController {

    private final SliderProperties sliderProperties;

    public UiConfigController(SliderProperties sliderProperties) {
        this.sliderProperties = sliderProperties;
    }

    @GetMapping("/api/options/ui-config")
    public Map<String, Object> getUiConfig() {
        return Map.of(
                "sliderEnabled", sliderProperties.isEnabled(),
                "sliderMinPercent", sliderProperties.getMinPercent(),
                "sliderMaxPercent", sliderProperties.getMaxPercent(),
                "sliderDebounceMs", sliderProperties.getDebounceMs()
        );
    }
}
