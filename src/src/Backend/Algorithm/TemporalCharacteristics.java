package Backend.Algorithm;

import Backend.Algorithm.Reader.Channel;
import Backend.Helper.PrintHelper;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author Ethan Carnahan
 * Advanced sound analysis that analyzes:
 * - How a frequency bin's volume changes correlates with the volume changes of other bins over time.
 * - The best-matching frequencies of each bin's peaks (BPM).
 */
public class TemporalCharacteristics extends SimpleCharacteristics {
  //region Fields and public methods
  // Dimensions from left-to-right: [This bin][Bin compared to][Number of samples ahead]
  private final float[][][] leftCorrelaton, rightCorrelation;
  // Dimensions from left-to-right: [This bin][Possible peak frequency]
  private final double[][] leftPeakRates, rightPeakRates;
  // Number of samples to look ahead.
  private static final int CORRELATION_SAMPLES = (int)Math.round(Transform.TIME_RESOLUTION * 1); // From 0 to 1 second ahead
  // Which peak rates to check in beats-per-minute.
  public static final int RATE_MIN = 15;
  public static final int RATE_MAX = 300; // Needs to be less than (60 * Transform.TIME_RESOLUTION / 3).

  public TemporalCharacteristics(Normalizer normalizer) {
    super(normalizer);
    float[][] left = normalizer.getNormalized(Channel.LEFT);
    float[][] right = normalizer.getNormalized(Channel.RIGHT);

    System.out.println("TemporalCharacteristics: Calculating characteristics");

    leftCorrelaton = calculateCorrelation(left, getAverageVolume(Channel.LEFT));
    leftPeakRates = calculatePeakRates(left, getAverageVolume(Channel.LEFT));
    if (right != null) {
      rightCorrelation = calculateCorrelation(right, getAverageVolume(Channel.RIGHT));
      rightPeakRates = calculatePeakRates(right, getAverageVolume(Channel.RIGHT));
    } else {
      rightCorrelation = null;
      rightPeakRates = null;
    }
  }

  // Used for loading
  private TemporalCharacteristics(SimpleCharacteristics simple, float[][][] leftCorrelaton, float[][][] rightCorrelation,
      double[][] leftPeakRates, double[][] rightPeakRates) {
    super(simple.leftVolume, simple.rightVolume, simple.leftRisePlusFall, simple.rightRisePlusFall,
        simple.leftRiseMinusFall, simple.rightRiseMinusFall);
    this.leftCorrelaton = leftCorrelaton;
    this.rightCorrelation = rightCorrelation;
    this.leftPeakRates = leftPeakRates;
    this.rightPeakRates = rightPeakRates;
  }

  public float[][][] getCorrelation(Channel channel) {
    return (channel == Channel.LEFT ? leftCorrelaton : rightCorrelation);
  }

  public double[][] getPeakRates(Channel channel) {
    return (channel == Channel.LEFT ? leftPeakRates : rightPeakRates);
  }

  public void write(String filepath) throws IOException {
    super.write(filepath);

    // Write temporary raw file
    File tempFile = new File(filepath + ".temp");
    tempFile.createNewFile();
    BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));

    writer.write(rightCorrelation != null ? "Stereo" : "Mono");
    writer.newLine();
    writeCorrelationArray(writer, leftCorrelaton);
    writePeakRateArray(writer, leftPeakRates);
    if (rightCorrelation != null) {
      writeCorrelationArray(writer, rightCorrelation);
      writePeakRateArray(writer, rightPeakRates);
    }

    writer.flush();
    writer.close();

    // Compress file - See https://www.digitalocean.com/community/tutorials/java-gzip-example-compress-decompress-file
    File compFile = new File(filepath + ".tem");
    compFile.createNewFile();
    FileInputStream inputStream = new FileInputStream(tempFile);
    FileOutputStream outputStream = new FileOutputStream(compFile);
    GZIPOutputStream zipOut = new GZIPOutputStream(outputStream);

    byte[] buffer = new byte[65536];
    while (inputStream.read(buffer) != -1)
      zipOut.write(buffer);

    zipOut.flush();
    zipOut.close();
    outputStream.close();
    inputStream.close();
    tempFile.delete();
  }

  public static TemporalCharacteristics load(String filepath) throws IOException {
    SimpleCharacteristics sc = SimpleCharacteristics.load(filepath);

    if (!filepath.contains(".tem"))
      filepath = filepath + ".tem";

    FileInputStream inputStream = new FileInputStream(filepath);
    GZIPInputStream zipIn = new GZIPInputStream(inputStream);
    BufferedReader reader = new BufferedReader(new InputStreamReader(zipIn));

    boolean stereo = reader.readLine().equals("Stereo");

    float[][][] lc = loadCorrelationArray(reader);
    double[][] lr = loadPeakRateArray(reader);
    float[][][] rc = null;
    double[][] rr = null;
    if (stereo) {
      rc = loadCorrelationArray(reader);
      rr = loadPeakRateArray(reader);
    }

    return new TemporalCharacteristics(sc, lc, rc, lr, rr);
  }
  //endregion

  //region Private methods
  private static float[][][] calculateCorrelation(float[][] channel, double[] averageVolume) {
    CorrelationTask task = new CorrelationTask(channel, averageVolume, 0, Transform.FREQUENCY_RESOLUTION);
    try (ForkJoinPool fjp = new ForkJoinPool()) {
      return fjp.invoke(task);
    }
  }

  private static class CorrelationTask extends RecursiveTask<float[][][]> {
    private final float[][] channel;
    private final double[] averageVolume;
    private final int start, end;
    private static final int THRESHOLD = 1;

    public CorrelationTask(float[][] channel, double[] averageVolume, int start, int end) {
      this.channel = channel;
      this.averageVolume = averageVolume;
      this.start = start;
      this.end = end;
    }

    @Override
    protected float[][][] compute() {
      int length = end - start;
      if (length <= THRESHOLD)
        return partialCorrelation();

      CorrelationTask task1 = new CorrelationTask(channel, averageVolume, start, start + (length/2));
      task1.fork();
      CorrelationTask task2 = new CorrelationTask(channel, averageVolume, start + (length/2), end);
      float[][][] result2 = task2.compute();
      float[][][] result1 = task1.join();

      return joinArrays(result1, result2);
    }

    private float[][][] partialCorrelation() {
      float[][][] result = new float[end - start][Transform.FREQUENCY_RESOLUTION][CORRELATION_SAMPLES];
      for (int i = start; i < end; i++)
        for (int j = 0; j < result[0].length; j++)
          for (int k = 0; k < result[0][0].length; k++)
            result[i - start][j][k] = (float) correlation(channel, i, j, k, averageVolume);
      return result;
    }

    private static float[][][] joinArrays(float[][][] a, float[][][] b) {
      float[][][] result = new float[a.length + b.length][a[0].length][a[0][0].length];

      for (int i = 0; i < a.length; i++)
        for (int j = 0; j < a[0].length; j++)
          System.arraycopy(a[i][j], 0, result[i][j], 0, a[0][0].length);

      for (int i = 0; i < b.length; i++)
        for (int j = 0; j < b[0].length; j++)
          System.arraycopy(b[i][j], 0, result[i + a.length][j], 0, b[0][0].length);

      return result;
    }
  }

  // Pearson correlation.
  private static double correlation(float[][] channel, int binA, int binB, int samplesAhead, double[] averageVolume) {
    if (binA == binB && samplesAhead == 0)
      return 1.0;

    /*double result = 0.0;

    for (int i = 1; i < channel.length - samplesAhead; i++) {
      double a = channel[i][binA] - channel[i - 1][binA];
      double b = channel[i + samplesAhead][binB] - channel[i + samplesAhead - 1][binB];
      result += a * b;
    }

    return result / ((channel.length - samplesAhead - 1) * averageVolume[binA]);*/

    double sumX = 0.0, sumY = 0.0, sumXY = 0.0;

    for (int i = 1; i < channel.length - samplesAhead; i++) {
      double x = (channel[i][binA] - channel[i - 1][binA]);
      double y = (channel[i + samplesAhead][binB] - channel[i + samplesAhead - 1][binB]);
      sumX += x * x;
      sumY += y * y;
      sumXY += x * y;
    }

    return sumXY / Math.sqrt(sumX * sumY);
  }

  private static double[][] calculatePeakRates(float[][] channel, double[] averageVolume) {
    PeakRatesTask task = new PeakRatesTask(channel, averageVolume, 0, Transform.FREQUENCY_RESOLUTION);
    try (ForkJoinPool fjp = new ForkJoinPool()) {
      return fjp.invoke(task);
    }
  }

  private static class PeakRatesTask extends RecursiveTask<double[][]> {
    private final float[][] channel;
    private final double[] averageVolume;
    private final int start, end;
    private static final int THRESHOLD = 1;

    public PeakRatesTask(float[][] channel, double[] averageVolume, int start, int end) {
      this.channel = channel;
      this.averageVolume = averageVolume;
      this.start = start;
      this.end = end;
    }

    @Override
    protected double[][] compute() {
      int length = end - start;
      if (length <= THRESHOLD)
        return partialPeakRates();

      PeakRatesTask task1 = new PeakRatesTask(channel, averageVolume, start, start + (length/2));
      task1.fork();
      PeakRatesTask task2 = new PeakRatesTask(channel, averageVolume, start + (length/2), end);
      double[][] result2 = task2.compute();
      double[][] result1 = task1.join();

      return joinArrays(result1, result2);
    }

    private double[][] partialPeakRates() {
      double[][] result = new double[end - start][RATE_MAX - RATE_MIN + 1];
      for (int i = start; i < end; i++)
        for (int j = 0; j < result[0].length; j++)
          result[i - start][j] = peakRateMatch(channel, i, RATE_MIN + j, averageVolume);
      return result;
    }

    private double[][] joinArrays(double[][] a, double[][] b) {
      double[][] result = new double[a.length + b.length][a[0].length];

      for (int i = 0; i < a.length; i++)
        System.arraycopy(a[i], 0, result[i], 0, a[0].length);
      for (int i = 0; i < b.length; i++)
        System.arraycopy(b[i], 0, result[i + a.length], 0, b[0].length);

      return result;
    }
  }

  // Peak rate strategy: Use lots of interpolation.
  // Generate a peak detection window for each BPM, with {-1.0, 1.0, 1.0, -1.0} being the smallest possible window.
  // Slide window across channel to generate "is peak" values for each sample.
  // Iterate across "is peak" array by different amounts for each BPM to calculate match amount.
  private static double peakRateMatch(float[][] channel, int bin, int rate, double[] averageVolume) {
    double result = 0.0;
    double samplesPerBeat = Transform.TIME_RESOLUTION / (rate / 60.0);

    double[] window = getWindow(rate);
    double[] peaks = getPeaks(channel, bin, window);

    double start, end;
    for (double i = 0; i < channel.length - samplesPerBeat - 1; i++) {
      start = interpolate(peaks, i);
      end = interpolate(peaks, i + samplesPerBeat);
      if (start > averageVolume[bin] * 2 && end > averageVolume[bin] * 2)
        result += 100;
    }

    return result / ((channel.length - samplesPerBeat - 1));
  }

  private static double[] getWindow(int rate) {
    // convert rate from BPM to minimum required odd window length
    int windowLength = (int)Math.ceil(Transform.TIME_RESOLUTION / (rate / 60.0));
    if (windowLength % 2 == 1) windowLength++;

    // calculate window values
    double negativeValues = -2.0 / (windowLength - 2);

    // create window
    double[] result = new double[windowLength];
    Arrays.fill(result, negativeValues);
    result[(windowLength - 1) / 2] = 1.0;
    result[windowLength / 2] = 1.0;

    return result;
  }

  // Runs peak detection window through transform samples.
  private static double[] getPeaks(float[][] channel, int bin, double[] window) {
    double[] result = new double[channel.length];

    for (int i = window.length / 2; i < result.length - (window.length / 2); i++) {
      double windowSum = 0.0;
      for (int j = 0; j < window.length; j++)
        windowSum += window[j] * channel[i + j - (window.length / 2)][bin];
      result[i] = windowSum;
    }

    return result;
  }

  private static double interpolate(double[] channel, double index) {
    int bottomIndex = (int)Math.floor(index);
    int topIndex = (int)Math.ceil(index);

    double bottomToTopIndex = index - bottomIndex;
    double bottomToTopValue = channel[topIndex] - channel[bottomIndex];

    return (channel[bottomIndex] + (bottomToTopValue * bottomToTopIndex));
  }

  private static void writeCorrelationArray(BufferedWriter writer, float[][][] array) throws IOException {
    for (float[][] twoD : array) {
      for (float[] oneD : twoD) {
        for (float value : oneD) {
          writer.write(String.valueOf(value));
          writer.newLine();
        }
      }
    }
  }

  private static float[][][] loadCorrelationArray(BufferedReader reader) throws IOException {
    float[][][] result = new float[Transform.FREQUENCY_RESOLUTION][Transform.FREQUENCY_RESOLUTION][CORRELATION_SAMPLES];
    for (int i = 0; i < result.length; i++)
      for (int j = 0; j < result[0].length; j++)
        for (int k = 0; k < result[0][0].length; k++)
          result[i][j][k] = Float.parseFloat(reader.readLine());
    return result;
  }

  private static void writePeakRateArray(BufferedWriter writer, double[][] array) throws IOException {
    for (double[] oneD : array) {
      for (double value : oneD) {
        writer.write(String.valueOf(value));
        writer.newLine();
      }
    }
  }

  private static double[][] loadPeakRateArray(BufferedReader reader) throws IOException {
    double[][] result = new double[Transform.FREQUENCY_RESOLUTION][RATE_MAX - RATE_MIN + 1];
    for (int i = 0; i < result.length; i++)
      for (int j = 0; j < result[0].length; j++)
        result[i][j] = Double.parseDouble(reader.readLine());
    return result;
  }
  //endregion

  // Prints the average correlation/peak rate information of the audio file in args[0].
  public static void main(String[] args) {
    try {
      Reader reader = Reader.readFile(args[0]);
      Transform transform = new Transform(reader);
      Normalizer normalizer = new Normalizer(transform);
      long startTime = System.nanoTime();
      TemporalCharacteristics temporalCharacteristics = new TemporalCharacteristics(normalizer);
      System.out.println("Calculation time: " + ((System.nanoTime() - startTime) / 1000000000.0) + " seconds");
      System.out.println("Left channel characteristics:");

      System.out.println("Correlation (same time only):");
      for (int i = 0; i < Transform.FREQUENCY_RESOLUTION; i += 4) {
        for (int j = 0; j < Transform.FREQUENCY_RESOLUTION; j += 4) {
          System.out.print(PrintHelper.format.format(Transform.frequencyAtBin(i)) +
              " with " + PrintHelper.format.format(Transform.frequencyAtBin(j)) + ": ");
          System.out.println(PrintHelper.format.format(temporalCharacteristics.getCorrelation(Channel.LEFT)[i][j][0]));
        }
      }
      System.out.println();

      double[] bpms = new double[RATE_MAX - RATE_MIN + 1];
      for (int i = 0; i < bpms.length; i++)
        bpms[i] = RATE_MIN + i;
      PrintHelper.printValues("BPM Matches", bpms);
      for (int i = 0; i < Transform.FREQUENCY_RESOLUTION; i += 4)
        PrintHelper.printValues(PrintHelper.format.format(Transform.frequencyAtBin(i)) +
            "hz", temporalCharacteristics.getPeakRates(Channel.LEFT)[i]);
    } catch (IOException e) {
      e.printStackTrace();
      System.out.println(e.getMessage());
    }
  }
}
