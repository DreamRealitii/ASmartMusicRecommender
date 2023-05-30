package Backend.Spotify;

import Backend.Helper.HttpRequest;
import Backend.Helper.ParseJson;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * @author Ethan Carnahan
 * Bootleg Java access to Spotify API.
 * See tutorials: <a href="https://developer.spotify.com/documentation/web-api/tutorials/code-flow">General Flow</a>,
 * <a href="https://developer.spotify.com/documentation/web-api/tutorials/code-pkce-flow">PKCE Flow</a>
 */
public class SpotifyAuth {

  //region Fields and AccessCode
  private static final String CLIENT_ID = "7449838d0e614661b59e4778c51e31ae";
  private static final String REDIRECT_URI = "http://localhost:1234/auth";
  private static final String SCOPES = "playlist-modify-private";
  private static final int REDIRECT_PORT = 1234;
  private AccessCode accessCode;

  // Spotify access code with expiration data and refresh token.
  // Rest of program only needs "code".
  private static class AccessCode {

    private String code, refresh;
    private final String type;
    private LocalDateTime expiration;

    // Used for ReadAccessCode().
    private AccessCode(String code, String type, String refresh, LocalDateTime expiration) {
      this.code = code;
      this.type = type;
      this.refresh = refresh;
      this.expiration = expiration;
    }

    // Creates AccessCode object from the json data sent by Spotify.
    private AccessCode(String jsonLine) {
      // Parse JSON for value locations.
      try {
        code = ParseJson.getString(jsonLine, "access_token");
        System.out.println("SpotifyAuth: Access Token = " + code);
        type = ParseJson.getString(jsonLine, "token_type");
        System.out.println("SpotifyAuth: Token Type = " + type);
        refresh = ParseJson.getString(jsonLine, "refresh_token");
        System.out.println("SpotifyAuth: Refresh Token = " + refresh);
        // Calculate expiration time.
        int expiresIn = ParseJson.getInt(jsonLine, "expires_in");
        expiration = LocalDateTime.now().plusSeconds(expiresIn);
        System.out.println("SpotifyAuth: Expiration Time = " + expiration);
      } catch (RuntimeException e) {
        throw new RuntimeException("SpotifyAuth: Failed to parse access token - " + e.getMessage());
      }
    }

    // Writes this access code to the directory.
    private void WriteAccessCode(String directory) throws IOException {
      System.out.println("SpotifyAuth: Saving access code to " + directory + "\\SpotifyAccessCode");
      FileWriter writer = new FileWriter(directory + "\\SpotifyAccessCode", false);
      writer.write(code + "\n" + type + "\n" + refresh + "\n" + expiration + "\n");
      writer.flush();
      writer.close();
    }

    // Reads the access code from this directory.
    private static AccessCode ReadAccessCode(String directory) throws IOException {
      System.out.println(
          "SpotifyAuth: Loading access code from " + directory + "\\SpotifyAccessCode");
      BufferedReader reader = new BufferedReader(new FileReader(directory + "\\SpotifyAccessCode"));
      String code = reader.readLine();
      String type = reader.readLine();
      String refresh = reader.readLine();
      LocalDateTime expiration = LocalDateTime.parse(reader.readLine());
      return new AccessCode(code, type, refresh, expiration);
    }
  }
  //endregion

  //region Public methods
  public SpotifyAuth() {
    accessCode = null;
  }

  /**
   * @return A valid access code to put in the "Authorization" header of Spotify API requests.
   */
  public String getAccessCode() {
    // Load AccessCode from local storage.
    if (accessCode == null) {
      try {
        accessCode = AccessCode.ReadAccessCode(System.getProperty("user.dir"));
      } catch (IOException e) {
        // Request and save new AccessCode if there isn't one.
        System.out.println("SpotifyAuth: Failed to load access code - " + e.getMessage());
        System.out.println("SpotifyAuth: Getting new access code");
        accessCode = getNewAccessCode();
        try {
          accessCode.WriteAccessCode(System.getProperty("user.dir"));
        } catch (IOException e2) {
          System.out.println("SpotifyAuth: Failed to save new access code - " + e2.getMessage());
        }
      }
    }

    // Refresh access code if it's expired.
    if (LocalDateTime.now().isAfter(accessCode.expiration)) {
      System.out.println("SpotifyAuth: Saved access code is expired");
      System.out.println("SpotifyAuth: Getting new access code");
      refreshAccessCode(accessCode);
      try {
        accessCode.WriteAccessCode(System.getProperty("user.dir"));
      } catch (IOException e2) {
        System.out.println("SpotifyAuth: Failed to save new access code - " + e2.getMessage());
      }
    }

    return accessCode.type + " " + accessCode.code;
  }
  //endregion

  //region Get first access token.
  private static AccessCode getNewAccessCode() {
    String codeVerifier = generateCodeVerifier(); // Also used as "state".
    System.out.println("SpotifyAuth: codeVerifier = " + codeVerifier);
    String codeChallenge = generateCodeChallenge(codeVerifier);
    System.out.println("SpotifyAuth: codeChallenge = " + codeChallenge);
    new Thread(() -> openAuthorizationTab(codeVerifier, codeChallenge)).start();
    String authorizationCode = getAuthorizationCode(codeVerifier);
    System.out.println("SpotifyAuth: authorizationCode = " + authorizationCode);
    return getAccessCode(codeVerifier, authorizationCode);
  }

  private static String generateCodeVerifier() {
    // Randomly generate string.
    StringBuilder result = new StringBuilder();
    String possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
      for (int i = 0; i < 64; i++) {
          result.append(possible.charAt((int) Math.floor(Math.random() * possible.length())));
      }

    // Convert to Base64-URL.
    byte[] bytes = result.toString().getBytes(StandardCharsets.UTF_8);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private static String generateCodeChallenge(String code) {
    // SHA-256 hash. See https://www.baeldung.com/sha-256-hashing-java
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalArgumentException(e.getMessage());
    }
    byte[] hash = digest.digest(code.getBytes(StandardCharsets.UTF_8));

    // Convert to Base64-URL.
    return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
  }

  // Opens the Spotify webpage asking users to authorize ASMR to access Spotify.
  private static void openAuthorizationTab(String codeVerifier, String codeChallenge) {
    // Build URI.
    StringBuilder url = new StringBuilder("https://accounts.spotify.com/authorize?");
    url.append("client_id=");
    url.append(CLIENT_ID);
    url.append("&");
    url.append("response_type=code&");
    url.append("redirect_uri=");
    url.append(REDIRECT_URI);
    url.append("&");
    url.append("state=");
    url.append(codeVerifier);
    url.append("&");
    url.append("scope=");
    url.append(SCOPES);
    url.append("&");
    url.append("code_challenge_method=S256&");
    url.append("code_challenge=");
    url.append(codeChallenge);
    System.out.println("SpotifyAuth: Opening browser to authorize");
    URI uri;
    try {
      uri = new URL(url.toString()).toURI();
    } catch (MalformedURLException | URISyntaxException e) {
      throw new IllegalStateException("SpotifyAuth: Bad URL - " + e.getMessage());
    }

    // Open link in default browser. See https://stackoverflow.com/questions/10967451/open-a-link-in-browser-with-java-button
    try {
      Desktop.getDesktop().browse(uri);
    } catch (IOException e) {
      throw new IllegalStateException("SpotifyAuth: Unable to open browser - " + e.getMessage());
    }
  }

  // Blocks program and waits for user to grant us access to Spotify.
  // See https://javarevisited.blogspot.com/2015/06/how-to-create-http-server-in-java-serversocket-example.html#axzz7yL5vt0B9
  private static String getAuthorizationCode(String codeVerifier) {
    String result;
    try {
      // Start server.
      ServerSocket server = new ServerSocket(REDIRECT_PORT);
      Socket client = server.accept();

      // Read request.
      InputStreamReader isr = new InputStreamReader(client.getInputStream());
      BufferedReader reader = new BufferedReader(isr);
      String line = reader.readLine();
      System.out.println("SpotifyAuth: Got authorization code");

      // Parse code, state, and error (if user rejected)
      int codeStart = line.indexOf("?code=");
      int stateStart = line.indexOf("&state=");
      int errorStart = line.indexOf("?error=");
        if (errorStart != -1) {
            throw new IllegalStateException("SpotifyAuth: Authentication request failed.");
        }
        if (codeStart == -1 || stateStart == -1) {
            throw new IllegalStateException(
                "SpotifyAuth: HTTP request did not contain code or state.");
        }
      int stateEnd = line.indexOf(" ", stateStart);
      String code = line.substring(codeStart + 6, stateStart);
      String state = line.substring(stateStart + 7, stateEnd);

      // Something bad happened if state doesn't match codeVerifier.
        if (state.equals(codeVerifier)) {
            result = code;
        } else {
            throw new IllegalStateException(
                "SpotifyAuth: States do not match - " + state + " != " + codeVerifier);
        }

      // Send response and close.
      String response = "HTTP/1.1 200 OK\r\n\r\n";
      client.getOutputStream().write(response.getBytes(StandardCharsets.UTF_8));
      client.close();
      server.close();

    } catch (IOException e) {
      throw new IllegalStateException("SpotifyAuth: Failed to start server - " + e.getMessage());
    } catch (IllegalStateException e) {
      throw new IllegalStateException(e.getMessage());
    }
    return result;
  }

  // See https://docs.oracle.com/javase/tutorial/networking/urls/readingWriting.html
  private static AccessCode getAccessCode(String codeVerifier, String authorizationCode) {
    // Build URL and request body.
    String url = "https://accounts.spotify.com/api/token";
    StringBuilder body = new StringBuilder();
    body.append("grant_type=authorization_code&");
    body.append("code=");
    body.append(authorizationCode);
    body.append("&");
    body.append("redirect_uri=");
    body.append(REDIRECT_URI);
    body.append("&");
    body.append("client_id=");
    body.append(CLIENT_ID);
    body.append("&");
    body.append("code_verifier=");
    body.append(codeVerifier);

    System.out.println("SpotifyAuth: Sending request for access token.");
    try {
      // Parse access token from response.
      AccessCode result = new AccessCode(HttpRequest.postAndGetJsonFromUrlBody(url, body.toString(),
          "application/x-www-form-urlencoded", null));
      System.out.println("SpotifyAuth: Got access token.");
      return result;
    } catch (RuntimeException e) {
      throw new RuntimeException("SpotifyAuth: Failed to get access token - " + e.getMessage());
    }
  }
  //endregion

  //region Refresh access token.
  private static void refreshAccessCode(AccessCode accessCode) {
    String url = "https://accounts.spotify.com/api/token";
    StringBuilder body = new StringBuilder();
    body.append("grant_type=refresh_token&");
    body.append("refresh_token=");
    body.append(accessCode.refresh);
    body.append("&");
    body.append("client_id=");
    body.append(CLIENT_ID);

    System.out.println("SpotifyAuth: Sending request for refresh token.");
    try {
      // Parse refreshed token from response.
      String result = HttpRequest.postAndGetJsonFromUrlBody(url, body.toString(),
          "application/x-www-form-urlencoded", null);
      System.out.println("SpotifyAuth: Got refreshed token.");
      accessCode.code = ParseJson.getString(result, "access_token");
      accessCode.refresh = ParseJson.getString(result, "refresh_token");
      int expiresIn = ParseJson.getInt(result, "expires_in");
      accessCode.expiration = LocalDateTime.now().plusSeconds(expiresIn);
    } catch (RuntimeException e) {
      throw new RuntimeException("SpotifyAuth: Failed to get access token - " + e.getMessage());
    }
  }
  //endregion

  // Test getting access token.
  public static void main(String[] args) {
    SpotifyAuth sa = new SpotifyAuth();
    System.out.println(sa.getAccessCode());
  }
}
