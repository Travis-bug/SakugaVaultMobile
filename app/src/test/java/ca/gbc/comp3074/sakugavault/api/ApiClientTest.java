package ca.gbc.comp3074.sakugavault.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import ca.gbc.comp3074.sakugavault.BuildConfig;

/** Unit tests: turning the back end's relative image links into full URLs. */
public class ApiClientTest {

    @Test
    public void absoluteUrlsAreLeftAlone() {
        assertEquals("https://cdn.example.com/a.jpg", ApiClient.absoluteUrl("https://cdn.example.com/a.jpg"));
        assertEquals("http://x/y.png", ApiClient.absoluteUrl("http://x/y.png"));
    }

    @Test
    public void relativeUrlsGetTheApiBase() {
        String base = BuildConfig.API_BASE_URL; // ends with "/"
        assertEquals(base + "images/x.jpg", ApiClient.absoluteUrl("/images/x.jpg"));
        assertEquals(base + "images/x.jpg", ApiClient.absoluteUrl("images/x.jpg"));
    }

    @Test
    public void emptyUrlsBecomeNull() {
        assertNull(ApiClient.absoluteUrl(null));
        assertNull(ApiClient.absoluteUrl(""));
        assertNull(ApiClient.absoluteUrl("   "));
    }
}
