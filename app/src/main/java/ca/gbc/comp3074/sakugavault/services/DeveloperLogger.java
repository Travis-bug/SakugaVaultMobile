package ca.gbc.comp3074.sakugavault.services;

import android.content.Context;
import org.json.JSONObject;
import java.io.File;
import java.io.FileWriter;
import ca.gbc.comp3074.sakugavault.data.local.WatchListEntry;

public class DeveloperLogger {

    // 3. The helper method that builds and saves the JSON
    public static void logErrorToJsonFile(Context context, WatchListEntry entry, Exception e) {
        try {
            // Build the JSON Object matching your structure
            JSONObject errorJson = new JSONObject();
            errorJson.put("type", "Error Message");

            // Format the message with the entry data and exception detail
            String errorMessage = String.format("Failed to extract ID from link for entry: %s. Exception: %s",
                    entry.toString(), e.getMessage());
            errorJson.put("message", errorMessage);

            errorJson.put("origin", "idFromLink(WatchListEntry entry)");
            errorJson.put("inputType", "String");

            // Save to a file named "error_logs.json" in the app's internal storage
            File logFile = new File(context.getFilesDir(), "error_logs.json");

            // true = append to the end of the file instead of overwriting
            try (FileWriter writer = new FileWriter(logFile, true)) {
                // Write the JSON as a single line, followed by a newline character
                writer.append(errorJson.toString()).append("\n");
                writer.flush();
            }

        } catch (Exception jsonException) {
            // If the JSON writing fails, fall back to standard Android Logcat so you don't lose it entirely
            android.util.Log.e("ErrorLogger", "Failed to write error to JSON file", jsonException);
        }
    }
}
