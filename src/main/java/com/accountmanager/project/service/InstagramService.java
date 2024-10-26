package com.accountmanager.project.service;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class InstagramService {

    /**
     *
     * @param pageId
     * @param accessToken
     * @return
     * This will return the business Instagram account associated with the page.
     */
    public String getInstagramBusinessAccountId(String pageId, String accessToken) {
        String url = "https://graph.facebook.com/v20.0/" + pageId + "?fields=instagram_business_account&access_token=" + accessToken;

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        JSONObject jsonResponse = new JSONObject(response.getBody());
        JSONObject instagramBusinessAccount = jsonResponse.getJSONObject("instagram_business_account");

        return instagramBusinessAccount.getString("id");
    }

    /**
     *
     * @param instagramBusinessAccountId
     * @param imageUrl
     * @param caption
     * @param accessToken
     * @return
     */
    public String postOnInstagram(String instagramBusinessAccountId, String imageUrl, String caption, String accessToken) {
        try {
            String containerId = createMediaContainer(instagramBusinessAccountId, imageUrl, caption, accessToken);
            return publishMedia(instagramBusinessAccountId, containerId, accessToken);
        } catch (Exception e) {
            log.error("Error posting on Instagram: {}",  e.getMessage());
            return "Couldn't post on Instagram";
        }
    }


    /**
     *
     * @param instagramBusinessAccountId
     * @param imageUrl
     * @param caption
     * @param accessToken
     * @return
     * A container is created before posting.
     */
    private String createMediaContainer(String instagramBusinessAccountId, String imageUrl, String caption, String accessToken) {
        String uploadUrl = "https://graph.facebook.com/v20.0/" + instagramBusinessAccountId + "/media";
        MultiValueMap<String, String> uploadParams = new LinkedMultiValueMap<>();
        uploadParams.add("image_url", imageUrl);
        uploadParams.add("caption", caption);
        uploadParams.add("access_token", accessToken);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(uploadParams, headers);
        ResponseEntity<String> response = restTemplate.exchange(uploadUrl, HttpMethod.POST, requestEntity, String.class);

        JSONObject jsonResponse = new JSONObject(response.getBody());
        return jsonResponse.getString("id");
    }

    /**
     *
     * @param instagramBusinessAccountId
     * @param containerId
     * @param accessToken
     * @return
     * @throws InterruptedException
     * The post will be posted using this method.
     */
    private String publishMedia(String instagramBusinessAccountId, String containerId, String accessToken) throws InterruptedException {
        String publishUrl = "https://graph.facebook.com/v20.0/" + instagramBusinessAccountId + "/media_publish";
        MultiValueMap<String, String> publishParams = new LinkedMultiValueMap<>();
        publishParams.add("creation_id", containerId);
        publishParams.add("access_token", accessToken);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        RestTemplate restTemplate = new RestTemplate();

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(publishParams, headers);
        ResponseEntity<String> response = restTemplate.exchange(publishUrl, HttpMethod.POST, requestEntity, String.class);

        JSONObject jsonResponse = new JSONObject(response.getBody());
        String postId = jsonResponse.getString("id");
        return getPostPermalinkWithRetry(postId, accessToken);
    }

    /**
     *
     * @param postId
     * @param accessToken
     * @return
     * @throws InterruptedException
     * This method is used to get the link to the post.
     */
    private String getPostPermalinkWithRetry(String postId, String accessToken) throws InterruptedException {
        String permalink = null;
        int retryCount = 0;
        int maxRetries = 5;
        int delay = 2000;

        while (retryCount < maxRetries) {
            try {
                String url = "https://graph.facebook.com/v20.0/" + postId + "?fields=permalink&access_token=" + accessToken;

                RestTemplate restTemplate = new RestTemplate();
                ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

                JSONObject jsonResponse = new JSONObject(response.getBody());
                permalink = jsonResponse.getString("permalink");

                if (permalink != null) {
                    break;
                }
            } catch (Exception e) {
                Thread.sleep(delay);
                retryCount++;
            }
        }
        if (permalink == null) {
            throw new RuntimeException("Failed to fetch permalink after " + maxRetries + " retries.");
        }
        return permalink;
    }
}
