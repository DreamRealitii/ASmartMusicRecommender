package Backend.Algorithm;

import Backend.Algorithm.Reader.Channel;
import Backend.Helper.PrintHelper;
import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;

/**
 * @author Ethan Carnahan
 * Sets a sound to a specified average volume, and then
 * converts fourier transform amplitudes from actual loudness to perceived loudness.
 * Perceived loudness is arbitrarily set so 0 phons = 100 loudness.
 */
public class Normalizer {
  //region Fields and public method
  private final float[][] normalizedLeft, normalizedRight;
  // A full amplitude sine wave will be treated as this volume.
  private static final double dbOfMax = 90;
  // The normalizer will try to set a fourier transform to this perceived volume +- errorBound.
  private static final double targetVolume = 256; // 80 phons
  private static final double errorBound = 0.001, ratioMultiplier = 2;//, spreadDamp = 1;

  public Normalizer(Transform transform) {
    float[][] left = transform.getFrequencyAmplitudes(Channel.LEFT);
    float[][] right = transform.getFrequencyAmplitudes(Channel.RIGHT);

    System.out.println("Normalizer: Running normalization on transform of " + left.length + " samples");

    normalizedLeft = normalizeTransform(left);
    normalizedRight = normalizeTransform(right);
  }

  public Normalizer(float[][] left, float[][] right) {
    System.out.println("Normalizer: Running normalization on transform of " + left.length + " samples");

    normalizedLeft = normalizeTransform(left);
    normalizedRight = normalizeTransform(right);
  }

  public float[][] getNormalized(Channel channel) {
    return (channel == Channel.LEFT ? normalizedLeft : normalizedRight);
  }
  //endregion

  //region Private methods
  private static float[][] normalizeTransform(float[][] channel) {
    // Check null
    if (channel == null)
      return null;

    // Copy array
    float[][] result = new float[channel.length][channel[0].length];
    for (int i = 0; i < channel.length; i++)
      System.arraycopy(channel[i], 0, result[i], 0, channel[0].length);

    // Do nothing for silence
    double currentVolume = getOverallVolume(channel);
    if (currentVolume == 0)
      return result;

    // Find correct volume
    while (Math.abs(currentVolume - targetVolume) > errorBound) {
      float multiplier = (float) (1 + (((targetVolume / currentVolume) - 1) * ratioMultiplier));
      multiply2DArray(result, multiplier);
      currentVolume = getOverallVolume(result);
    }

    return loudnessToPerceivedLoudness(result);
  }

  private static double getOverallVolume(float[][] channel) {
    GetVolumeTask task = new GetVolumeTask(channel, 0, channel.length);
    try (ForkJoinPool fjp = new ForkJoinPool()) {
      return fjp.invoke(task) / (channel.length * Transform.FREQUENCY_RESOLUTION);
    }
  }

  // Needs to be divided by channel.length and Transform.FREQUENCY_RESOLUTION afterwards.
  private static class GetVolumeTask extends RecursiveTask<Double> {
    private final float[][] channel;
    private final int start, end;
    private static final int THRESHOLD = 1;

    public GetVolumeTask(float[][] channel, int start, int end) {
      this.channel = channel;
      this.start = start;
      this.end = end;
    }

    @Override
    protected Double compute() {
      int length = end - start;
      if (length <= THRESHOLD)
        return partialVolume();

      GetVolumeTask firstTask = new GetVolumeTask(channel, start, start + (length / 2));
      firstTask.fork();
      GetVolumeTask secondTask = new GetVolumeTask(channel, start + (length / 2), end);
      Double secondResult = secondTask.compute();
      Double firstResult = firstTask.join();

      return firstResult + secondResult;
    }

    private Double partialVolume() {
      double sum = 0.0;

      for (int i = start; i < end; i++) {
        double sampleSum = 0.0;
        double[] perceivedLoudness = loudnessToPerceivedLoudness(channel[i]);
        for (double loudness : perceivedLoudness)
          sampleSum += loudness;
        sum += sampleSum;
      }

      return sum;
    }
  }

  private static double loudnessToDb(double loudness) {
    return dbOfMax + (20 * Math.log10(loudness / Short.MAX_VALUE));
  }

  private static double phonsToLoudness(double phons) {
    return Math.pow(2, phons / 10);
  }

  // Convert loudness to perceived loudness.
  private static double[] loudnessToPerceivedLoudness(float[] loudness) {
    double[] result = new double[loudness.length];

    for (int i = 0; i < result.length; i++) {
      if (loudness[i] == 0.0) {
        result[i] = 0.0;
      } else {
        double db = loudnessToDb(loudness[i]);
        double frequency = Transform.frequencyAtBin(i);
        double phons = EqualLoudness.dbToPhons(db, frequency);
        result[i] = phonsToLoudness(phons);
      }
    }

    return result;
  }

  private static float[][] loudnessToPerceivedLoudness(float[][] transform) {
    float[][] result = new float[transform.length][transform[0].length];

    for (int i = 0; i < result.length; i++) {
      for (int j = 0; j < result[0].length; j++) {
        if (transform[i][j] == 0.0) {
          result[i][j] = 0.0f;
        } else {
          double db = loudnessToDb(transform[i][j]);
          double frequency = Transform.frequencyAtBin(j);
          double phons = EqualLoudness.dbToPhons(db, frequency);
          result[i][j] = (float)phonsToLoudness(phons);
        }
      }
    }

    return result;
  }

  private static class MultiplyArrayTask extends RecursiveAction {
    private final float[][] array;
    private final int start, end;
    private final float multiplier;
    private static final int THRESHOLD = 1;


    public MultiplyArrayTask(float[][] array, int start, int end, float multiplier) {
      this.array = array;
      this.start = start;
      this.end = end;
      this.multiplier = multiplier;
    }

    @Override
    protected void compute() {
      int length = end - start;
      if (length <= THRESHOLD) {
        partialMultiply();
        return;
      }

      MultiplyArrayTask task1 = new MultiplyArrayTask(array, start, start + (length / 2), multiplier);
      task1.fork();
      MultiplyArrayTask task2 = new MultiplyArrayTask(array, start + (length / 2), end, multiplier);
      task2.compute();
      task1.join();
    }

    private void partialMultiply() {
      for (int i = start; i < end; i++)
        for (int j = 0; j < array[0].length; j++)
          array[i][j] *= multiplier;
    }
  }

  private static void multiply2DArray(float[][] array, float multiplier) {
    MultiplyArrayTask task = new MultiplyArrayTask(array, 0, array.length, multiplier);
    try (ForkJoinPool fjp = new ForkJoinPool()) {
      fjp.invoke(task);
    }
  }
  //endregion

  // Test the effects of normalization on a perfectly flat frequency response.
  // Changing testVolume should have no effect. Changing targetVolume should.
  public static void main(String[] args) {
    float testVolume = Short.MAX_VALUE;
    float[][] volume = new float[10000][Transform.FREQUENCY_RESOLUTION];
    for (float[] floats : volume)
      Arrays.fill(floats, testVolume);

    long startTime = System.nanoTime();
    float[][] loudness = normalizeTransform(volume);
    System.out.println("Calculation time: " + ((System.nanoTime() - startTime) / 1000000000.0) + " seconds");

    PrintHelper.printFrequencies();
    PrintHelper.printValues("Loudness", loudness[5000]);
  }
}