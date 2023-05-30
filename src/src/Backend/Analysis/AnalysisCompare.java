package Backend.Analysis;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

/**
 * Calls CompareTo on a list of sound analyses and returns a sorted list of song pairs sorted by match value.
 */
public class AnalysisCompare {

  public static class CompareResult {
    public final SoundAnalysis a, b;
    public final double result;

    public CompareResult(SoundAnalysis a, SoundAnalysis b) {
      this.a = a;
      this.b = b;
      this.result = a.compareTo(b);
    }
  }

  public static List<CompareResult> compareAllAnalyses(List<? extends SoundAnalysis> analyses) {
    // gather results
    List<CompareResult> result;
    AllAnalysesTask task = new AllAnalysesTask(analyses, 0, analyses.size());
    try (ForkJoinPool fjp = new ForkJoinPool()) {
      result = fjp.invoke(task);
    }

    // sort results
    result.sort(Comparator.comparingDouble(o -> o.result));
    Collections.reverse(result);

    return result;
  }

  private static class AllAnalysesTask extends RecursiveTask<List<CompareResult>> {
    private final List<? extends SoundAnalysis> analyses;
    private final int start, end;
    private static final int THRESHOLD = 10;

    public AllAnalysesTask(List<? extends SoundAnalysis> analyses, int start, int end) {
      this.analyses = analyses;
      this.start = start;
      this.end = end;
    }

    @Override
    protected List<CompareResult> compute() {
      int length = end - start;
      if (length <= THRESHOLD)
        return partialCompare();

      AllAnalysesTask task1 = new AllAnalysesTask(analyses, start, start + (length / 2));
      task1.fork();
      AllAnalysesTask task2 = new AllAnalysesTask(analyses, start + (length / 2), end);
      List<CompareResult> result2 = task2.compute();
      List<CompareResult> result1 = task1.compute();

      result1.addAll(result2);
      return result1;
    }

    private List<CompareResult> partialCompare() {
      List<CompareResult> result = new ArrayList<>();
      for (int i = start; i < end; i++)
        for (int j = i + 1; j < analyses.size(); j++)
          result.add(new CompareResult(analyses.get(i), analyses.get(j)));
      return result;
    }
  }

  public static List<CompareResult> compareTheseToThoseAnalyses(List<? extends SoundAnalysis> userAnalyses, List<? extends SoundAnalysis> compareTo) {
    // gather results
    List<CompareResult> result;
    TheseToThoseTask task = new TheseToThoseTask(userAnalyses, compareTo, 0, compareTo.size());
    try (ForkJoinPool fjp = new ForkJoinPool()) {
      result = fjp.invoke(task);
    }

    // sort results
    result.sort(Comparator.comparingDouble(o -> o.result));
    Collections.reverse(result);

    return result;
  }

  // "those" is bigger list.
  private static class TheseToThoseTask extends RecursiveTask<List<CompareResult>> {
    private final List<? extends SoundAnalysis> these, those;
    private final int start, end;
    private static final int THRESHOLD = 10;

    public TheseToThoseTask(List<? extends SoundAnalysis> these, List<? extends  SoundAnalysis> those, int start, int end) {
      this.these = these;
      this.those = those;
      this.start = start;
      this.end = end;
    }

    @Override
    protected List<CompareResult> compute() {
      int length = end - start;
      if (length <= THRESHOLD)
        return partialCompare();

      TheseToThoseTask task1 = new TheseToThoseTask(these, those, start, start + (length / 2));
      task1.fork();
      TheseToThoseTask task2 = new TheseToThoseTask(these, those, start + (length / 2), end);
      List<CompareResult> result2 = task2.compute();
      List<CompareResult> result1 = task1.compute();

      result1.addAll(result2);
      return result1;
    }

    private List<CompareResult> partialCompare() {
      List<CompareResult> result = new ArrayList<>();
      for (int i = start; i < end; i++)
        for (SoundAnalysis analysis : these)
          result.add(new CompareResult(analysis, those.get(i)));
      return result;
    }
  }

  // Filter list so only the most and least similar match to each song is displayed.
  // Reduces the results size from 0.5n^2 down to 2n at most.
  public static List<CompareResult> mostAndLeastSimilar(List<CompareResult> matches) {
    Set<CompareResult> result = new HashSet<>();

    // get set of sound analyses
    Set<SoundAnalysis> set = new HashSet<>();
    for (CompareResult match : matches)
      set.add(match.a);

    // gather results
    for (SoundAnalysis song : set) {
      // get the most similar match
      for (int i = 0; i < matches.size(); i++) {
        if (matches.get(i).a.equals(song) || matches.get(i).b.equals(song)) {
          result.add(matches.get(i));
          break;
        }
      }

      // get the least similar match
      for (int i = matches.size() - 1; i >= 0; i--) {
        if (matches.get(i).a.equals(song) || matches.get(i).b.equals(song)) {
          result.add(matches.get(i));
          break;
        }
      }
    }

    // sort results
    List<CompareResult> resultList = new ArrayList<>(result);
    resultList.sort(Comparator.comparingDouble(o -> o.result));
    Collections.reverse(resultList);

    return resultList;
  }

  // Test running compareAnalyses with RandomAnalysis and print results.
  public static void main(String[] args) {
    List<SoundAnalysis> analyses = new ArrayList<>();
    for(int i = 0; i < 4; i++)
      analyses.add(new RandomAnalysis());

    List<CompareResult> results = compareAllAnalyses(analyses);

    DecimalFormat format = new DecimalFormat("0.00%");
    System.out.println("AnalysisCompare results:");
    for(CompareResult result : results)
      System.out.println(format.format(result.result)); // definitely a readable line of code
  }
}
