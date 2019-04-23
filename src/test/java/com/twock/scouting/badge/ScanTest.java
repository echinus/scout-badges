package com.twock.scouting.badge;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.OngoingStubbing;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ScanTest {
    private static final String CHALLENGE_HTML = "/cubChallenge.html";
    private static final String ACTIVITY1_HTML = "/cubActivity1.html";
    private static final String ACTIVITY2_HTML = "/cubActivity2.html";
    private static final String CHALLENGE_CHIEFSCOUT_HTML = "/cubChallengeChiefScout.html";

    private static String readText(String file) {
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(ScanTest.class.getResource(file).toURI()));
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read " + file, e);
        }
    }

    @SuppressWarnings("unchecked")
    private Scan scan(String... testFiles) {
        BadgeConfig config = new BadgeConfig();
        config.setBaseUrl("http://127.0.0.1");
        Function<String, Document> mock = Mockito.mock(Function.class);
        OngoingStubbing<Document> stubbing = when(mock.apply(any()));
        for (String testFile : testFiles) {
            stubbing = stubbing.thenReturn(Jsoup.parse(readText(testFile)));
        }
        return new Scan(config, mock);
    }

    @Test
    public void testSingle() {
        List<Link> links = scan(CHALLENGE_HTML).badgeIndex("/");
        assertEquals(8, links.size());
        assertEquals("Chief Scout's Silver Award", links.get(0).getName());
    }

    @Test
    public void testMulti() {
        List<Link> links = scan(ACTIVITY1_HTML, ACTIVITY2_HTML).badgeIndex("/");
        assertEquals(38, links.size());
        assertEquals("World Faiths Activity Badge", links.get(links.size() - 1).getName());
    }

    @Test
    public void testContent() {
        Elements chiefScout = scan(CHALLENGE_CHIEFSCOUT_HTML).downloadBadgeInfo("/");
        assertTrue(chiefScout.toString().trim().startsWith("<h2>Chief Scout's Silver Award</h2>"));
    }
}
