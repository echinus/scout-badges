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

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static java.nio.charset.StandardCharsets.UTF_8;

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
        Scan scan = new SpringApplication(Scan.class).run(args).getBean(Scan.class);
        List<SectionBadgeContent> data = scan.scan();
        scan.writeToDisk(data);
    }

    public List<SectionBadgeContent> scan() {
        List<SectionBadgeContent> result = new ArrayList<>();
        for (BadgeConfig.ScoutSection section : badgeConfig.getSections()) {
            SectionBadgeContent sectionContent = new SectionBadgeContent(section);
            for (Link badgeType : section.getBadgeTypes()) {
                TypeBadgeContent typeContent = new TypeBadgeContent(sectionContent, badgeType);
                List<Link> links = badgeIndex(badgeType.getUrl());
                log.info("Section={}, Type={}, {} badges found", section.getName(), badgeType.getName(), links.size());
                for (Link badge : links) {
                    Elements content = downloadBadgeInfo(badge.getUrl());
                    typeContent.getContent().add(new BadgeContent(typeContent, badge, content));
                }
                sectionContent.getContent().add(typeContent);
            }
            result.add(sectionContent);
        }
        return result;
    }

    public void writeToDisk(List<SectionBadgeContent> data) {
        String template;
        try {
            template = Files.readString(Paths.get(getClass().getResource("/template.html").toURI()), UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve template.html", e);
        }
        for (SectionBadgeContent section : data) {
            File outputDir = new File("out/" + section.getSection().getName());
            if (!outputDir.exists()) {
                if (!outputDir.mkdirs()) {
                    log.warn("Unable to create directory: " + outputDir.getAbsolutePath());
                }
            }
            for (TypeBadgeContent badgeType : section.getContent()) {
                StringBuilder sb = new StringBuilder();
                for (BadgeContent badge : badgeType.getContent()) {
                    sb.append(badge.getContent().toString()
                            .replaceAll("src=\"/", "src=\"" + badgeConfig.getBaseUrl() + "/")
                            .replaceAll("href=\"/", "href=\"" + badgeConfig.getBaseUrl() + "/")
                    );
                }

                StringBuilder navbar = new StringBuilder();
                for (SectionBadgeContent navSection : data) {
                    for (TypeBadgeContent navType : navSection.getContent()) {
                        if (navSection.equals(section) && navType.equals(badgeType)) {
                            navbar.append("<li> ")
                                    .append(navSection.getSection().getName())
                                    .append(" ")
                                    .append(navType.getBadgeType().getName())
                                    .append(" </li>");
                        } else {
                            navbar.append("<li><a href=\"../")
                                    .append(navSection.getSection().getName())
                                    .append("/")
                                    .append(navType.getBadgeType().getName())
                                    .append(".html\" class=\"main divider\"> ")
                                    .append(navSection.getSection().getName())
                                    .append(" ")
                                    .append(navType.getBadgeType().getName())
                                    .append(" </a></li>");
                        }
                    }
                }

                String fileContent = template
                        .replace("{{content}}", sb.toString())
                        .replace("{{sectionName}}", section.getSection().getName())
                        .replace("{{badgeType}}", badgeType.getBadgeType().getName())
                        .replace("{{navbar}}", navbar);

                File outputFile = new File(outputDir, badgeType.getBadgeType().getName() + ".html");
                try (Writer out = new FileWriter(outputFile, UTF_8)) {
                    out.write(fileContent);
                } catch (Exception e) {
                    throw new RuntimeException("Unable to write to " + outputFile.getAbsolutePath(), e);
                }
                log.info("Wrote " + outputFile.getAbsolutePath());
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

    public Elements downloadBadgeInfo(String url) {
        log.info("Retrieving " + url);
        Document document = retriever.apply(url);
        Elements content = document.select("div[class=seven columns]");
        if (content.isEmpty()) {
            throw new RuntimeException("Failed to find content in " + url);
        }
        return content.first().children();
    }

    @Data
    public static class SectionBadgeContent {
        private final BadgeConfig.ScoutSection section;
        private final List<TypeBadgeContent> content = new ArrayList<>();
    }

    @Data
    public static class TypeBadgeContent {
        private final SectionBadgeContent sectionContent;
        private final Link badgeType;
        private final List<BadgeContent> content = new ArrayList<>();
    }

    @Data
    public static class BadgeContent {
        private final TypeBadgeContent typeContent;
        private final Link badge;
        private final Elements content;
    }
}
