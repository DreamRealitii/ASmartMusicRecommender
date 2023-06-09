package Backend.Analysis;

import Backend.Algorithm.Normalizer;
import Backend.Algorithm.Reader;
import Backend.Algorithm.Reader.Channel;
import Backend.Algorithm.TemporalCharacteristics;
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
 * The sound analysis of a song given by our own algorithms.
 */
public class FullAnalysis implements SoundAnalysis {
  //region Fields and public methods
  private TemporalCharacteristics characteristics;
  private final String filePath, fileName;
  private static final double CORRELATION_WEIGHT = 5000.0, PEAKRATE_WEIGHT = 0.0;
  private static final double CORRELATION_EXPONENT = 3.0, PEAKRATE_EXPONENT = 2.5;
  private static final double ARCTAN_MULTIPLIER = 2.0 / Math.PI;

  public FullAnalysis(String filePath, boolean load, boolean save) throws IOException {
    this.filePath = filePath;

    Path path;
    String savePath;
    try {
      path = Paths.get(filePath);
      this.fileName = path.getFileName().toString();
      if(filePath.contains("\\SavedAnalysis\\") && filePath.contains(".tem"))
        savePath = filePath.substring(0, filePath.length()-4);
      else
        savePath = System.getProperty("user.dir") + "\\SavedAnalysis\\" + fileName;
      path = Paths.get(savePath);
    } catch (InvalidPathException e) {
      throw new IOException("FullAnalysis: Invalid filepath - " + e.getMessage());
    }

    if (load) {
      System.out.println("FullAnalysis: Loading analysis for " + fileName);
      try {
        this.characteristics = TemporalCharacteristics.load(savePath);
        return;
      } catch (IOException e) {
        System.out.println("FullAnalysis: Failed to load file - " + e.getMessage());
      }
    }

    System.out.println("FullAnalysis: Analysing new song " + fileName);
    Reader reader = Reader.readFile(filePath);
    Transform transform = new Transform(reader);
    Normalizer normalizer = new Normalizer(transform);
    this.characteristics = new TemporalCharacteristics(normalizer);
    if (save) {
      System.out.println("FullAnalysis: Saving analysis to " + savePath);
      Files.createDirectories(path.getParent());
      this.characteristics.write(savePath);
    }
  }

  // Gets all .tem analyses saved in SavedAnalysis folder.
  public static List<FullAnalysis> getAllSavedAnalyses() throws IOException {
    File saveFolder = new File(System.getProperty("user.dir") + "\\SavedAnalysis");
    File[] files = saveFolder.listFiles();
    if (files == null)
      throw new IOException("FullAnalysis: SavedAnalysis is not directory.");

    List<FullAnalysis> result = new ArrayList<>(files.length);
    for (File file : files)
      if (file.getPath().endsWith(".tem"))
        result.add(new FullAnalysis(file.getPath(), true, false));

    return result;
  }

  @Override
  public double compareTo(SoundAnalysis other) {
    if (!(other instanceof FullAnalysis otherFull)) {
      throw new IllegalArgumentException("Incompatible sound analysis types.");
    }

    float[][][] thisLeftCorrelation = this.characteristics.getCorrelation(Channel.LEFT);
    float[][][] thisRightCorrelation = this.characteristics.getCorrelation(Channel.RIGHT);
    double[][] thisLeftPeakRates = this.characteristics.getPeakRates(Channel.LEFT);
    double[][] thisRightPeakRates = this.characteristics.getPeakRates(Channel.RIGHT);
    float[][][] otherLeftCorrelation = otherFull.characteristics.getCorrelation(Channel.LEFT);
    float[][][] otherRightCorrelation = otherFull.characteristics.getCorrelation(Channel.RIGHT);
    double[][] otherLeftPeakRates = otherFull.characteristics.getPeakRates(Channel.LEFT);
    double[][] otherRightPeakRates = otherFull.characteristics.getPeakRates(Channel.RIGHT);

    System.out.println("FullAnalysis: Comparing " + this.fileName + " to " + otherFull.fileName);

    // if both stereo
    if (thisRightCorrelation != null && otherRightCorrelation != null)
      return stereoCompare(thisLeftCorrelation, otherLeftCorrelation, thisLeftPeakRates, otherLeftPeakRates,
          thisRightCorrelation, otherRightCorrelation, thisRightPeakRates, otherRightPeakRates);
    // if one stereo and one mono
    if (thisRightCorrelation != null)
      return stereoToMonoCompare(thisLeftCorrelation, thisRightCorrelation, thisLeftPeakRates,
          thisRightPeakRates, otherLeftCorrelation, otherLeftPeakRates);
    if (otherRightCorrelation != null)
      return stereoToMonoCompare(otherLeftCorrelation, otherRightCorrelation, otherLeftPeakRates,
          otherRightPeakRates, thisLeftCorrelation, thisLeftPeakRates);
    // if both mono
    return monoCompare(thisLeftCorrelation, otherLeftCorrelation, thisLeftPeakRates, otherLeftPeakRates);
  }

  public TemporalCharacteristics getCharacteristics() {
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
  // a/b = FullAnalysis Objects
  // C/R = Correlation/Peak Rates
  // 1/2 = Left/Right Channels
  private static double monoCompare(float[][][] aC, float[][][] bC, double[][] aR, double[][] bR) {
    double correlationDifference = correlationDifferences(aC, bC, CORRELATION_EXPONENT) * CORRELATION_WEIGHT;
    double peakRateDifference = peakRateDifferences(aR, bR, PEAKRATE_EXPONENT) * PEAKRATE_WEIGHT;

    System.out.println("correlation difference = " + correlationDifference);
    System.out.println("peak rates difference  = " + peakRateDifference);
    System.out.println();

    double difference = ARCTAN_MULTIPLIER * Math.atan(correlationDifference + peakRateDifference);

    return 1.0 - difference;
  }

  private static double stereoToMonoCompare(float[][][] aC1, float[][][] aC2, double[][] aR1,
      double[][] aR2, float[][][] bC, double[][] bR) {
    double leftCompare = monoCompare(aC1, bC, aR1, bR);
    double rightCompare = monoCompare(aC2, bC, aR2, bR);
    return (0.5 * leftCompare) + (0.5 * rightCompare);
  }

  private static double stereoCompare(float[][][] aC1, float[][][] bC1, double[][] aR1, double[][] bR1,
      float[][][] aC2, float[][][] bC2, double[][] aR2, double[][] bR2) {
    double leftCompare = monoCompare(aC1, bC1, aR1, bR1);
    double rightCompare = monoCompare(aC2, bC2, aR2, bR2);
    return (0.5 * leftCompare) + (0.5 * rightCompare);
  }

  // Sums a/b together and calculates the sum difference, then splits a/b to do it again.
  private static double correlationDifferences(float[][][] a, float[][][] b, double exp) {
    double result = 0.0;

    for (int i = 0; i < a.length; i++)
      for (int j = 0; j < a[0].length; j++)
        for (int k = 0; k < a[0][0].length; k++)
          result += Math.pow(Math.abs(a[i][j][k] - b[i][j][k]), exp) / ((k * k) + 1);

    return result / (a.length * a[0].length * a[0][0].length);
  }

  private static double peakRateDifferences(double[][] a, double[][] b, double exp) {
    double result = 0.0;

    for (int i = 0; i < a.length; i++)
      for (int j = 0; j < a[0].length; j++)
        result += Math.pow(Math.abs(a[i][j] - b[i][j]), exp);

    return result / (a.length * a[0].length);
  }

  // Compares each sound file in args to each other and sorts results by match percentage.
  // Inputting only one song will compare it to all previously scanned songs (including itself).
  // Inputting no songs will compare all previously scanned songs.
  public static void main(String[] args) {
    // Load arguments
    List<FullAnalysis> analyses = new ArrayList<>(args.length);
    for (String file : args) {
      try {
        analyses.add(new FullAnalysis(file, true, true));
      } catch (Exception e) {
        System.out.println("FullAnalysis: Failed to scan file - " + e.getMessage());
      }
    }

    // Compare analyses
    List<CompareResult> results = null;
    long startTime = System.nanoTime();
    if (analyses.size() == 0) {  // No arguments: Compare all saved.
      try {
        List<FullAnalysis> others = getAllSavedAnalyses();
        if (others.size() < 2)
          throw new IllegalStateException("FullAnalysis: Need at least two saved analyses to compare.");
        results = AnalysisCompare.compareAllAnalyses(others);
        results = AnalysisCompare.mostAndLeastSimilar(results);
      } catch (IOException | IllegalStateException e) {
        System.out.println("FullAnalysis: Failed to load saved analyses - " + e.getMessage());
        System.exit(1);
      }
    } else if (analyses.size() == 1) {  // One argument: Compare one against all saved.
      try {
        List<FullAnalysis> others = getAllSavedAnalyses();
        results = AnalysisCompare.compareTheseToThoseAnalyses(analyses, others);
      } catch (IOException e) {
        System.out.println("FullAnalysis: Failed to load saved analyses - " + e.getMessage());
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
      FullAnalysis a = (FullAnalysis)result.a;
      FullAnalysis b = (FullAnalysis)result.b;
      System.out.println(a.fileName + " compared to " + b.fileName + " = " +
          PrintHelper.format.format(result.result * 100) + "% match");
    }
  }
}
