package ca.gbc.comp3074.sakugavault.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;

import org.junit.Test;

import ca.gbc.comp3074.sakugavault.api.dto.AuthResponseDto;
import ca.gbc.comp3074.sakugavault.api.dto.CatalogSearchResponseDto;
import ca.gbc.comp3074.sakugavault.api.dto.LoginRequestDto;
import ca.gbc.comp3074.sakugavault.api.dto.UpsertWatchHistoryRequestDto;
import ca.gbc.comp3074.sakugavault.api.dto.WatchPageDto;

/** Unit tests: the JSON the back end sends maps onto the app's DTO classes (and back). */
public class DtoJsonTest {

    private final Gson gson = new Gson();

    @Test
    public void authResponseParses() {
        String json = "{\"accessToken\":\"abc\",\"accessTokenExpiresAtUtc\":\"2026-11-11T20:00:00Z\","
                + "\"user\":{\"id\":\"u1\",\"displayName\":\"Travis\",\"userName\":\"travis\",\"email\":\"t@example.com\"}}";
        AuthResponseDto dto = gson.fromJson(json, AuthResponseDto.class);
        assertEquals("abc", dto.accessToken);
        assertNotNull(dto.user);
        assertEquals("u1", dto.user.id);
        assertEquals("travis", dto.user.userName);
    }

    @Test
    public void searchResponseParses() {
        String json = "{\"query\":\"frieren\",\"totalResults\":1,\"results\":[{\"id\":\"a1\",\"title\":\"Frieren\","
                + "\"episodeCount\":28,\"subAvailable\":true,\"dubAvailable\":false,\"genres\":[\"Adventure\"]}]}";
        CatalogSearchResponseDto dto = gson.fromJson(json, CatalogSearchResponseDto.class);
        assertEquals(1, dto.totalResults);
        assertEquals("Frieren", dto.results.get(0).title);
        assertTrue(dto.results.get(0).subAvailable);
        assertFalse(dto.results.get(0).dubAvailable);
        assertEquals("Adventure", dto.results.get(0).genres.get(0));
    }

    @Test
    public void watchPageIgnoresUnknownFields() {
        // The back end can add fields the app doesn't know yet; Gson must skip them.
        String json = "{\"id\":\"a1\",\"title\":\"X\",\"somethingNew\":123,\"seasons\":[],\"comments\":[]}";
        WatchPageDto dto = gson.fromJson(json, WatchPageDto.class);
        assertEquals("a1", dto.id);
        assertTrue(dto.seasons.isEmpty());
    }

    @Test
    public void requestBodiesUseTheBackEndsFieldNames() {
        assertEquals("{\"identifier\":\"travis\",\"password\":\"secret123\"}",
                gson.toJson(new LoginRequestDto("travis", "secret123")));
        String progress = gson.toJson(new UpsertWatchHistoryRequestDto("a1", 3, 120, 1440, false));
        assertTrue(progress.contains("\"animeId\":\"a1\""));
        assertTrue(progress.contains("\"episodeNumber\":3"));
        assertTrue(progress.contains("\"positionSeconds\":120"));
        assertTrue(progress.contains("\"durationSeconds\":1440"));
        assertTrue(progress.contains("\"completed\":false"));
    }
}
