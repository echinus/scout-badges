package com.twock.scouting.badge;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "badges")
@Data
public class BadgeConfig {
    private String baseUrl;
    private List<ScoutSection> sections;

    @Data
    public static class ScoutSection {
        private String name;
        private List<Link> badgeTypes;
    }
}
