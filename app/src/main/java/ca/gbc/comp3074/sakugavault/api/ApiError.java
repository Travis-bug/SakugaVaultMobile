package ca.gbc.comp3074.sakugavault.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import retrofit2.Response;

/**
 * A failed API call, turned into a message that is safe to show to the user.
 * Same rules as the website's src/lib/api.ts: the first validation message wins,
 * then the server's "detail", then its "title", then a message for the status code.
 */
public final class ApiError {

    /** HTTP status code, or 0 when the server could not be reached at all. */
    public final int status;
    public final String message;

    public ApiError(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public boolean isUnauthorized() {
        return status == 401;
    }

    public boolean isNetworkError() {
        return status == 0;
    }

    public static ApiError fromResponse(Response<?> response) {
        String body = null;
        try {
            if (response.errorBody() != null) {
                body = response.errorBody().string();
            }
        } catch (IOException ignored) {
            // No readable body; the status code message is used instead.
        }
        return new ApiError(response.code(), buildMessage(response.code(), body));
    }

    public static ApiError fromFailure(Throwable throwable) {
        return new ApiError(0, "Can't reach the SakugaVault server. Check that the back end is running, then try again.");
    }

    static String buildMessage(int status, String body) {
        if (body != null && !body.trim().isEmpty()) {
            try {
                JsonElement root = JsonParser.parseString(body);
                if (root.isJsonObject()) {
                    JsonObject problem = root.getAsJsonObject();
                    String validation = firstValidationMessage(problem);
                    if (validation != null) {
                        return validation;
                    }
                    String detail = stringField(problem, "detail");
                    if (detail != null && !looksTechnical(detail)) {
                        return detail;
                    }
                    String title = stringField(problem, "title");
                    if (title != null && !looksTechnical(title)) {
                        return title;
                    }
                }
            } catch (RuntimeException ignored) {
                // Not JSON (for example an HTML error page). Fall through to the status message.
            }
        }
        return messageForStatus(status);
    }

    private static String firstValidationMessage(JsonObject problem) {
        if (!problem.has("errors") || !problem.get("errors").isJsonObject()) {
            return null;
        }
        for (Map.Entry<String, JsonElement> entry : problem.getAsJsonObject("errors").entrySet()) {
            if (!entry.getValue().isJsonArray()) {
                continue;
            }
            for (JsonElement message : entry.getValue().getAsJsonArray()) {
                if (message.isJsonPrimitive() && !message.getAsString().trim().isEmpty()) {
                    return message.getAsString().trim();
                }
            }
        }
        return null;
    }

    private static String stringField(JsonObject object, String name) {
        if (!object.has(name) || !object.get(name).isJsonPrimitive()) {
            return null;
        }
        String value = object.get(name).getAsString().trim();
        return value.isEmpty() ? null : value;
    }

    private static boolean looksTechnical(String message) {
        String m = message.trim().toLowerCase(Locale.ROOT);
        return m.startsWith("<!doctype html") || m.startsWith("<html")
                || m.contains("sakugavault.contracts") || m.contains("record type")
                || m.contains("stack trace") || m.contains("system.") || m.contains("exception");
    }

    private static String messageForStatus(int status) {
        if (status == 400) {
            return "Some fields need attention. Review your input and try again.";
        }
        if (status == 401) {
            return "Your session or credentials were rejected. Try signing in again.";
        }
        if (status == 403) {
            return "You do not have permission to do that.";
        }
        if (status == 404) {
            return "The requested item could not be found.";
        }
        if (status == 409) {
            return "That request conflicts with existing data. Review your input and try again.";
        }
        if (status == 429) {
            return "Too many attempts were made. Wait a moment and try again.";
        }
        if (status >= 500) {
            return "The server hit a problem. Please try again in a moment.";
        }
        return "Request failed with status " + status;
    }
}
