package com.accountmanager.project.service;


import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class FacebookService {
    /**
     *
     * @return
     *Get the page ID of the user's page.
     */
    public String getFacebookPageId(String accessToken) {
        String url = "https://graph.facebook.com/v20.0/me/accounts?access_token=" + accessToken;

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        JSONObject jsonResponse = new JSONObject(response.getBody());
        JSONArray data = jsonResponse.getJSONArray("data");

        if (!data.isEmpty()) {
            JSONObject page = data.getJSONObject(0);
            return page.getString("id");
        } else {
            throw new RuntimeException("No Facebook Pages found for the user.");
        }
    }

    /**
     *
     * @param pageId
     * @param userAccessToken
     * @return
     * To post on a page, get the page access token.
     */
    public String getPageAccessToken(String pageId, String userAccessToken) {
        try {
            //Your Facebook App details
            String appId = System.getenv("FB_APP_ID");
            String appSecret = System.getenv("FB_APP_SECRET");
            String url = "https://graph.facebook.com/v20.0/" + pageId + "?fields=access_token&access_token=" + userAccessToken;

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            JSONObject jsonResponse = new JSONObject(response.getBody());
            String shortLivedToken = jsonResponse.getString("access_token");

            //Will create a long expiry token
            HttpClient client = HttpClient.newHttpClient();
            String uri = String.format(
                    "https://graph.facebook.com/v20.0/oauth/access_token?" +
                            "grant_type=fb_exchange_token&" +
                            "client_id=%s&" +
                            "client_secret=%s&" +
                            "fb_exchange_token=%s",
                    appId, appSecret, shortLivedToken
            );

            // Create HttpRequest
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(uri))
                    .GET()
                    .build();

            // Send request and get response
            HttpResponse<String> mainResponse = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Parse response to get long-lived token
            JSONObject jsonMainResponse = new JSONObject(mainResponse.body());

            return jsonMainResponse.getString("access_token");
        } catch (InterruptedException | URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     *
     * @param pageId
     * @param imageUrl
     * @param caption
     * @param pageAccessToken
     * @return
     * This method builds and completes the request for posting on a page with an 'Image.'
     */
    public String postOnFacebook(String pageId, String imageUrl, String caption, String pageAccessToken) {
        String url = "https://graph.facebook.com/v20.0/" + pageId + "/photos";

        MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
        params.add("message", caption);
        params.add("link", new FileSystemResource(imageUrl));
        params.add("access_token", pageAccessToken);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(params, headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
        JSONObject jsonResponse = new JSONObject(response.getBody());
        String postId = jsonResponse.getString("id");

        //Returns the url of the post.
        return "https://www.facebook.com/" + pageId + "/posts/" + postId;
    }

    /**
     * This function changes the code to an access token
     * @param code
     * @return
     */
    public String exchangeAccessTokenFromCode(String code){
        String appClientId =  System.getenv("FB_APP_ID") ;
        String clientSecret = System.getenv("FB_APP_SECRET") ;
        String redirectUri = System.getenv("FB_REDIRECT_URI") ;
        String tokenUrl =  "https://graph.facebook.com/v20.0/oauth/access_token";


        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", appClientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(params, headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(tokenUrl, HttpMethod.POST, requestEntity, String.class);

        JSONObject jsonResponse = new JSONObject(response.getBody());
        return jsonResponse.getString("access_token");
    }

}

