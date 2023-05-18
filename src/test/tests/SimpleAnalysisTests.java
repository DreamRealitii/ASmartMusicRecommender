import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import Backend.Analysis.SimpleAnalysis;
import Backend.Analysis.SoundAnalysis;
import java.io.IOException;
import org.junit.jupiter.api.*;

// Tests the overall algorithm for SimpleAnalysis
public class SimpleAnalysisTests {
  private static final double zeroErrorBound = 0.0;
  private static final double smallErrorBound = 0.02;
  private static final double largeErrorBound = 0.2;

  private void testEqual(String file1, String file2, double errorBound) throws IOException {
    SoundAnalysis a = new SimpleAnalysis(file1, false, false);
    SoundAnalysis b = new SimpleAnalysis(file2, false, false);
    assertEquals(1.0, a.compareTo(b), errorBound);
  }

  private void testDifferent(String file1, String file2, double errorBound) throws IOException {
    SoundAnalysis a = new SimpleAnalysis(file1, false, false);
    SoundAnalysis b = new SimpleAnalysis(file2, false, false);
    assertEquals(0.0, a.compareTo(b), errorBound);
  }

  private void testMoreDifferent(String base, String similar, String different) throws IOException {
    SoundAnalysis a = new SimpleAnalysis(base, false, false);
    SoundAnalysis b = new SimpleAnalysis(similar, false, false);
    SoundAnalysis c = new SimpleAnalysis(different, false, false);
    assertTrue(a.compareTo(b) > a.compareTo(c));
  }

  @Test
  public void testReflexive() throws IOException {
    SoundAnalysis sa = new SimpleAnalysis("src/test/resource/white_noise.wav", false, false);
    assertEquals(1.0, sa.compareTo(sa), zeroErrorBound);
  }

  @Test
  public void testSymmetric() throws IOException {
    SoundAnalysis a = new SimpleAnalysis("src/test/resource/white_noise.wav", false, false);
    SoundAnalysis b = new SimpleAnalysis("src/test/resource/pink_noise.wav", false, false);
    assertEquals(a.compareTo(b), b.compareTo(a), zeroErrorBound);
  }

  // Passing Requirement: Same sound with reversed polarity has ~100% match result.
  @Test
  public void testPolarity() throws IOException {
    testEqual("src/test/resource/white_noise.wav", "src/test/resource/white_noise_inverted.wav", smallErrorBound);
  }

  // Passing Requirement: Same sound in .wav and .mp3 format has ~100% match result.
  @Test
  public void testFormats() throws IOException {
    testEqual("src/test/resource/Blue_Pearl.mp3", "src/test/resource/Blue_Pearl.wav", smallErrorBound);
  }

  // Passing Requirement: Same sound with different volume has ~100% match result.
  @Test
  public void testVolume() throws IOException {
    testEqual("src/test/resource/pink_noise.wav", "src/test/resource/pink_noise_quiet.wav", smallErrorBound);
  }

  // Passing Requirement: Two silent sounds with different length have 100% match result.
  // Removed for now because I can't get Audacity to output actual silence.
  /*
  @Test
  public void testSilence() throws IOException {
    testEqual("src/test/resource/silence_short.wav", "src/test/resource/silence_long.wav", zeroErrorBound);
  }
  */

  // Passing Requirement: Two white noise samples have nearly 100% match result.
  @Test
  public void testEquivalentNoise() throws IOException {
    testEqual("src/test/resource/white_noise.wav", "src/test/resource/white_noise_2.wav", smallErrorBound);
  }

  // Passing Requirement: An audible and silent sound have nearly 0% match result.
  @Test
  public void testSilenceVersusNoise() throws IOException {
    testDifferent("src/test/resource/white_noise.wav", "src/test/resource/silence_long.wav", largeErrorBound);
  }

  // Passing Requirement: A song played backwards does not match against the original.
  @Test
  public void testReverse() throws IOException {
    testDifferent("src/test/resource/Blue_Pearl.wav", "src/test/resource/Blue_Pearl_Reversed.mp3", largeErrorBound);
  }

  // Passing Requirement: More reverb applied to same sound = lower match result.
  @Test
  public void testReverb() throws IOException {
    testMoreDifferent("src/test/resource/Blue_Pearl.wav",
        "src/test/resource/Blue_Pearl_Less_Reverb.mp3",
        "src/test/resource/Blue_Pearl_More_Reverb.mp3");
  }

  // Passing Requirement: More compression on same sound = lower match result.
  @Test
  public void testCompression() throws IOException {
    testMoreDifferent("src/test/resource/Blue_Pearl.wav",
        "src/test/resource/Blue_Pearl_Less_Compressed.mp3",
        "src/test/resource/Blue_Pearl_More_Compressed.mp3");
  }

  // Passing Requirement: More pitch-shift on same sound = lower match result.
  @Test
  public void testPitch() throws IOException {
    testMoreDifferent("src/test/resource/Blue_Pearl.wav",
        "src/test/resource/Blue_Pearl_Less_Pitch.mp3",
        "src/test/resource/Blue_Pearl_More_Pitch.mp3");
  }

  // Passing Requirement: More tempo-shift on same sound = lower match result.
  @Test
  public void testTempo() throws IOException {
    testMoreDifferent("src/test/resource/Blue_Pearl.wav",
        "src/test/resource/Blue_Pearl_Less_Tempo.mp3",
        "src/test/resource/Blue_Pearl_More_Tempo.mp3");
  }

  // Passing Requirement: A cover of a song matches closely to the original.
  @Test
  public void testCover() throws IOException {
    testEqual("src/test/resource/Ghost_Original.mp3", "src/test/resource/Ghost_Halloween.mp3", largeErrorBound);
  }
}
