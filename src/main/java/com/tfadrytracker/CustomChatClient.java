package com.tfadrytracker;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import net.runelite.http.api.chat.Duels;
import okhttp3.*;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class CustomChatClient
{
    private final OkHttpClient client;
    private final HttpUrl apiBase;
    private final Gson gson;

    public static class StreakPair {
        public StreakPair(int personal, int team) {
            this.personal = personal;
            this.team = team;
        }
        int personal;
        int team;
    }

    @Inject
    private CustomChatClient(OkHttpClient client, @Named("runelite.api.base") HttpUrl apiBase, Gson gson)
    {
        this.client = client;
        this.apiBase = apiBase;
        this.gson = gson;
    }
    public String submitMemory(String username, StreakPair streak) throws IOException
    {
        HttpUrl url = apiBase.newBuilder()
                .addPathSegment("chat")
                .addPathSegment("duels")
                .addQueryParameter("name", username)
                .addQueryParameter("wins", Integer.toString(streak.personal))
                .addQueryParameter("losses", Integer.toString(streak.team))
                .addQueryParameter("winningStreak", "0")
                .addQueryParameter("losingStreak", "0")
                .build();

        Request request = new Request.Builder()
                .post(RequestBody.create(null, new byte[0]))
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute())
        {
            return response.body().string();
        }
    }

    public StreakPair getMemory(String username) throws IOException {
        HttpUrl url = apiBase.newBuilder()
                .addPathSegment("chat")
                .addPathSegment("duels")
                .addQueryParameter("name", username)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("unable to lookup memory");
            }

            InputStream in = response.body().byteStream();
            Duels duels = gson.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), Duels.class);

            String lastPersonalItem;

            return new StreakPair(duels.getWins(), duels.getLosses());
        } catch (JsonParseException ex) {
            throw new IOException(ex);
        }
    }
}
