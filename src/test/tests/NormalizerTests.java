import static org.junit.jupiter.api.Assertions.assertEquals;

import Backend.Algorithm.*;

import Backend.Algorithm.Reader.Channel;
import org.junit.jupiter.api.*;

public class NormalizerTests {
    private static final double errorBound = 0.1;

    // Passing requirement: Two flat frequency responses of different volumes are normalized to the same perceived loudness.
    @Test
    public void testVolume() {
        float[][] quiet = generateNormalizedFlatTransform(10, 0.1f);
        float[][] loud = generateNormalizedFlatTransform(10, 10000f);
        assert2DArrayEquals(quiet, loud);
    }

    // Passing requirement: Two flat frequency responses of different lengths are normalized to the same perceived loudness.
    @Test
    public void testDuration() {
        float[][] brief = generateNormalizedFlatTransform(10, 100f);
        float[][] lengthy = generateNormalizedFlatTransform(100, 100f);
        assert2DArrayEquals(brief, lengthy);
    }

    private float[][] generateNormalizedFlatTransform(int length, float volume) {
        float[][] result = new float[length][Transform.FREQUENCY_RESOLUTION];
        for (int i = 0; i < result.length; i++)
            for (int j = 0; j < result[0].length; j++)
                result[i][j] = volume;
        return new Normalizer(result, null).getNormalized(Channel.LEFT);
    }

    private void assert2DArrayEquals(float[][] a, float[][] b) {
        for (int i = 0; i < a.length && i < b.length; i++)
            for (int j = 0; j < a[i].length && j < b[i].length; j++)
                assertEquals(a[i][j], b[i][j], errorBound);
    }
}
