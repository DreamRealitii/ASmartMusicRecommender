package Backend.Algorithm;

import Backend.Algorithm.Reader.Channel;
import Backend.Helper.PrintHelper;

import java.io.*;

/**
 * @author Ethan Carnahan
 * Basic sound analysis that calculates the perceived frequency balance and dynamics of a song.
 * How to use: Pass in a Transform object and duration, and call get methods for volume/dynamics information.
 * For now, more dynamic means the rate of change in volume.
 * In the future, more dynamic will mean the bigger peaks in volume are narrower in time.
 */
public class SimpleCharacteristics {
  //region Fields and public methods
  // Average volume of each frequency bin.
  private final double[] averageLeftVolume, averageRightVolume;
  // Average rate of volume change for each frequency bin.
  private final double[] averageLeftRise, averageRightRise;
  private final double[] averageLeftFall, averageRightFall;
  // Needed to weigh rise/fall differently.
  private static final double VOLUME_CHANGE_EXPONENT = 2.0;
  private static final double VOLUME_CHANGE_WEIGHT = 0.005;

  public SimpleCharacteristics(Normalizer normalizer) {
    float[][] left = normalizer.getNormalized(Channel.LEFT);
    float[][] right = normalizer.getNormalized(Channel.RIGHT);

    System.out.println("SimpleCharacteristics: Calculating characteristics");

    double[][] leftCharacteristics = calculateChannelInfo(left);
    averageLeftVolume = leftCharacteristics[0];
    averageLeftRise = leftCharacteristics[1];
    averageLeftFall = leftCharacteristics[2];
    if (right != null) {
      double[][] rightCharacteristics = calculateChannelInfo(right);
      averageRightVolume = rightCharacteristics[0];
      averageRightRise = rightCharacteristics[1];
      averageRightFall = rightCharacteristics[2];
    } else {
      averageRightVolume = null;
      averageRightRise = null;
      averageRightFall = null;
    }
  }

  // Used for loading.
  private SimpleCharacteristics(double[] averageLeftVolume, double[] averageRightVolume,
  double[] averageLeftRise, double[] averageRightRise, double[] averageLeftFall, double[] averageRightFall) {
    this.averageLeftVolume = averageLeftVolume;
    this.averageRightVolume = averageRightVolume;
    this.averageLeftRise = averageLeftRise;
    this.averageRightRise = averageRightRise;
    this.averageLeftFall = averageLeftFall;
    this.averageRightFall = averageRightFall;
  }

  public double[] getAverageVolume(Channel channel) {
    return (channel == Channel.LEFT) ? averageLeftVolume : averageRightVolume;
  }

  public double[] getAverageRise(Channel channel) {
    return (channel == Channel.LEFT) ? averageLeftRise : averageRightRise;
  }

  public double[] getAverageFall(Channel channel) {
    return (channel == Channel.LEFT) ? averageLeftFall : averageRightFall;
  }

  public void write(String filepath) throws IOException {
    File file = new File(filepath);
    file.createNewFile();
    BufferedWriter writer = new BufferedWriter(new FileWriter(file));

    writer.write(averageRightVolume != null ? "Stereo" : "Mono");
    writer.newLine();
    writeArray(writer, averageLeftVolume);
    writeArray(writer, averageLeftRise);
    writeArray(writer, averageLeftFall);
    if (averageRightVolume != null) {
      writeArray(writer, averageRightVolume);
      writeArray(writer, averageRightRise);
      writeArray(writer, averageRightFall);
    }

    writer.flush();
    writer.close();
  }

  public static SimpleCharacteristics load(String filepath) throws IOException {
    BufferedReader reader = new BufferedReader(new FileReader(filepath));

    boolean stereo = reader.readLine().equals("Stereo");

    double[] lv = loadArray(reader);
    double[] lr = loadArray(reader);
    double[] lf = loadArray(reader);
    double[] rv, rr, rf;
    if (stereo) {
      rv = loadArray(reader);
      rr = loadArray(reader);
      rf = loadArray(reader);
    } else {
      rv = null;
      rr = null;
      rf = null;
    }

    return new SimpleCharacteristics(lv, rv, lr, rr, lf, rf);
  }
  //endregion

  //region Private methods
  private static double[][] calculateChannelInfo(float[][] channel) {
    double[][] result = new double[3][];
    result[0] = calculateVolume(channel);
    result[1] = calculateVolumeChange(channel, true);
    result[2] = calculateVolumeChange(channel, false);
    return result;
  }

  private static double[] calculateVolume(float[][] channel) {
    double[] result = new double[channel[0].length];

    // for each frequency
    for (int i = 0; i < result.length; i++) {
      result[i] = 0;

      // for each time
      for (float[] sample : channel) {
        result[i] += sample[i];
      }

      result[i] /= channel.length;
    }

    return result;
  }

  private static double[] calculateVolumeChange(float[][] channel, boolean rise) {
    double[] result = new double[channel[0].length];

    // for each frequency
    for (int j = 0; j < result.length; j++) {
      result[j] = 0;

      // for each time
      for (int i = 1; i < channel.length; i++) {
        if (rise && channel[i][j] > channel[i-1][j])
          result[j] += Math.pow((channel[i][j] - channel[i-1][j]), VOLUME_CHANGE_EXPONENT);
        if (!rise && channel[i][j] < channel[i-1][j])
          result[j] += Math.pow((channel[i-1][j] - channel[i][j]), VOLUME_CHANGE_EXPONENT);
      }

      result[j] *= VOLUME_CHANGE_WEIGHT / channel.length;
    }

    return result;
  }

  private static void writeArray(BufferedWriter writer, double[] array) throws IOException {
    for (double value : array) {
      writer.write(String.valueOf(value));
      writer.newLine();
    }
  }

  private static double[] loadArray(BufferedReader reader) throws IOException {
    double[] result = new double[Transform.FREQUENCY_RESOLUTION];
    for (int i = 0; i < Transform.FREQUENCY_RESOLUTION; i++)
      result[i] = Double.parseDouble(reader.readLine());
    return result;
  }
  //endregion

  // Prints the average volume/DR information of the audio file in args[0].
  public static void main(String[] args) {
    try {
      Reader reader = Reader.readFile(args[0]);
      Transform transform = new Transform(reader);
      Normalizer normalizer = new Normalizer(transform);
      long startTime = System.nanoTime();
      SimpleCharacteristics simpleCharacteristics = new SimpleCharacteristics(normalizer);
      System.out.println("Calculation time: " + ((System.nanoTime() - startTime) / 1000000000.0) + " seconds");

      System.out.println("Left channel characteristics:");
      double[] leftVolume = simpleCharacteristics.getAverageVolume(Channel.LEFT);
      double[] leftRise = simpleCharacteristics.getAverageRise(Channel.LEFT);
      double[] leftFall = simpleCharacteristics.getAverageFall(Channel.LEFT);

      PrintHelper.printFrequencies();
      PrintHelper.printValues("Loudness", leftVolume);
      PrintHelper.printValues("Rise", leftRise);
      PrintHelper.printValues("Fall", leftFall);

    } catch (IOException e) {
      System.out.println(e.getMessage());
    }
  }
}
