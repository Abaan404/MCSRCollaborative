package com.abaan404.mcsrcollaborative.utils;

import com.abaan404.mcsrcollaborative.McsrCollaborative;
import com.google.gson.Gson;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class Http {
    private static final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    private static final Gson gson = new Gson();

    public static <T> CompletableFuture<T> get(String uri, Class<T> responseType) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> parseResponse(response, responseType));
    }

    private static <T> T parseResponse(HttpResponse<String> response, Class<T> responseType) {
        if (response.statusCode() >= 400) {
            McsrCollaborative.LOGGER.error("API Request to {} failed: {} {}", response.uri(), response.statusCode(), response.body());
            throw new RuntimeException("API returned error: " + response.statusCode() + " " + response.body());
        }
        return gson.fromJson(response.body(), responseType);
    }
}