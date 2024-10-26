package com.accountmanager.project.service;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.UploadedMedia;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class XService {
    private Twitter twitter;
    private RequestToken requestToken;
    private Map<String, String> tokenSecrets = new HashMap<>();

    private String clientId = System.getenv("X_CLIENT_ID");
    private String redirectUri = System.getenv("X_REDIRECT_URI");

    public void init() {
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey(System.getenv("X_CONSUMER_KEY"))
                .setOAuthConsumerSecret(System.getenv("X_CONSUMER_SECRET"));
        TwitterFactory tf = new TwitterFactory(cb.build());
        twitter = tf.getInstance();
    }

    /**
     * This method is used for v1.1.
     * @return
     */
    public String authenticate() {
        try {
            init();
            requestToken = twitter.getOAuthRequestToken(redirectUri);
            tokenSecrets.put(requestToken.getToken(), requestToken.getTokenSecret());
            return requestToken.getAuthorizationURL();
        } catch (TwitterException e) {
           log.error("Authentication of Twitter Failed: {}", e.getMessage());
            return "Error";
        }
    }

    /**
     *
     * @param oauthToken
     * @param oauthVerifier
     * @return
     * By using v1.1 auth the tokens are generated.
     */
    public Map<String, String> authenticatedFinish(String oauthToken, String oauthVerifier) {
        if (oauthToken != null && oauthVerifier != null) {
            try {
                String tokenSecret = tokenSecrets.get(oauthToken);
                RequestToken requestToken = new RequestToken(oauthToken, tokenSecret);
                AccessToken accessToken = twitter.getOAuthAccessToken(requestToken, oauthVerifier);

                Map<String, String> xTokens = new HashMap<>();
                xTokens.put("accessToken", accessToken.getToken());
                xTokens.put("accessTokenSecret", accessToken.getTokenSecret());
                return xTokens;
            } catch (TwitterException e) {
                log.error("Final Authentication Failed: {}", e.getMessage());
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * This method uses v2.0.
     * @return
     */
    public String getAuthorizationUrl() {
        String authorizationUrl = "https://twitter.com/i/oauth2/authorize";
        String responseType = "code";
        String scope = "tweet.read tweet.write users.read offline.access";
        String state = "random_state_string";
        String codeChallenge = "challenge";

        return authorizationUrl + "?response_type=" + responseType + "&client_id=" + clientId + "&redirect_uri=" + redirectUri + "&scope=" + scope + "&state=" + state + "&code_challenge=" + codeChallenge + "&code_challenge_method=plain";
    }

    /**
     * Will provide a bearer token that will be used for posting.
     * @param authorizationCode
     * @param codeVerifier
     * @return
     */
    public  Map<String, String>  getAccessToken(String authorizationCode, String codeVerifier) {
        try {
            String url = "https://api.x.com/2/oauth2/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("code", authorizationCode);
            params.add("grant_type", "authorization_code");
            params.add("client_id", clientId);
            params.add("redirect_uri", redirectUri);
            params.add("code_verifier", codeVerifier);

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(params, headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);

            JSONObject jsonResponse = new JSONObject(response.getBody());
            Map<String, String> newBearer = new HashMap<>();
            newBearer.put("bearerAccess", jsonResponse.getString("access_token"));
            newBearer.put("refreshToken", jsonResponse.getString("refresh_token"));
            return newBearer;

        } catch (HttpClientErrorException e) {
            log.error("HTTP error: {}" , e.getStatusCode() + " - " + e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Error getting access token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * As the bearer token expires, a new token is generated by this method.
     * @param refreshToken
     * @return
     */
    public  Map<String, String> getNewAccessToken(String refreshToken){
        try {
            String url = "https://api.x.com/2/oauth2/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("refresh_token", refreshToken);
            params.add("grant_type", "refresh_token");
            params.add("client_id", clientId);

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(params, headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);

            JSONObject jsonResponse = new JSONObject(response.getBody());
            Map<String, String> newBearer = new HashMap<>();
            newBearer.put("bearerAccess", jsonResponse.getString("access_token"));
            newBearer.put("refreshToken", jsonResponse.getString("refresh_token"));
            return newBearer;
        } catch (HttpClientErrorException e) {
            log.error("HTTP error: {}" , e.getStatusCode() + " - " + e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Error getting access token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Will create a media_id.
     * @param fileName
     * @param image
     * @return
     * @throws TwitterException
     */
    public UploadedMedia uploadMedia(String fileName, InputStream image) throws TwitterException {
        return twitter.uploadMedia(fileName, image);
    }


    public String postTweet(String tweetText, String bearerToken, String accessToken, String accessTokenSecret, String filename, InputStream image) {
        try {
            String url = "https://api.x.com/2/tweets";
            ConfigurationBuilder cb = new ConfigurationBuilder();
            cb.setDebugEnabled(true)
                    .setOAuthConsumerKey(System.getenv("X_CONSUMER_KEY"))
                    .setOAuthConsumerSecret(System.getenv("X_CONSUMER_SECRET"))
                    .setOAuthAccessToken(accessToken)
                    .setOAuthAccessTokenSecret(accessTokenSecret);
            TwitterFactory tf = new TwitterFactory(cb.build());
            twitter = tf.getInstance();

            UploadedMedia uploadedMedia = uploadMedia(filename, image);
            String mediaId = String.valueOf(uploadedMedia.getMediaId());

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(bearerToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Set up the body
            JSONObject body = new JSONObject();
            body.put("text", tweetText);
            JSONObject media = new JSONObject();
            media.put("media_ids", new String[]{mediaId});
            body.put("media", media);

            // Create the HTTP entity
            HttpEntity<String> requestEntity = new HttpEntity<>(body.toString(), headers);

            // Send the request
            RestTemplate restTemplate = new RestTemplate();
            try {
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
                JSONObject jsonResponse = new JSONObject(response.getBody());
                String tweetId = jsonResponse.getJSONObject("data").getString("id");
                return "https://twitter.com/i/web/status/" + tweetId;
            } catch (HttpClientErrorException e) {
                log.error("HTTP error while posting: {}" , e.getStatusCode() + " - " + e.getMessage());
                throw e;
            }
        } catch (TwitterException e) {
            throw new RuntimeException(e);
        }
    }
}