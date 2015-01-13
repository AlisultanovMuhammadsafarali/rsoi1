
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;

import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;

import org.json.*;

/*
* Servlet parameters
* */

 @WebServlet(name = "oauth", urlPatterns = {"/oauth/*", "/oauth"},
initParams = {
    // clientId is 'Consumer Key' in the Remote Access UI
    @WebInitParam(name = "clientId", value = "0000000048132572"),
    // clientSercret is 'Consumer Secret' int rhe Remote Accress UI
    @WebInitParam(name = "clientSecret", value = "e47gBbkXskk9ho5dfunmfQWgoH6KhCgf"),
    // This must be identical to 'Callback URL' in the Remote Access UI
    @WebInitParam(name = "redirectUri", value = "http://ideakeep1.ru:8080/oauth_servlet/oauth"),
    @WebInitParam(name = "environment", value = "https://login.live.com"), })

//@WebServlet("/")
public class OAuthServlet extends HttpServlet {

    private String clientId = null;
    private String clientSecret = null;
    private String redirectUri = null;
    private String environment = null;
    private String authUrl = null;
    private String tokenUrl = null;

    String page = "mypage.jsp";

    public void init() throws ServletException {
        clientId = this.getInitParameter("clientId");
        clientSecret = this.getInitParameter("clientSecret");
        redirectUri = this.getInitParameter("redirectUri");
        environment = this.getInitParameter("environment");

        authUrl = environment + "/oauth20_authorize.srf?response_type=code&client_id="
                              + clientId + "&scope=wl.signin%20wl.basic%20wl.skydrive%20wl.photos"
                              + "&redirect_uri="+ redirectUri;

       tokenUrl = environment + "/oauth20_token.srf";
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String code = request.getParameter("code");
        String instanceUrl = null;

        if (code == null) {
            response.sendRedirect(authUrl);
        }
            else {

            System.out.println("Auth successful - got callback");

            HttpClient client = HttpClientBuilder.create().build();
            HttpPost post = new HttpPost(tokenUrl);

            post.setHeader("Content-Type","application/x-www-form-urlencoded");

            List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
            urlParameters.add(new BasicNameValuePair("client_id", clientId));
            urlParameters.add(new BasicNameValuePair("client_secret", clientSecret));
            urlParameters.add(new BasicNameValuePair("redirect_uri", redirectUri));
            urlParameters.add(new BasicNameValuePair("grant_type", "authorization_code"));
            urlParameters.add(new BasicNameValuePair("code", code));

            post.setEntity(new UrlEncodedFormEntity(urlParameters));

            try {
                HttpResponse resp = client.execute(post);

                BufferedReader rd = new BufferedReader(new InputStreamReader(resp.getEntity().getContent()));

                StringBuffer result = new StringBuffer();
                String line = "";
                while ((line = rd.readLine()) != null) {
                    result.append(line);
                }

                try {

                    JSONObject authResponse = new JSONObject(result.toString());

                    String token_type = authResponse.getString("token_type");
                    String expires_in = authResponse.getString("expires_in");
                    String scope = authResponse.getString("scope");
                    String access_token = authResponse.getString("access_token");
                    String authentication_token = authResponse.getString("authentication_token");
                    String user_id = authResponse.getString("user_id");

                    System.out.println("Auth response: " + authResponse.toString(2));

                    String getUrl = "https://apis.live.net/v5.0/me/skydrive?access_token="+access_token;

                    HttpGet getMethod = new HttpGet(getUrl);
                    resp = client.execute(getMethod);

                    rd = new BufferedReader(new InputStreamReader(resp.getEntity().getContent()));

                    result = new StringBuffer();
                    line = "";
                    while ((line = rd.readLine()) != null) {
                        result.append(line);
                    }

                    JSONObject jsonResp = new JSONObject(result.toString());
                    request.setAttribute("name", jsonResp.toString());

                    //System.out.println("Got access token: " + accessToken);
                } catch (JSONException e) {
                    e.printStackTrace();
                    throw new ServletException(e);
                }
            } catch (HttpException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                post.releaseConnection();
            }

            RequestDispatcher dispatcher = request.getRequestDispatcher(page);
            if (dispatcher != null) {
                dispatcher.forward(request, response);
            }
        }
    }


}
