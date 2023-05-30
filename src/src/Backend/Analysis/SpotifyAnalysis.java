package Backend.Analysis;

import Backend.Helper.ParseJson;
import Backend.Spotify.SpotifyAPI;

import java.text.DecimalFormat;
import java.util.Arrays;

/**
 * @author Ethan Carnahan
 * The sound analysis of a song given by Spotify's "Audio Features". See
 * <a href="https://developer.spotify.com/documentation/web-api/reference/get-audio-features">Spotify API</a>
 */
public class SpotifyAnalysis implements SoundAnalysis {

  //region Fields, Constants, Constructor
  // 0.0-1.0 Values
  private final double acousticness, danceability, energy, instrumentalness, liveness, speechiness, valence;
  // > 1.0 Values
  private final double loudness, tempo;
  // Integer Values.
  private final int duration_ms, key, mode, time_signature;

  // Maximum difference = 1.0 * number of analysis values.
  private static final double MAX_DIFFERENCE = 13.0;
  // Exponentially adjusts the value of individual differences less than 1.0.
  private static final double DIFFERENCE_EXPONENT = 2.0;
  // Multiplies arctan bounds from pi/2 to 1.
  private static final double ARCTAN_MULTIPLIER = 2.0 / Math.PI;
  private final String trackId, trackName;
  private final String[] artistsID, genres;

  public SpotifyAnalysis(String jsonString, String trackId) {
    acousticness = ParseJson.getDouble(jsonString, "acousticness");
    danceability = ParseJson.getDouble(jsonString, "danceability");
    energy = ParseJson.getDouble(jsonString, "energy");
    instrumentalness = ParseJson.getDouble(jsonString, "instrumentalness");
    liveness = ParseJson.getDouble(jsonString, "liveness");
    speechiness = ParseJson.getDouble(jsonString, "speechiness");
    valence = ParseJson.getDouble(jsonString, "valence");
    loudness = ParseJson.getDouble(jsonString, "loudness");
    tempo = ParseJson.getDouble(jsonString, "tempo");
    duration_ms = ParseJson.getInt(jsonString, "duration_ms");
    key = ParseJson.getInt(jsonString, "key");
    mode = ParseJson.getInt(jsonString, "mode");
    time_signature = ParseJson.getInt(jsonString, "time_signature");
    this.trackId = trackId;
    this.artistsID = new String[] {""};
    this.genres = new String[]{""};
    this.trackName = "";
  }

  public SpotifyAnalysis(String jsonString, String trackId, String[] artistsId, String[] genres, String trackName) {
    acousticness = ParseJson.getDouble(jsonString, "acousticness");
    danceability = ParseJson.getDouble(jsonString, "danceability");
    energy = ParseJson.getDouble(jsonString, "energy");
    instrumentalness = ParseJson.getDouble(jsonString, "instrumentalness");
    liveness = ParseJson.getDouble(jsonString, "liveness");
    speechiness = ParseJson.getDouble(jsonString, "speechiness");
    valence = ParseJson.getDouble(jsonString, "valence");
    loudness = ParseJson.getDouble(jsonString, "loudness");
    tempo = ParseJson.getDouble(jsonString, "tempo");
    duration_ms = ParseJson.getInt(jsonString, "duration_ms");
    key = ParseJson.getInt(jsonString, "key");
    mode = ParseJson.getInt(jsonString, "mode");
    time_signature = ParseJson.getInt(jsonString, "time_signature");
    this.trackId = trackId;
    this.artistsID = artistsId;
    this.genres = genres;
    this.trackName = trackName;
  }

  public SpotifyAnalysis(double acousticness, double danceability, double energy,
      double instrumentalness, double liveness, double speechiness, double valence, double loudness,
      double tempo, int duration_ms, int key, int mode, int time_signature, String trackId) {
    this.acousticness = acousticness;
    this.danceability = danceability;
    this.energy = energy;
    this.instrumentalness = instrumentalness;
    this.liveness = liveness;
    this.speechiness = speechiness;
    this.valence = valence;
    this.loudness = loudness;
    this.tempo = tempo;
    this.duration_ms = duration_ms;
    this.key = key;
    this.mode = mode;
    this.time_signature = time_signature;
    this.trackId = trackId;
    this.artistsID = new String[] {""};
    this.genres = new String[]{""};
    this.trackName = "";
  }

  //endregion

  //region Methods
  public String getTrackId() {
    return trackId;
  }

  public String getTrackName() { return trackName;}

  public String[] getArtistsID(){
    return artistsID;
  }

  public String[] getGenres(){
    return genres;
  }

  @Override
  public double compareTo(SoundAnalysis other) {
      if (!(other instanceof SpotifyAnalysis otherSpotify)) {
          throw new IllegalArgumentException("Incompatible sound analysis types.");
      }

    double difference = 0.0;

    // 0.0-1.0 Values: Just subtract to get a value between 0.0-1.0.
    difference += Math.pow(Math.abs(otherSpotify.acousticness - this.acousticness),
        DIFFERENCE_EXPONENT);
    difference += Math.pow(Math.abs(otherSpotify.danceability - this.danceability),
        DIFFERENCE_EXPONENT);
    difference += Math.pow(Math.abs(otherSpotify.energy - this.energy), DIFFERENCE_EXPONENT);
    difference += Math.pow(Math.abs(otherSpotify.instrumentalness - this.instrumentalness),
        DIFFERENCE_EXPONENT);
    difference += Math.pow(Math.abs(otherSpotify.liveness - this.liveness), DIFFERENCE_EXPONENT);
    difference += Math.pow(Math.abs(otherSpotify.speechiness - this.speechiness),
        DIFFERENCE_EXPONENT);
    difference += Math.pow(Math.abs(otherSpotify.valence - this.valence), DIFFERENCE_EXPONENT);

    // > 1.0 Values: Use arctan(difference or ratio) to clamp value between 0.0-1.0.
    // If tempos are somehow zero, assume they are very different.
    difference += Math.pow(
        ARCTAN_MULTIPLIER * Math.atan(Math.abs(otherSpotify.loudness - this.loudness)),
        DIFFERENCE_EXPONENT);
      if (this.tempo == 0.0 || otherSpotify.tempo == 0.0) {
          difference += 1.0;
      } else {
          difference += Math.pow(ARCTAN_MULTIPLIER * Math.atan(
              -1 + (Math.max(otherSpotify.tempo, this.tempo) / Math.min(otherSpotify.tempo,
                  this.tempo))), DIFFERENCE_EXPONENT);
      }

    // Integer Values: Calculate 0.0-1.0 difference with separate methods.
    difference += Math.pow(compareDuration(otherSpotify.duration_ms, this.duration_ms),
        DIFFERENCE_EXPONENT);
    difference += Math.pow(compareKey(otherSpotify.key, this.key), DIFFERENCE_EXPONENT);
    difference += Math.pow(compareMode(otherSpotify.mode, this.mode), DIFFERENCE_EXPONENT);
    difference += Math.pow(compareTimeSignature(otherSpotify.time_signature, this.time_signature),
        DIFFERENCE_EXPONENT);

    // Divide by MAX_DIFFERENCE to get match value between 0.0-1.0.
    return 1.0 - (difference / MAX_DIFFERENCE);
  }

  // These methods return 1.0 for different values, not similar.

  // Use same method as > 1.0 values but for integers.
  // Surely durations won't be zero.
  private static double compareDuration(int otherDuration, int thisDuration) {
    return ARCTAN_MULTIPLIER * Math.atan(Math.abs(
        1.0 - ((double) Math.max(otherDuration, thisDuration) / Math.min(otherDuration,
            thisDuration))));
  }

  // Musical notes are circular, so 0/11 (C, B) are closer than 0/6 (C, F#).
  // -1 means Spotify doesn't know. If both are -1, it might be the same song.
  private static double compareKey(int otherKey, int thisKey) {
      if (otherKey == -1 && thisKey == -1) {
          return 0.0;
      }
      if (otherKey == -1 || thisKey == -1) {
          return 1.0;
      }
    int MAX_KEY_DIFFERENCE = 6;
    return (double) Math.min(Math.abs(otherKey - thisKey), 12 - Math.abs(otherKey - thisKey))
        / MAX_KEY_DIFFERENCE;
  }

  // Major/minor modes sound very different.
  private static double compareMode(int otherMode, int thisMode) {
    return otherMode == thisMode ? 0.0 : 1.0;
  }

  // All time signatures sound equally very different from each other.
  private static double compareTimeSignature(int otherTimeSignature, int thisTimeSignature) {
    return otherTimeSignature == thisTimeSignature ? 0.0 : 1.0;
  }
  //endregion

  // Tests comparing specified Spotify songs.
  // Creates a playlist for the username in args[0].
  // Songs:
  // Jungle from Terraria: 5KTKq3BrXttaTT7P0RQbQF
  // Long-Lost Chapters from Genshin Impact: 1DMWlAyeAcwzIRsHIq1eT5
  // The Oathkeeper by Lizz Robinett: 7mdmGuGhme4qcx6xZNrWmZ
  // Cherry Blossom Drops by SOOOO: 17lrs2l9qXEuFybi7hSsid
  // This Future by Camellia: 7wSmFJYz8qot1NTqNMYJKK
  // Aegleseeker by Silentroom: 6I7Nu1gsf0NhkzYlyfHa7q
  public static void main(String[] args) {
    // Load track features.
    SpotifyAnalysis[] songs = new SpotifyAnalysis[6];
    double[][] comparisons = new double[songs.length][songs.length];
    songs[0] = SpotifyAPI.getTrackFeatures("5KTKq3BrXttaTT7P0RQbQF");
    songs[1] = SpotifyAPI.getTrackFeatures("1DMWlAyeAcwzIRsHIq1eT5");
    songs[2] = SpotifyAPI.getTrackFeatures("7mdmGuGhme4qcx6xZNrWmZ");
    songs[3] = SpotifyAPI.getTrackFeatures("17lrs2l9qXEuFybi7hSsid");
    songs[4] = SpotifyAPI.getTrackFeatures("7wSmFJYz8qot1NTqNMYJKK");
    songs[5] = SpotifyAPI.getTrackFeatures("6I7Nu1gsf0NhkzYlyfHa7q");

    // Make comparisons.
    DecimalFormat format = new DecimalFormat("0.00%");
      for (int i = 0; i < songs.length; i++) {
          for (int j = 0; j < songs.length; j++) {
              comparisons[i][j] = songs[i].compareTo(songs[j]);
          }
      }

    // Find closest/furthest matches between different songs.
    int maxSong1 = 0, maxSong2 = 0;
    int minSong1 = 0, minSong2 = 0;
    double max = 0.0, min = 1.0;
    for (int i = 0; i < songs.length; i++) {
      for (int j = 0; j < songs.length; j++) {
        if (i != j && comparisons[i][j] > max) {
          max = comparisons[i][j];
          maxSong1 = i;
          maxSong2 = j;
        }
        if (comparisons[i][j] < min) {
          min = comparisons[i][j];
          minSong1 = i;
          minSong2 = j;
        }
      }
    }
    System.out.println("SpotifyAnalysis: Closest song similarity is between songs " +
        maxSong1 + " and " + maxSong2 + " = " + format.format(max));
    System.out.println("SpotifyAnalysis: Furthest song similarity is between songs " +
        minSong1 + " and " + minSong2 + " = " + format.format(min));
    String[] trackIds = new String[6];
    for (int i = 0; i < songs.length; i++) {
      trackIds[i] = "spotify:track:" + songs[i].trackId;
    }

    SpotifyAPI.setUserId(args[0]);
    for (int i = 0; i < songs.length; i++) {
      String[] recommendations = SpotifyAPI.getRecommendations(songs[i]);
      System.out.println("Recommendations: " + Arrays.toString(recommendations) + "Length: " + recommendations.length);
    }
//    SpotifyAPI.createPlaylist(trackIds);

    System.out.println("Random track link: " + Arrays.toString(SpotifyAPI.randomSong(35)));
  }
}
