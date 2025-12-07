package com.demo.optionpricing.model;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Data;
@Data
@Component
@ConfigurationProperties(prefix = "demo.slider")
public class SliderProperties {
    private boolean enabled = false;
    private int minPercent = 50;
    private int maxPercent = 150;
    private int debounceMs = 300;
}
