package Backend.Spotify;

import Backend.Analysis.SpotifyAnalysis;
import Backend.Helper.HttpRequest;
import Backend.Helper.ParseJson;

import java.util.Arrays;
import java.util.HashMap;

/**
 * @author Ethan Carnahan, Eric Kumar
 * Used to interact with Spotify.
 */
public class SpotifyAPI {

  private static final String FEATURES_URL = "https://api.spotify.com/v1/audio-features/";
  private static final String USERS_URL = "https://api.spotify.com/v1/users/";
  private static final String CREATE_PLAYLIST_URL = "https://api.spotify.com/v1/users/";
  private static final String VIEW_PLAYLIST_URL = "https://api.spotify.com/v1/playlists/";
  private static final String SEARCH_SONG_URL = "https://api.spotify.com/v1/search?q=";
  private static final String TRACK_URL = "https://api.spotify.com/v1/tracks/";
  private static final String SONG_RECOMMENDATION_URL = "https://api.spotify.com/v1/recommendations?";
  private static final String JSON_TYPE = "application/json";
  private static final SpotifyAuth auth = new SpotifyAuth();
  private static String USER_ID = "";


  //region Public methods

  /**
   * Sets the username to use for future SpotifyAPI calls.
   *
   * @param userId The user's Spotify username.
   * @return The display name of the Spotify user, or username if there isn't one.
   * @throws RuntimeException if username does not exist or something else goes wrong.
   */
  public static String setUserId(String userId) {
    String accessToken = auth.getAccessCode();
    String url = USERS_URL + userId;
    String jsonString;
    try {
      jsonString = HttpRequest.getJsonFromUrl(url, accessToken);
    } catch (RuntimeException e) {
      throw new RuntimeException("Spotify API: Invalid username - " + e.getMessage());
    }

    USER_ID = userId;
    System.out.println("SpotifyAPI: Set user ID to " + USER_ID);

    try {
      return ParseJson.getString(jsonString, "display_name");
    } catch (RuntimeException e) { // Happens if display_name is null
      return userId;
    }
  }

  /**
   * Gets the Spotify URL of a song.
   *
   * @param trackId The random string after "track/" in the url of a song.
   * @return Spotify URL of the specified song.
   * @throws RuntimeException if something goes wrong. It could be so many things.
   */
  public static String getTrackURL(String trackId) {
    String accessToken = auth.getAccessCode();
    String url = TRACK_URL + "/" + trackId;
    String jsonString;
    try {
      jsonString = HttpRequest.getJsonFromUrl(url, accessToken);
    } catch (RuntimeException e) {
      throw new RuntimeException("SpotifyAPI: Failed to connect to Spotify - " + e.getMessage());
    }

    return ParseJson.getString(ParseJson.getObject(jsonString, "external_urls"), "spotify");
  }

  /**
   * Gets the Spotify URL of multiple songs.
   *
   * @param trackIds The random strings after "track/" in the url of a song.
   * @return Spotify URL of the specified songs.
   * @throws RuntimeException if something goes wrong. It could be so many things.
   */
  public static String[] getTrackURLs(String[] trackIds) {
    String[] result = new String[trackIds.length];
    String accessToken = auth.getAccessCode();
    StringBuilder url = new StringBuilder(TRACK_URL + "?ids=" + trackIds[0]);
    for (int i = 1; i < trackIds.length; i++) {
      url.append(",");
      url.append(trackIds[i]);
    }
    String jsonString;
    try {
      jsonString = HttpRequest.getJsonFromUrl(url.toString(), accessToken);
    } catch (RuntimeException e) {
      e.printStackTrace();
      throw new RuntimeException("SpotifyAPI: Failed to connect to Spotify - " + e.getMessage());
    }

    String[] tracks = ParseJson.getArray(jsonString, "tracks");
    for (int i = 0; i < tracks.length; i++) {
      result[i] = ParseJson.getString(ParseJson.getObject(tracks[i], "external_urls"), "spotify");
    }

    return result;
  }

  /**
   * Gets Spotify's track analysis of a song.
   *
   * @param trackId The random string after "track/" in the url of a song.
   * @return Spotify's basic track analysis.
   * @throws RuntimeException if something goes wrong. It could be so many things.
   */
  public static SpotifyAnalysis getTrackFeatures(String trackId) {
    // Request track features.
    String accessToken = auth.getAccessCode();
    String url = FEATURES_URL + trackId;
    String jsonString;
    try {
      jsonString = HttpRequest.getJsonFromUrl(url, accessToken);
    } catch (RuntimeException e) {
      throw new RuntimeException("SpotifyAPI: Failed to connect to Spotify - " + e.getMessage());
    }

    // Parse response into a SpotifyAnalysis object.
    HashMap<String, String[]> artistsAndGenres = getArtistAndGenre(trackId);
    return new SpotifyAnalysis(jsonString, trackId, artistsAndGenres.get("artists"), artistsAndGenres.get("genres"));
  }

  /**
   * Gets the artistID and genre of a track
   *
   * @param trackId TrackId for song to get the artist and genre
   * @throws RuntimeException if something goes wrong. It could be so many things.
   */
  private static HashMap<String, String[]> getArtistAndGenre(String trackId) {
    HashMap<String, String[]> result = new HashMap<>();
    String accessToken = auth.getAccessCode();
    String url = TRACK_URL + trackId;
    String responseString;
    try{
      responseString = HttpRequest.getJsonFromUrl(url, accessToken);
      String[] artistsArray = ParseJson.getArray(responseString, "artists");
      if (artistsArray.length > 1) {
        String[] artists = new String[artistsArray.length];
        int i = 0;
        for (String s : artistsArray) {
          artists[i] = ParseJson.getString(s, "id");
          i++;
          System.out.println("Artists: " + Arrays.toString(artists));
          result.put("artists", artists);
        }
      }else {
        String[] id = new String[] {ParseJson.getString(artistsArray[0], "id")};
        result.put("artists", id);
      }
      String album = ParseJson.getObject(responseString, "album");
      if (album.contains("genres")) {
        String[] genres = ParseJson.getArray(album, "genres");
        System.out.println("Genres");
        result.put("genres", genres);
      }else {
        String secondResponseString = HttpRequest.getJsonFromUrl("https://api.spotify.com/v1/recommendations/available-genre-seeds", accessToken);
        String[] recommendedGenres = ParseJson.getArray(secondResponseString, "genres");
        int seedGenre =  (int)(Math.random() * recommendedGenres.length);
        String[] genre = new String[] {recommendedGenres[seedGenre]};
        result.put("genres", genre);
      }



      //System.out.println("Results:" + result);

    }catch(RuntimeException e) {
      e.printStackTrace();
      throw new RuntimeException("SpotifyAPI: Failed to connect to Spotify - " + e.getMessage());
    }
    return result;
  }

  /**
   * Creates a playlist from a list of songs
   *
   * @param trackIds List of the random strings after "track/" in the url of a song.
   * @throws RuntimeException if something goes wrong. It could be so many things.
   */
  public static void createPlaylist(String[] trackIds) {
    String accessToken = auth.getAccessCode();
    String playlistCreationUrl = CREATE_PLAYLIST_URL + USER_ID + "/playlists";
    String uris = String.join(",", trackIds);
    StringBuilder body = new StringBuilder();
    body.append("{\"name\": \"ASMR playlist\",");
    body.append("\"description\": \"Playlist created by ASMR\",");
    body.append("\"public\": false}");
    String responseString;
    try {
      responseString = HttpRequest.postAndGetJsonFromUrlBody(playlistCreationUrl, body.toString(), JSON_TYPE,
              accessToken);
      String id = ParseJson.getString(responseString, "id");
      String playlistAdditionUrl = VIEW_PLAYLIST_URL + id + "/tracks?uris=" + uris;
      HttpRequest.postAndGetJsonFromUrlBody(playlistAdditionUrl, "", null, accessToken);

    } catch (RuntimeException e) {
      throw new RuntimeException("SpotifyAPI: Failed to connect to Spotify - " + e.getMessage());
    }
  }

  /**
   * Fetches a random song from Spotify
   *
   * @throws RuntimeException if something goes wrong. It could be so many things.
   * @return The Spotify IDs of random songs from Spotify
   */

  public static String[] randomSong(int num) {
    String[] spotifyIds = new String[num - 1];

    String song = "";
    String accessToken = auth.getAccessCode();
    // A list of all characters that can be chosen.
    String characters = "abcdefghijklmnopqrstuvwxyz";

    // Gets a random character from the characters string.
    String randomCharacter = String.valueOf(
        characters.charAt((int) Math.floor(Math.random() * characters.length())));

    // Places the wildcard character at the beginning, or both beginning and end, randomly.
    switch ((int) Math.round(Math.random())) {
      case 0 -> song = randomCharacter + "$";
      case 1 -> song = "$" + randomCharacter + "$";
    }
    System.out.println("SpotifyAPI: Getting " + num + " random songs using query " + song);

    String responseString;
    try {
      String url = SEARCH_SONG_URL + song
          + "&type=track"
          + "&limit="+num;

      responseString = HttpRequest.getJsonFromUrl(url, accessToken);
      String tracks = ParseJson.getObject(responseString, "tracks");
      String[] items = ParseJson.getArray(tracks, "items");
      for (int i = 0; i < num - 1; i++) {

        //System.out.println("Track: " + track);
        //System.out.println("Items:" + Arrays.toString(items));

        String id = ParseJson.getString(items[i],"id");
        spotifyIds[i] = id;
//      responseString = HttpRequest.getJsonFromUrl(SEARCH_TRACK_URL + id, accessToken);
//      spotifyLink = ParseJson.getString(ParseJson.getObject(responseString, "external_urls"), "spotify");
        //System.out.println("Random song Link: " + spotifyLink);
      }

    } catch (RuntimeException e) {
      e.printStackTrace();
      throw new RuntimeException("SpotifyAPI: Failed to connect to Spotify - " + e.getMessage());
    }
    return spotifyIds;
  }

  /**
   * Gets track recommendations from Spotify that we will use in analysis to give
   * better recommendations
   *
   * @throws RuntimeException if something goes wrong. It could be so many things.
   * @return The Spotify IDs of songs recommended by Spotify
   */
  public static String[] getRecommendations(SpotifyAnalysis track) {
    String accessToken = auth.getAccessCode();

    String genres = Arrays.toString(track.getGenres());
    genres = genres.replaceAll("\"", "");
    genres = genres.substring(1, genres.length()-1);
    System.out.println("genres: " + genres);
    String artists = Arrays.toString(track.getArtistsID());
    artists = artists.substring(1, artists.length()-1);
    System.out.println("artists: " + artists);
    String requestUrl = SONG_RECOMMENDATION_URL + "seed_artists=" + artists.replaceAll("\\s", "") + "&seed_genres=" + genres.replaceAll("\\s", "") + "&seed_tracks=" + track.getTrackId();
    System.out.println("Request URL: " + requestUrl);
    String responseString;
    try{
      responseString = HttpRequest.getJsonFromUrl(requestUrl, accessToken);
      String[] tracks = ParseJson.getArray(responseString, "tracks");
      String[] recommendations;
      if (tracks.length > 1) {
        recommendations = new String[tracks.length];
        for(int i = 0; i < tracks.length; i++) {
          recommendations[i] = ParseJson.getString(tracks[i], "id");
        }
      }else {
        recommendations = new String[1];
        recommendations[0] = ParseJson.getString(tracks[0], "id");
      }
      return recommendations;
    }catch(RuntimeException e) {
      e.printStackTrace();
      throw new RuntimeException("SpotifyAPI: Failed to connect to Spotify - " + e.getMessage());
    }
  }
  //endregion

}