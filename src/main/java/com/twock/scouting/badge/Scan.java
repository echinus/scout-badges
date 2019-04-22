package com.twock.scouting.badge;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Slf4j
@SpringBootApplication
@EnableConfigurationProperties(BadgeConfig.class)
@Data
public class Scan {
    private final Function<String, Document> retriever;
    private final BadgeConfig badgeConfig;

    @Autowired
    public Scan(BadgeConfig badgeConfig, Function<String, Document> retriever) {
        this.badgeConfig = badgeConfig;
        this.retriever = retriever;
    }

    public static void main(String[] args) {
        new SpringApplication(Scan.class).run(args).getBean(Scan.class).scan();
    }

    public void scan() {
        for (BadgeConfig.ScoutSection section : badgeConfig.getSections()) {
            for (Link badgeType : section.getBadgeTypes()) {
                List<Link> links = badgeIndex(badgeType.getUrl());
                log.info("Section={}, Type={}, {} badges found", section.getName(), badgeType.getName(), links.size());
            }
        }
    }

    public List<Link> badgeIndex(String url) {
        List<Link> result = new ArrayList<>();
        String nextPage = url;
        while (nextPage != null) {
            String targetUrl = nextPage.startsWith("http") ? nextPage : badgeConfig.getBaseUrl() + nextPage;
            log.info("Retrieving {}", targetUrl);
            Document doc = retriever.apply(targetUrl);
            Element badgeTable = doc.getElementsByAttributeValue("summary", "Badges tables").first();
            Elements badgeLinks = badgeTable.select("a > div");
            for (Element badgeLink : badgeLinks) {
                String linkSource = badgeConfig.getBaseUrl() + badgeLink.parent().attr("href");
                String linkText = badgeLink.text();
                log.info("{}: {}", linkText, linkSource);
                result.add(new Link(linkText, linkSource));
            }
            String pageBase = targetUrl.replaceFirst("\\?.*", "");
            nextPage = null;
            Elements nextPageLink = doc.getElementsContainingOwnText("Next >");
            for (Element element : nextPageLink) {
                if ("a".equals(element.tagName())) {
                    nextPage = pageBase + element.attr("href");
                    break;
                }
            }
        }
        return result;
    }

    private void downloadBadgeInfo(String linkSource) {

    }
}
