package Frontend;

import Backend.Algorithm.Reader.Channel;
import Backend.Algorithm.SimpleCharacteristics;
import Backend.Algorithm.Transform;
import java.awt.Font;
import javax.swing.JFrame;

import javax.swing.WindowConstants;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryMarker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.TextAnchor;

// See https://www.javatpoint.com/jfreechart-line-chart
public class LocalAnalysisResult extends JFrame {

  private static final Font labelFont = new Font(Font.SANS_SERIF, Font.ITALIC, 10);

  public LocalAnalysisResult(String filename, SimpleCharacteristics characteristics) {
    super("Analysis of " + filename);

    DefaultCategoryDataset dataset = createDataset(characteristics);

    JFreeChart chart = ChartFactory.createLineChart("Simple Analysis of " + filename,
        "Frequency Bins (20hz - 20khz)", "Perceived Loudness",
        dataset, PlotOrientation.VERTICAL, true, true, false);

    for (int i = 0; i < 20; i++) {
      String label = String.valueOf((int)(Transform.frequencyAtBin(
          i * Transform.FREQUENCY_RESOLUTION / 20)));
      CategoryMarker marker = new CategoryMarker(label);
      marker.setLabel(label + "hz");
      marker.setLabelFont(labelFont);
      marker.setDrawAsLine(true);
      marker.setLabelAnchor(RectangleAnchor.BOTTOM);
      marker.setLabelTextAnchor(TextAnchor.BASELINE_CENTER);
      chart.getCategoryPlot().addDomainMarker(marker);
    }

    ChartPanel panel = new ChartPanel(chart);
    setContentPane(panel);

    pack();
    setSize(1200, 600);
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
  }

  private static DefaultCategoryDataset createDataset(SimpleCharacteristics characteristics) {
    String series1 = "Perceived Loudness", series2 = "Rise+Fall Speed", series3 = "Rise-Fall Speed";
    DefaultCategoryDataset result = new DefaultCategoryDataset();

    double[] loudness = getLoudness(characteristics);
    for (int i = 0; i < Transform.FREQUENCY_RESOLUTION; i++)
      result.addValue(loudness[i], series1, String.valueOf((int)(Transform.frequencyAtBin(i))));

    double[] rise = getRisePlusFall(characteristics);
    for (int i = 0; i < Transform.FREQUENCY_RESOLUTION; i++)
      result.addValue(rise[i], series2, String.valueOf((int)(Transform.frequencyAtBin(i))));

    double[] fall = getRiseMinusFall(characteristics);
    for (int i = 0; i < Transform.FREQUENCY_RESOLUTION; i++)
      result.addValue(fall[i], series3, String.valueOf((int)(Transform.frequencyAtBin(i))));

    return result;
  }


  private static double[] getLoudness(SimpleCharacteristics characteristics) {
    return getCenter(characteristics.getAverageVolume(Channel.LEFT),
        characteristics.getAverageVolume(Channel.RIGHT));
  }

  private static double[] getRisePlusFall(SimpleCharacteristics characteristics) {
    return getCenter(characteristics.getAverageRisePlusFall(Channel.LEFT),
        characteristics.getAverageRisePlusFall(Channel.RIGHT));
  }

  private static double[] getRiseMinusFall(SimpleCharacteristics characteristics) {
    return getCenter(characteristics.getAverageRiseMinusFall(Channel.LEFT),
        characteristics.getAverageRiseMinusFall(Channel.RIGHT));
  }

  // For mono: Returns left
  // For stereo: Returns 0.5*left + 0.5*right.
  private static double[] getCenter(double[] left, double[] right) {
    if (right != null) {
      double[] result = new double[left.length];

      for (int i = 0; i < result.length; i++)
        result[i] = (left[i] + right[i]) / 2.0;

      return result;
    } else {
      return left;
    }
  }
}
