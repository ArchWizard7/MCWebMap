package me.ArchWizard7.MCWebMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class TestConnection {
    public static void main(String[] args) throws IOException {
        String path = "http://localhost:5000/register-tile?id=0&registered=31528c4f-241e-4f5c-805e-b7cf482c95ad";
        URL url = new URL(path);

        // Create HttpURLConnection object
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        // Get response code
        int responseCode = connection.getResponseCode();
        System.out.println("Response Code: " + responseCode);

        // Read response
        // BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        // String line;
        // StringBuilder response = new StringBuilder();
        //
        // while ((line = reader.readLine()) != null) {
        //     response.append(line);
        // }

        // reader.close();

        // System.out.println("[Response Body]");
        // System.out.println(response);

        connection.disconnect();
    }
}
