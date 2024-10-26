package com.accountmanager.project.controller;

import com.accountmanager.project.service.FacebookService;
import com.accountmanager.project.service.InstagramService;
import com.accountmanager.project.service.XService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/")
@Tag(name = "Token Generator APIs", description = "Login, Generate and Get Tokens")
public class TokenController {

    @Autowired
    private FacebookService facebookService;

    @Autowired
    private InstagramService instagramService;

    @Autowired
    private XService xService;

    private boolean condition = false;
    private boolean xConditionMet = false;
    private String fbPage;
    private String instagramAccount;
    private Map<String, String> xTokens = new HashMap<>();
    private String pageAccessToken;

    /**
     * This function is the entry point, when user tries to log in
     * @return
     */
    @GetMapping("/login")
    @Operation(summary = "Login with Facebook")
    public RedirectView loginToFbAndInst(){
        String appClientId = System.getenv("FB_APP_ID");
        String redirectUri = System.getenv("FB_REDIRECT_URI");
        String oauthUrl = "https://www.facebook.com/v20.0/dialog/oauth?client_id=" + appClientId + "&redirect_uri=" + redirectUri + "&scope=instagram_basic,instagram_content_publish,pages_show_list,pages_read_engagement,pages_manage_posts&response_type=code";

        return new RedirectView(oauthUrl);
    }

    /**
     * After user authenticate your app, they will be redirected to this function with a 'code'
     * @param code
     *
     */
    @GetMapping("/authenticate-user-fb")
    @Operation(summary = "Will authenticate User FB account")
    public RedirectView handleFbAuthentication(@RequestParam("code") String code){
        if (code != null){
            try {
                String fbAccessToken = facebookService.exchangeAccessTokenFromCode(code);
                fbPage = facebookService.getFacebookPageId(fbAccessToken);
                pageAccessToken = facebookService.getPageAccessToken(fbPage, fbAccessToken);
                instagramAccount = instagramService.getInstagramBusinessAccountId(fbPage, pageAccessToken);
                condition = true;
                return new RedirectView("/close");
            }catch (RuntimeException e){
                log.error("Error in FB page: {}", e.getMessage());
                return new RedirectView("/fbError");
            }
        }
        return new RedirectView("/fbError");
    }

    @GetMapping("/check-condition")
    @Operation(summary = "To retrieve tokens ")
    public Map<String, Boolean> isCondition() {
        Map<String, Boolean> response = new HashMap<>();
        response.put("conditionMet", condition);
        response.put("xConditionMet",xConditionMet);
        return response;
    }

    @GetMapping("/get-access")
    @Operation(summary = "Will send FB tokens to user")
    public ResponseEntity<Map<String,String>> getAccess(){
        HashMap<String, String> accessToken = new HashMap<>();
        accessToken.put("fbToken", pageAccessToken);
        accessToken.put("fbPage", fbPage);
      accessToken.put("instagramAccount", instagramAccount);
       condition = false;
        return ResponseEntity.ok(accessToken);
    }

    @GetMapping("/twitter")
    @Operation(summary = "For X sign in V1.1")
    public RedirectView XBothTokens(){
        String link = xService.authenticate();
        return  new RedirectView(link);
    }


    @GetMapping("/authenticate")
    @Operation(summary = "For X sign in V2.0")
    public RedirectView handleX(){
        String link = xService.getAuthorizationUrl();
         return new RedirectView(link);
    }

    /**
     * This function will authenticate one with v1.1 and other one with v2.0
     * @param code
     * @param oauthToken
     * @param oauthVerifier
     */
    @GetMapping("/authenticate-user-x")
    @Operation(summary = "authenticate X user")
    public RedirectView handleXAuthentication(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "oauth_token", required = false) String oauthToken,
            @RequestParam(value = "oauth_verifier", required = false) String oauthVerifier
    ){
        try {
            if (code != null) {
                String codeVerifier = "challenge";
                Map<String, String> token = xService.getAccessToken(code, codeVerifier);
                xTokens.put("bearerToken", token.get("bearerAccess"));
                xTokens.put("refreshToken", token.get("refreshToken"));
                xConditionMet = true;
                return new RedirectView("/close");
            } else {
                Map<String, String> xAllTokens = xService.authenticatedFinish(oauthToken, oauthVerifier);
                xTokens.put("accessToken", xAllTokens.get("accessToken"));
                xTokens.put("accessTokenSecret", xAllTokens.get("accessTokenSecret"));
                return new RedirectView(System.getenv("BACKEND_URL")+"/authenticate");
            }
        } catch (RuntimeException e){
            log.error("Error in X: {}", e.getMessage());
            return new RedirectView("/xError");
        }
    }


    @GetMapping("/newBearer")
    @Operation(summary = "Refreshing token for X")
    public ResponseEntity<Map<String,String>> newBearerAccess(String refreshToken){
        return ResponseEntity.ok(xService.getNewAccessToken(refreshToken));
    }

    @GetMapping("/getXTokens")
    @Operation(summary = "send X tokens")
    public ResponseEntity<Map<String,String>> getXToken(){
        xConditionMet = false;
        return ResponseEntity.ok(xTokens);
    }


}
