package com.romys.tiktok.profile.extract.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class App {
    private OkHttpClient client = new OkHttpClient();
    private ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {
        new App().getContent("https://tiktok.com/@fall.for.yo");
    }

    private void parseData(String content) throws JsonMappingException, JsonProcessingException{
        Document document = Jsoup.parse(content);
        JsonNode jsonNode = this.objectMapper.readTree(
            document.select("#__UNIVERSAL_DATA_FOR_REHYDRATION__").html()
        );
        System.out.println(jsonNode.get("__DEFAULT_SCOPE__").get("webapp.user-detail").toString());
    }

    public String getContent(String url){
        Request request = new Request.Builder()
                .url(url)
                .build();
    
        try (Response response = client.newCall(request).execute()) {
        if (response.isSuccessful()) {    
                this.parseData(response.body().string());
            } else {
                System.out.println("Request failed: " + response);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }
}
