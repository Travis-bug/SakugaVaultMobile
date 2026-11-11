package ca.gbc.comp3074.sakugavault.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;

/** Unit tests: how a failed API answer is turned into a message for the user. */
public class ApiErrorTest {

    @Test
    public void validationMessageWinsOverDetailAndTitle() {
        String body = "{\"title\":\"One or more validation errors occurred.\","
                + "\"detail\":\"See errors\","
                + "\"errors\":{\"Password\":[\"Password must be at least 8 characters long.\"]}}";
        assertEquals("Password must be at least 8 characters long.", ApiError.buildMessage(400, body));
    }

    @Test
    public void detailIsUsedWhenThereAreNoValidationErrors() {
        String body = "{\"title\":\"Unauthorized\",\"detail\":\"Invalid username/email or password.\",\"status\":401}";
        assertEquals("Invalid username/email or password.", ApiError.buildMessage(401, body));
    }

    @Test
    public void titleIsUsedWhenDetailIsMissing() {
        assertEquals("Anime not found.", ApiError.buildMessage(404, "{\"title\":\"Anime not found.\"}"));
    }

    @Test
    public void technicalDetailFallsBackToStatusMessage() {
        String body = "{\"detail\":\"System.NullReferenceException: Object reference not set\"}";
        assertEquals("The server hit a problem. Please try again in a moment.", ApiError.buildMessage(500, body));
    }

    @Test
    public void htmlBodyFallsBackToStatusMessage() {
        // What Jenkins (or any web server) sends when it is sitting on port 8080 instead of the API.
        String body = "<!DOCTYPE html><html><body>Forbidden</body></html>";
        assertEquals("You do not have permission to do that.", ApiError.buildMessage(403, body));
    }

    @Test
    public void emptyBodyUsesStatusMessages() {
        assertEquals("Some fields need attention. Review your input and try again.", ApiError.buildMessage(400, null));
        assertEquals("Your session or credentials were rejected. Try signing in again.", ApiError.buildMessage(401, ""));
        assertEquals("The requested item could not be found.", ApiError.buildMessage(404, "   "));
        assertEquals("That request conflicts with existing data. Review your input and try again.", ApiError.buildMessage(409, null));
        assertEquals("Too many attempts were made. Wait a moment and try again.", ApiError.buildMessage(429, null));
        assertEquals("The server hit a problem. Please try again in a moment.", ApiError.buildMessage(503, null));
        assertEquals("Request failed with status 418", ApiError.buildMessage(418, null));
    }

    @Test
    public void networkFailureHasStatusZero() {
        ApiError error = ApiError.fromFailure(new IOException("connect timed out"));
        assertTrue(error.isNetworkError());
        assertFalse(error.isUnauthorized());
        assertEquals(0, error.status);
        assertTrue(error.message.contains("Can't reach the SakugaVault server"));
    }

    @Test
    public void unauthorizedFlag() {
        assertTrue(new ApiError(401, "x").isUnauthorized());
        assertFalse(new ApiError(403, "x").isUnauthorized());
    }
}
