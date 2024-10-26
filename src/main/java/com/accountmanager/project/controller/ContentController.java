package com.accountmanager.project.controller;

import com.accountmanager.project.service.FacebookService;
import com.accountmanager.project.service.InstagramService;
import com.accountmanager.project.service.S3Service;
import com.accountmanager.project.service.XService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


@Slf4j
@RestController
@Tag(name = "Post Content API")
public class ContentController {

    @Autowired
    private FacebookService facebookService;

    @Autowired
    private InstagramService instagramService;

    @Autowired
    private XService xService;

    @Autowired
    private S3Service s3Service;


    @PostMapping("/submit")
    @Operation(summary = "Will upload the content")
    public Map<String, String> submittedData(@RequestParam("fbToken") String pageAccessToken, @RequestParam("fbPage") String facebookPageId,
                                             @RequestParam("bearerToken") String bearerToken,
                                             @RequestParam("accessToken") String accessToken,
                                             @RequestParam("accessTokenSecret") String accessTokenSecret,
                                             @RequestParam("instagramAccount") String instagramAccount,
                                             @RequestParam("file") String file, @RequestParam("caption") String caption) {
        try {
            Map<String, String> imageInfo = saveImage(file);
            String path = imageInfo.get("path");
            String fileName = imageInfo.get("fileName");

            try (InputStream imageStream = new FileInputStream(path)) {
                String fbLink = facebookService.postOnFacebook(facebookPageId, path, caption, pageAccessToken);
                String xLink =  xService.postTweet(caption, bearerToken, accessToken, accessTokenSecret, fileName, imageStream);
                String key =  s3Service.uploadImage(path);
                String onlineImage = s3Service.getImageLink(key);
                String instPostUrl = instagramService.postOnInstagram(instagramAccount, onlineImage, caption, pageAccessToken);

                s3Service.deleteImage(key);
                Map<String, String> links = new HashMap<>();
                links.put("FB" , fbLink);
                links.put("X", xLink);
                links.put("Instagram", instPostUrl);
                return links;
            } catch (Exception e) {
               log.error("An Error: {}", e.getMessage());
            }finally {
                try {
                    Files.delete(Paths.get(path));
                } catch (IOException e) {
                    log.error("Error deleting file: {}", e.getMessage());
                }
            }

        } catch (Exception e) {
           log.error("Error in file creation: {}", e.getMessage());
        }
        return Map.of();
    }

    private  Map<String, String> saveImage(String file) throws IOException{
        String base64Image = file.split(",")[1];
        byte[] imageBytes = Base64.getDecoder().decode(base64Image);

        String outputPath = "src/main/resources/static/uploads/";
        String uniqueID = UUID.randomUUID().toString();
        String fileName =  uniqueID  + "uploaded_image_.jpg" ;
        Path uploadPath = Paths.get(outputPath);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        String path = outputPath + fileName;

        try (OutputStream outputStream = new FileOutputStream(path)) {
            outputStream.write(imageBytes);
        }
        Map<String, String> result = new HashMap<>();
        result.put("path", path);
        result.put("fileName", fileName);
        return result;
    }
}
