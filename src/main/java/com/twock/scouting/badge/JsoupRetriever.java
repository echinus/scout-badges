package com.twock.scouting.badge;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.function.Function;

@Component
public class JsoupRetriever implements Function<String, Document> {
    @Override
    public Document apply(String url) {
        try {
            return Jsoup.connect(url).get();
        } catch (IOException e) {
            throw new RuntimeException("Failed to connect to " + url, e);
        }
    }
}
