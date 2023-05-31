package Backend.Analysis;

import Backend.Algorithm.Normalizer;
import Backend.Algorithm.Reader;
import Backend.Algorithm.Reader.Channel;
import Backend.Algorithm.SimpleCharacteristics;
import Backend.Algorithm.Transform;
import Backend.Analysis.AnalysisCompare.CompareResult;
import Backend.Helper.PrintHelper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Ethan Carnahan
 * Compares local sound files using SimpleCharacteristics
 */
public class SimpleAnalysis implements SoundAnalysis {
  //region Fields and public methods
  private SimpleCharacteristics characteristics;
  private final String filePath, fileName;
  // Multiplies the power of loudness/dynamics differences on the match result.
  // Lower values = higher match result.
  private static final double LOUDNESS_WEIGHT = 0.00005, DYNAMICS_SUM_WEIGHT = 0.0000001, DYNAMICS_DIFF_WEIGHT = 0.000001;
  // Higher values = Bigger result difference between big differences and small differences.
  private static final double LOUDNESS_EXPONENT = 1.5, DYNAMICS_SUM_EXPONENT = 2.0, DYNAMICS_DIFF_EXPONENT = 2.5;
  // Multiplies the power of differences on individual frequency bins.
  private static final double LOUDNESS_RECURSION = 0.5, DYNAMICS_SUM_RECURSION = 0.75, DYNAMICS_DIFF_RECURSION = 1.0;
  // Multiplies arctan bounds from pi/2 to 1.
  private static final double ARCTAN_MULTIPLIER = 2.0 / Math.PI;

  // save/load will save a new analysis and load previously analyzed songs.
  public SimpleAnalysis(String filePath, boolean load, boolean save) throws IOException {
    this.filePath = filePath;

    Path path;
    String savePath;
    try {
      path = Paths.get(filePath);
      this.fileName = path.getFileName().toString();
      if(filePath.contains("\\SavedAnalysis\\") && filePath.contains(".simp"))
        savePath = filePath;
      else
        savePath = System.getProperty("user.dir") + "\\SavedAnalysis\\" + fileName;
      path = Paths.get(savePath);
    } catch (InvalidPathException e) {
      throw new IOException("SimpleAnalysis: Invalid filepath - " + e.getMessage());
    }

    if (load) {
      System.out.println("SimpleAnalysis: Loading analysis for " + fileName);
      try {
        this.characteristics = SimpleCharacteristics.load(savePath);
        return;
      } catch (IOException e) {
        System.out.println("SimpleAnalysis: Failed to load file - " + e.getMessage());
      }
    }

    System.out.println("SimpleAnalysis: Analysing new song " + fileName);
    Reader reader = Reader.readFile(filePath);
    Transform transform = new Transform(reader);
    Normalizer normalizer = new Normalizer(transform);
    this.characteristics = new SimpleCharacteristics(normalizer);
    if (save) {
      System.out.println("SimpleAnalysis: Saving analysis to " + savePath);
      Files.createDirectories(path.getParent());
      this.characteristics.write(savePath);
    }
  }

  // Gets all .simp analyses saved in SavedAnalysis folder.
  public static List<SimpleAnalysis> getAllSavedAnalyses() throws IOException {
    File saveFolder = new File(System.getProperty("user.dir") + "\\SavedAnalysis");
    File[] files = saveFolder.listFiles();
    if (files == null)
      throw new IOException("SimpleAnalysis: SavedAnalysis is not directory.");

    List<SimpleAnalysis> result = new ArrayList<>(files.length);
    for (File file : files)
      if (file.getPath().endsWith(".simp"))
        result.add(new SimpleAnalysis(file.getPath(), true, false));

    return result;
  }

  @Override
  public double compareTo(SoundAnalysis other) {
    if (!(other instanceof SimpleAnalysis otherSimple)) {
      throw new IllegalArgumentException("Incompatible sound analysis types.");
    }

    double[] thisLeftLoudness = this.characteristics.getAverageVolume(Channel.LEFT);
    double[] thisRightLoudness = this.characteristics.getAverageVolume(Channel.RIGHT);
    double[] thisLeftRise = this.characteristics.getAverageRisePlusFall(Channel.LEFT);
    double[] thisRightRise = this.characteristics.getAverageRisePlusFall(Channel.RIGHT);
    double[] thisLeftFall = this.characteristics.getAverageRiseMinusFall(Channel.LEFT);
    double[] thisRightFall = this.characteristics.getAverageRiseMinusFall(Channel.RIGHT);
    double[] otherLeftLoudness = otherSimple.characteristics.getAverageVolume(Channel.LEFT);
    double[] otherRightLoudness = otherSimple.characteristics.getAverageVolume(Channel.RIGHT);
    double[] otherLeftRise = otherSimple.characteristics.getAverageRisePlusFall(Channel.LEFT);
    double[] otherRightRise = otherSimple.characteristics.getAverageRisePlusFall(Channel.RIGHT);
    double[] otherLeftFall = otherSimple.characteristics.getAverageRiseMinusFall(Channel.LEFT);
    double[] otherRightFall = otherSimple.characteristics.getAverageRiseMinusFall(Channel.RIGHT);

    System.out.println("SimpleAnalysis: Comparing " + this.fileName + " to " + otherSimple.fileName);

    // if both stereo
    if (thisRightLoudness != null && otherRightLoudness != null)
      return stereoCompare(thisLeftLoudness, otherLeftLoudness, thisLeftRise, otherLeftRise, thisLeftFall, otherLeftFall,
          thisRightLoudness, otherRightLoudness, thisRightRise, otherRightRise, thisRightFall, otherRightFall);
    // if one stereo and one mono
    if (thisRightLoudness != null)
      return stereoToMonoCompare(thisLeftLoudness, thisRightLoudness, otherLeftLoudness,
          thisLeftRise, thisRightRise, otherLeftRise, thisLeftFall, thisRightFall, otherLeftFall);
    if (otherRightLoudness != null)
      return stereoToMonoCompare(otherLeftLoudness, otherRightLoudness, thisLeftLoudness,
          otherLeftRise, otherRightRise, thisLeftRise, otherLeftFall, otherRightFall, thisLeftFall);
    // if both mono
    return monoCompare(thisLeftLoudness, otherLeftLoudness, thisLeftRise, otherLeftRise, thisLeftFall, otherLeftFall);
  }

  public SimpleCharacteristics getCharacteristics() {
    return characteristics;
  }

  public String getFilePath() {
    return filePath;
  }

  public String getFileName() {
    return fileName;
  }
  //endregion

  //region Private methods
  // a/b = SimpleAnalysis Objects
  // L/R/F = Loudness/Rise/Fall
  // 1/2 = Left/Right Channels
  private static double monoCompare(double[] aL, double[] bL, double[] aR, double[] bR, double[] aF, double[] bF) {
    double loudnessDifference = recursiveSumDifferences(aL, bL, 0, aL.length, LOUDNESS_EXPONENT, LOUDNESS_RECURSION) * LOUDNESS_WEIGHT;
    double risePlusFallDifference = recursiveSumDifferences(aR, bR, 0, aR.length, DYNAMICS_SUM_EXPONENT,
        DYNAMICS_SUM_RECURSION) * DYNAMICS_SUM_WEIGHT;
    double riseMinusFallDifference = recursiveSumDifferences(aF, bF, 0, aF.length, DYNAMICS_DIFF_EXPONENT,
        DYNAMICS_DIFF_RECURSION) * DYNAMICS_DIFF_WEIGHT;

    /*System.out.println("loudness = " + loudnessDifference);
    System.out.println("rise+fall = " + risePlusFallDifference);
    System.out.println("rise-fall = " + riseMinusFallDifference);
    System.out.println();*/

    double difference = ARCTAN_MULTIPLIER * Math.atan(loudnessDifference + risePlusFallDifference + riseMinusFallDifference);

    return 1.0 - difference;
  }

  private static double stereoToMonoCompare(double[] aL1, double [] aL2, double[] bL,
      double[] aR1, double[] aR2, double[] bR, double[] aF1, double[] aF2, double[] bF) {
    double leftCompare = monoCompare(aL1, bL, aR1, bR, aF1, bF);
    double rightCompare = monoCompare(aL2, bL, aR2, bR, aF2, bF);
    return (0.5 * leftCompare) + (0.5 * rightCompare);
  }

  private static double stereoCompare(double[] aL1, double[] bL1, double[] aR1, double[] bR1, double[] aF1, double[] bF1,
      double[] aL2, double[] bL2, double[] aR2, double[] bR2, double[] aF2, double[] bF2) {
    double leftCompare = monoCompare(aL1, bL1, aR1, bR1, aF1, bF1);
    double rightCompare = monoCompare(aL2, bL2, aR2, bR2, aF2, bF2);
    return (0.5 * leftCompare) + (0.5 * rightCompare);
  }

  // Sums a/b together and calculates the sum difference, then splits a/b to do it again.
  private static double recursiveSumDifferences(double[] a, double[] b, int start, int length, double exp, double rec) {
    if (length == 0 || start >= a.length)
      return 0.0;
    if (length == 1)
      return Math.pow(Math.abs(a[start] - b[start]), exp);
    double result = Math.pow(Math.abs(sumArray(a, start, length) - sumArray(b, start, length)), exp);
    int nextLength = (int)Math.ceil((double)length / 2.0);
    for (int i = start; i < nextLength; i += 1)
      result += recursiveSumDifferences(a, b, i, nextLength, exp, rec) * rec / nextLength;
    return result;
  }

  private static double sumArray(double[] array, int start, int length) {
    double result = 0.0;
    int end = Math.min(start + length, array.length);
    for (int i = start; i < end; i++)
      result += array[i];
    return result;
  }
  //endregion

  // Compares each sound file in args to each other and sorts results by match percentage.
  // Inputting only one song will compare it to all previously scanned songs (including itself).
  // Inputting no songs will compare all previously scanned songs.
  public static void main(String[] args) {
    // Load arguments
    List<SoundAnalysis> analyses = new ArrayList<>(args.length);
    for (String file : args) {
      try {
        analyses.add(new SimpleAnalysis(file, true, true));
      } catch (Exception e) {
        System.out.println("SimpleAnalysis: Failed to scan file - " + e.getMessage());
      }
    }

    // Compare analyses
    List<CompareResult> results = null;
    long startTime = System.nanoTime();
    if (analyses.size() == 0) {  // No arguments: Compare all saved.
      try {
        List<SimpleAnalysis> others = getAllSavedAnalyses();
        if (others.size() < 2)
          throw new IllegalStateException("SimpleAnalysis: Need at least two saved analyses to compare.");
        results = AnalysisCompare.compareAllAnalyses(others);
        results = AnalysisCompare.mostAndLeastSimilar(results);
      } catch (IOException | IllegalStateException e) {
        System.out.println("SimpleAnalysis: Failed to load saved analyses - " + e.getMessage());
        System.exit(1);
      }
    } else if (analyses.size() == 1) {  // One argument: Compare one against all saved.
      try {
        List<SimpleAnalysis> others = getAllSavedAnalyses();
        results = AnalysisCompare.compareTheseToThoseAnalyses(analyses, others);
      } catch (IOException e) {
        System.out.println("SimpleAnalysis: Failed to load saved analyses - " + e.getMessage());
        System.exit(1);
      }
    } else {  // Multiple arguments: Compare arguments against each other.
      results = AnalysisCompare.compareAllAnalyses(analyses);
      results = AnalysisCompare.mostAndLeastSimilar(results);
    }
    System.out.println("\nCalculation time: " + ((System.nanoTime() - startTime) / 1000000000.0) + " seconds");

    // Print results.
    Collections.reverse(results);
    for (CompareResult result : results) {
      SimpleAnalysis a = (SimpleAnalysis)result.a;
      SimpleAnalysis b = (SimpleAnalysis)result.b;
      System.out.println(a.fileName + " compared to " + b.fileName + " = " +
              PrintHelper.format.format(result.result * 100) + "% match");
    }
  }
}
