package Backend.Algorithm;

import Backend.Algorithm.Reader.Channel;
import Backend.Helper.PrintHelper;

import java.io.*;

/**
 * @author Ethan Carnahan
 * Basic sound analysis that calculates the perceived frequency balance and dynamics of a song.
 * How to use: Pass in a Transform object and duration, and call get methods for volume/dynamics information.
 */
public class SimpleCharacteristics {
  //region Fields and public methods
  // Average volume of each frequency bin.
  private final double[] leftVolume, rightVolume;
  // Average rate of volume change for each frequency bin.
  private final double[] leftRisePlusFall, rightRisePlusFall;
  private final double[] leftRiseMinusFall, rightRiseMinusFall;
  // Needed to weigh rise/fall differently.
  private static final double VOLUME_CHANGE_EXPONENT = 2.0;
  private static final double VOLUME_CHANGE_WEIGHT = 0.002;

  public SimpleCharacteristics(Normalizer normalizer) {
    float[][] left = normalizer.getNormalized(Channel.LEFT);
    float[][] right = normalizer.getNormalized(Channel.RIGHT);

    System.out.println("SimpleCharacteristics: Calculating characteristics");

    double[][] leftCharacteristics = calculateChannelInfo(left);
    leftVolume = leftCharacteristics[0];
    leftRisePlusFall = leftCharacteristics[1];
    leftRiseMinusFall = leftCharacteristics[2];
    if (right != null) {
      double[][] rightCharacteristics = calculateChannelInfo(right);
      rightVolume = rightCharacteristics[0];
      rightRisePlusFall = rightCharacteristics[1];
      rightRiseMinusFall = rightCharacteristics[2];
    } else {
      rightVolume = null;
      rightRisePlusFall = null;
      rightRiseMinusFall = null;
    }
  }

  // Used for loading.
  private SimpleCharacteristics(double[] averageLeftVolume, double[] averageRightVolume,
  double[] averageLeftRise, double[] averageRightRise, double[] averageLeftFall, double[] averageRightFall) {
    this.leftVolume = averageLeftVolume;
    this.rightVolume = averageRightVolume;
    this.leftRisePlusFall = averageLeftRise;
    this.rightRisePlusFall = averageRightRise;
    this.leftRiseMinusFall = averageLeftFall;
    this.rightRiseMinusFall = averageRightFall;
  }

  public double[] getAverageVolume(Channel channel) {
    return (channel == Channel.LEFT) ? leftVolume : rightVolume;
  }

  public double[] getAverageRisePlusFall(Channel channel) {
    return (channel == Channel.LEFT) ? leftRisePlusFall : rightRisePlusFall;
  }

  public double[] getAverageRiseMinusFall(Channel channel) {
    return (channel == Channel.LEFT) ? leftRiseMinusFall : rightRiseMinusFall;
  }

  public void write(String filepath) throws IOException {
    File file = new File(filepath + ".simp");
    file.createNewFile();
    BufferedWriter writer = new BufferedWriter(new FileWriter(file));

    writer.write(rightVolume != null ? "Stereo" : "Mono");
    writer.newLine();
    writeArray(writer, leftVolume);
    writeArray(writer, leftRisePlusFall);
    writeArray(writer, leftRiseMinusFall);
    if (rightVolume != null) {
      writeArray(writer, rightVolume);
      writeArray(writer, rightRisePlusFall);
      writeArray(writer, rightRiseMinusFall);
    }

    writer.flush();
    writer.close();
  }

  public static SimpleCharacteristics load(String filepath) throws IOException {
    if (!filepath.contains(".simp"))
      filepath = filepath + ".simp";
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
    double[] rise = calculateVolumeChange(channel, true);
    double[] fall = calculateVolumeChange(channel, false);
    result[1] = riseAndFall(rise, fall, true);
    result[2] = riseAndFall(rise, fall, false);
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

  // Calculates either (rise + fall) or (rise - fall)
  private static double[] riseAndFall(double[] rise, double[] fall, boolean sum) {
    double[] result = new double[rise.length];

    for (int i = 0; i < result.length; i++) {
      if (sum) {
        result[i] = rise[i] + fall[i];
      } else {
        if (rise[i] != fall[i])
          result[i] = rise[i] - fall[i];
      }
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
      double[] leftRise = simpleCharacteristics.getAverageRisePlusFall(Channel.LEFT);
      double[] leftFall = simpleCharacteristics.getAverageRiseMinusFall(Channel.LEFT);

      PrintHelper.printFrequencies();
      PrintHelper.printValues("Loudness", leftVolume);
      PrintHelper.printValues("Rise+Fall", leftRise);
      PrintHelper.printValues("Rise-Fall", leftFall);

    } catch (IOException e) {
      System.out.println(e.getMessage());
    }
  }
}
