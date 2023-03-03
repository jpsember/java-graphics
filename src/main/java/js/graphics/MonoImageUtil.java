package js.graphics;

import static js.base.Tools.*;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import js.base.Pair;
import js.data.DataUtil;
import js.data.IntArray;
import js.geometry.IPoint;
import js.geometry.IRect;
import js.graphics.gen.ImageStats;
import js.graphics.gen.MonoImage;
import js.json.JSList;
import js.json.JSMap;

public final class MonoImageUtil {

  // This is now the max pixel value for signed 16-bit pixels
  public static final int MAX_PIXEL_VALUE = 0x8000;

  /**
   * Construct MonoImage with pixels initialized to zero
   */
  public static MonoImage construct(IPoint imageSize) {
    return construct(imageSize, null);
  }

  /**
   * Construct MonoImage with its own copy of some pixels
   */
  public static MonoImage construct(IPoint imageSize, short[] pixels) {
    pixels = DataUtil.shortArray(imageSize.product(), pixels);
    MonoImage.Builder b = MonoImage.newBuilder();
    b.size(imageSize);
    b.pixels(DataUtil.copyOf(pixels));
    return verifyNonEmpty(b.build());
  }

  /**
   * Construct a deep copy of another MonoImage (with its own copy of the
   * pixels)
   */
  public static MonoImage construct(MonoImage source) {
    return source.toBuilder().pixels(DataUtil.copyOf(source.pixels())).build();
  }

  /**
   * Construct MonoImage from BufferedImage (with its own copy of the pixels)
   */
  public static MonoImage construct(BufferedImage bufferedImage) {
    return construct(ImgUtil.size(bufferedImage), ImgUtil.grayPixels(bufferedImage));
  }

  private static int unsignedShortToInt(int pixel) {
    return pixel & 0xffff;
  }

  /**
   * Construct an array containing only the non-zero pixels (useful for stats,
   * but not any spacial information of course)
   */
  private static short[] filterOmittedPixels(MonoImage img) {
    short[] filteredPixels = new short[img.pixels().length];
    int filteredPixelCount = 0;
    for (short pixel : img.pixels()) {
      if (pixel == 0)
        continue;
      filteredPixels[filteredPixelCount++] = pixel;
    }
    return Arrays.copyOf(filteredPixels, filteredPixelCount);
  }

  public static ImageStats generateStats(MonoImage img) {
    ImageStats.Builder b = ImageStats.newBuilder();
    auxGenerateStats(img, b);
    return b.build();
  }

  /**
   * Determine only the min and max pixel values
   */
  public static ImageStats generateRangeStatsOnly(MonoImage monoImage) {
    verifyNonEmpty(monoImage);
    int pixelCount = 0;
    ImageStats.Builder imageStats = ImageStats.newBuilder();
    int min = 0x10000;
    int max = -1;
    for (short shortPixel : monoImage.pixels()) {
      if (shortPixel == 0)
        continue;
      int intPixel = unsignedShortToInt(shortPixel);
      if (intPixel < min)
        min = intPixel;
      if (intPixel > max)
        max = intPixel;
      pixelCount++;
    }
    imageStats.min(min);
    imageStats.max(max);
    imageStats.count(pixelCount);
    imageStats.range(max - min);
    return imageStats.build();
  }

  private static void auxGenerateStats(MonoImage img, ImageStats.Builder b) {
    verifyNonEmpty(img);

    // Get a copy of the array with all zero pixels removed (so they don't affect the stats)
    //
    short[] pixels = filterOmittedPixels(img);
    b.count(pixels.length);
    if (b.count() == 0) {
      b.problem("no unfiltered pixels");
      return;
    }

    long sum = 0;
    int min = unsignedShortToInt(pixels[0]);
    int max = min;

    for (int pix : pixels) {
      pix = unsignedShortToInt(pix);
      if (pix < min)
        min = pix;
      if (pix > max)
        max = pix;
      sum += pix;
    }

    b.max(max);
    b.min(min);
    b.mean(Math.round(sum / (float) b.count()));
    int range = range(b);
    if (range == 1) {
      b.problem("unreasonable range");
      return;
    }

    // Frequency count of pixel with value (n - min)
    int[] hist = new int[range];

    int minPixelIndex = -1;
    int maxPixelIndex = -1;
    {
      int i = INIT_INDEX;
      for (int pix : img.pixels()) {
        pix = unsignedShortToInt(pix);
        i++;
        if (pix == 0)
          continue;
        hist[pix - min]++;
        if (pix == min)
          minPixelIndex = i;
        if (pix == max)
          maxPixelIndex = i;
      }
    }
    b.histogram(hist);
    b.minLoc(locationOfPixelWithIndex(img, minPixelIndex));
    b.maxLoc(locationOfPixelWithIndex(img, maxPixelIndex));

    {
      short[] cdf = new short[100];
      int percent = 0;
      int pixelCount = pixels.length;

      int pixelsProcessed = 0;

      for (int pixelValue = min; pixelValue <= max; pixelValue++) {
        int pixelFrequency = hist[pixelValue - min];
        if (pixelFrequency == 0)
          continue;
        cdf[percent] = (short) pixelValue;

        pixelsProcessed += pixelFrequency;

        while (pixelsProcessed >= ((1 + percent) * pixelCount) / 100.0f) {
          percent++;
          if (percent < 100)
            cdf[percent] = (short) pixelValue;
        }
      }

      while (percent < 100) {
        cdf[percent] = (short) max;
        percent++;
      }

      b.cdf(cdf);

      b.range(b.max() + 1 - b.min());
      b.median(readCDFValue(b, 50));

      final int CLIP_PCT = 2;
      int minClipped = readCDFValue(b, CLIP_PCT);
      int maxClipped = readCDFValue(b, 100 - 1 - CLIP_PCT);
      b.clippedRange(maxClipped + 1 - minClipped);
    }
  }

  /**
   * Utility method to read a cdf value form the stats and return as an integer.
   * I was getting a lot of errors because of not always correctly converting
   * the cdf values (which are stored as shorts) to integers
   */
  public static int readCDFValue(ImageStats stats, int index) {
    return unsignedShortToInt(stats.cdf()[index]);
  }

  /**
   * Construct a plot of the histogram, and return a modified ImageStats
   */
  public static ImageStats renderHistogram(ImageStats stats) {
    if (!stats.histogramPlot().isEmpty())
      return stats;
    ImageStats.Builder b = stats.toBuilder();
    b.histogramPlot(plotHistogram(stats));
    return b.build();
  }

  /**
   * Construct a plot of the cdf, and return a modified ImageStats
   */
  public static ImageStats renderCDF(ImageStats stats) {
    if (!stats.cdfPlot().isEmpty())
      return stats;
    ImageStats.Builder b = stats.toBuilder();
    b.cdfPlot(plotCDF(stats));
    return b.build();
  }

  public static ImageStats constructSmallStats(ImageStats stats) {
    ImageStats.Builder b = stats.toBuilder();
    b.histogram(null);
    b.cdf(null);
    b.histogramPlot(null);
    return b.build();
  }

  private static IPoint locationOfPixelWithIndex(MonoImage img, int index) {
    int y = index / img.size().x;
    int x = index % img.size().x;
    return new IPoint(x + img.offset().x, y + img.offset().y);
  }

  private static Map<String, String> plotHistogram(ImageStats stats) {
    return plotHistogram(stats, 32);
  }

  public static int range(ImageStats stats) {
    return stats.max() + 1 - stats.min();
  }

  private static Map<String, String> plotHistogram(ImageStats stats, int numberOfRows) {
    Map<String, String> m = treeMap();
    if (!stats.problem().isEmpty()) {
      m.put("problem:", stats.problem());
      return m;
    }

    String bar = "**********************************************************************";
    List<Pair<Integer, Float>> entries = arrayList();

    int valuesPerRow = Math.max(1, (int) Math.ceil(range(stats) / numberOfRows));
    int vi = stats.min();
    int[] hist = stats.histogram();
    float maxVal = 0;
    while (vi <= stats.max()) {
      int freq = 0;
      for (int j = 0; j < valuesPerRow; j++) {
        int k = vi + j - stats.min();
        if (k < hist.length)
          freq += hist[k];
      }
      int val = Math.round(vi + valuesPerRow * .5f);
      float avgFreq = freq / (float) valuesPerRow;
      entries.add(new Pair<>(val, avgFreq));
      if (maxVal < avgFreq)
        maxVal = avgFreq;
      vi += valuesPerRow;
    }
    for (Pair<Integer, Float> entry : entries) {
      String key = String.format("%5d", entry.first);
      int val = Math.round(entry.second * (bar.length() / maxVal));
      m.put(key, bar.substring(0, val));
    }
    return m;
  }

  private static Map<String, String> plotCDF(ImageStats stats) {
    Map<String, String> m = treeMap();
    if (!stats.problem().isEmpty()) {
      m.put("problem:", stats.problem());
      return m;
    }

    String bar = "**********************************************************************";

    short[] cdf = stats.cdf();
    checkArgument(cdf.length == 100);
    final int maxRowWidth = bar.length();
    float scale = maxRowWidth / (float) (stats.max() - stats.min());

    for (int pct = 0; pct < 100; pct++) {
      int pixelValue = unsignedShortToInt(cdf[pct]);
      String key = String.format("%3d %5d", pct, pixelValue);
      int val = Math.round((pixelValue - stats.min()) * scale);
      m.put(key, bar.substring(0, val));
    }
    m.put(String.format("    %5d", stats.min()), "");
    m.put(String.format("%3d %5d", 100, stats.max()), "");
    return m;
  }

  public static JSMap toJson(MonoImage g) {
    JSMap m = g.toJson();
    m.remove("pixels");
    short[] pix = g.pixels();
    int i = 0;
    int dw = Math.min(8, g.size().x);
    int dh = Math.min(8, g.size().y);
    int dx = (g.size().x - dw) / 2;
    int dy = (g.size().y - dh) / 2;
    for (int y = dy; y < dy + dh; y++) {
      JSList lst = list();
      for (int x = dx; x < dx + dw; x++) {
        lst.add(pix[i + x]);
      }
      i += g.size().x;
      m.put(String.format("z%03d", y), lst);
    }
    return m;
  }

  public static ImageStats constructSmallStats(BufferedImage bi) {
    MonoImage mi = construct(bi);
    ImageStats stats = generateStats(mi);
    stats = constructSmallStats(stats);
    return stats;
  }

  public static MonoImage constructClipped(MonoImage srcImage, IRect bounds) {
    IRect source = new IRect(srcImage.size());
    checkArgument(source.contains(bounds), "source image doesn't contain desired clip region");
    MonoImage destImage = MonoImageUtil.construct(bounds.size());
    copyPortion(srcImage, destImage, bounds, IPoint.ZERO);
    return destImage;
  }

  public static void copyPortion(MonoImage srcImage, MonoImage destImage, IRect sourceRect, IPoint destLoc) {
    int sourceIndex = sourceRect.y * srcImage.size().x + sourceRect.x;
    int destIndex = destLoc.y * destImage.size().x + destLoc.x;
    for (int y = 0; y < sourceRect.height; y++) {
      System.arraycopy(srcImage.pixels(), sourceIndex, destImage.pixels(), destIndex, sourceRect.width);
      sourceIndex += srcImage.size().x;
      destIndex += destImage.size().x;
    }
  }

  public static MonoImage verifyNonEmpty(MonoImage image) {
    if (image == null || image.size().isZero())
      throw badArg("MonoImage is null or empty:", INDENT, image);
    return image;
  }

  public static int nonMaskedPixelCount(MonoImage image) {
    int count = 0;
    for (short x : image.pixels())
      if (x != 0)
        count++;
    return count;
  }

  /**
   * Construct grayscale BufferedImage from MonoImage, scaling the 15-bit pixels
   * to 16 bits
   */
  public static BufferedImage to16BitBufferedImage(MonoImage monoImage) {
    BufferedImage bufferedImage = ImgUtil.build16BitGrayscaleImage(monoImage.size());
    short[] srcPixels = monoImage.pixels();
    short[] destPixels = ImgUtil.grayPixels(bufferedImage);
    for (int i = 0; i < destPixels.length; i++) {
      short sourcePixel = clampTo15BitRange(srcPixels[i]);
      destPixels[i] = (short) (sourcePixel << 1);
    }
    return bufferedImage;
  }

  /**
   * Construct grayscale BufferedImage from MonoImage, copying the 15-bit pixels
   * without change
   */
  public static BufferedImage to15BitBufferedImage(MonoImage monoImage) {
    BufferedImage bufferedImage = ImgUtil.build16BitGrayscaleImage(monoImage.size());
    short[] destPixels = ImgUtil.grayPixels(bufferedImage);
    System.arraycopy(monoImage.pixels(), 0, destPixels, 0, monoImage.pixels().length);
    return bufferedImage;
  }

  /**
   * Convert a BufferedImage that has a 15-bit range (i.e. derived from a
   * MonoImage) to one that has a 16-bit range
   */
  public static BufferedImage convert15to16Bit(BufferedImage bufferedImage) {
    MonoImage monoImage = construct(bufferedImage);
    return to16BitBufferedImage(monoImage);
  }

  /**
   * Convert an 8-bit monochromatic BufferedImage to a 15-bit MonoImage
   */
  public static MonoImage convert8BitBufferedImageMonoImage(BufferedImage gray8Image) {
    ImgUtil.assertImageType(gray8Image, BufferedImage.TYPE_BYTE_GRAY);
    byte[] grayPixels = ImgUtil.gray8Pixels(gray8Image);
    short[] gray15Pixels = new short[grayPixels.length];
    int i = INIT_INDEX;
    for (byte srcPixel : grayPixels) {
      i++;
      int sourceInt = ((int) srcPixel) & 0xff;
      gray15Pixels[i] = (short) (sourceInt << (15 - 8));
    }
    return construct(ImgUtil.size(gray8Image), gray15Pixels);
  }

  private static short clampTo15BitRange(short monoPixel) {
    // clamp any extremely hot pixels to the maximum 15-bit range
    if (monoPixel < 0)
      monoPixel = MAX_PIXEL_VALUE - 1;
    return monoPixel;
  }

  /**
   * Construct 8-bit RGB BufferedImage from MonoImage
   */
  public static BufferedImage to8BitRGBBufferedImage(MonoImage rawImage) {
    BufferedImage bufferedImage = ImgUtil.buildRGBImage(rawImage.size());
    int[] destPixels = ((DataBufferInt) bufferedImage.getRaster().getDataBuffer()).getData();
    short[] sourcePixels = rawImage.pixels();

    for (int i = 0; i < sourcePixels.length; i++) {
      short source = clampTo15BitRange(sourcePixels[i]);
      // We are converting a 15-bit pixel to an 8-bit one
      int gray = source >> 7;
      destPixels[i] = gray | (gray << 8) | (gray << 16);
    }
    return bufferedImage;
  }

  public static float[] calculateNormalizationTranslateAndScale(int minOrigPixelValue,
      int maxOrigPixelValue) {
    float translate = 0;
    float scale = 0;
    if (minOrigPixelValue < maxOrigPixelValue) {
      scale = ((float) MonoImageUtil.MAX_PIXEL_VALUE) / (maxOrigPixelValue - minOrigPixelValue);
      translate = -minOrigPixelValue;
    }
    float[] out = new float[2];
    out[0] = translate;
    out[1] = scale;
    return out;
  }

  /**
   * Apply linear normalization to image, translating pixels then scaling, and
   * clamping to a particular depth
   */
  public static MonoImage normalizeToDepth(MonoImage image, float translate, float scale, int depth,
      boolean omitZeroPixels) {
    short[] inPix = image.pixels();
    short maxPixelValue = (short) ((1 << depth) - 1);
    short[] outPix = new short[inPix.length];
    int j = 0;
    for (short inPixel : inPix) {
      short outPixVal = inPixel;
      if (!omitZeroPixels || inPixel != 0) {
        int p = (int) ((inPixel + translate) * scale);
        if (p < 0)
          p = 0;
        else if (p > maxPixelValue)
          p = maxPixelValue;
        outPixVal = (short) p;
      }
      outPix[j++] = outPixVal;
    }
    return image.build().toBuilder().pixels(outPix).build();
  }

  /**
   * Construct version of raw image, normalized using ImageMagick algorithm,
   * which (I think) is a linear mapping of the trimmed histogram to 0..65535,
   * where the darkest 2% and lightest %1 of pixels are trimmed.
   * <p>
   * See http://www.imagemagick.org/Usage/color_mods/#normalize
   */
  public static MonoImage normalizedImageMagick(MonoImage monoImage, ImageStats statsOrNull) {
    ImageStats stats = statsOrNull;
    if (stats == null)
      stats = MonoImageUtil.generateStats(monoImage);
    if (stats.cdf().length == 0)
      throw badArg("CDF is empty");

    int lowCutoffValue = stats.cdf()[1];
    int highCutoffValue = stats.cdf()[98];

    float scale = ((float) MAX_PIXEL_VALUE) / (highCutoffValue - lowCutoffValue);
    float translate = -lowCutoffValue;

    return MonoImageUtil.normalizeToDepth(monoImage, translate, scale, 15, false);
  }

  /**
   * Construct version of image with equalized histogram
   */
  public static MonoImage equalizeHistogram(MonoImage image) {
    ImageStats stats = generateStats(image);
    int[] histogram = stats.histogram();
    if (histogram.length == 0)
      return image;
    int[] cdf = new int[MAX_PIXEL_VALUE];
    int sum = 0;
    {
      int i = stats.min();
      while (i <= stats.max()) {
        cdf[i] = sum;
        sum += histogram[i - stats.min()];
        i++;
      }
      while (i < MAX_PIXEL_VALUE)
        cdf[i++] = sum;
    }
    float scaleFactor = (MAX_PIXEL_VALUE - 1) / (float) sum;

    short[] map = new short[MAX_PIXEL_VALUE];
    for (int i = 0; i < MAX_PIXEL_VALUE; i++) {
      int scaled = Math.round(cdf[i] * scaleFactor);
      if (scaled >= MAX_PIXEL_VALUE)
        throw badState("scaled value is:", scaled, "for i", i, "with max", stats.max());
      map[i] = (short) scaled;
    }
    short[] outPix = new short[image.pixels().length];
    int j = 0;
    for (short inPixel : image.pixels())
      outPix[j++] = map[inPixel];
    return construct(image.size(), outPix);
  }

  public static MonoImage sharpen(MonoImage image) {
    BufferedImage bi = to15BitBufferedImage(image);
    bi = ImgEffects.sharpen(bi);
    MonoImage mi = construct(bi);
    // The sharpening has some overflow effects that I think is treating 'less than zero' pixels as white.
    // If any pixels are negative, set them to zero.  Seems to work.
    int i = INIT_INDEX;
    short[] px = mi.pixels();
    for (short p : px) {
      i++;
      if (p < 0)
        px[i] = 0;
    }
    return mi;
  }

  /**
   * Add padding to an image (or return the original if no padding required)
   */
  public static MonoImage addPadding(MonoImage image, int px, int py) {
    if (px == 0 && py == 0)
      return image;
    int nw = image.size().x + px;
    int nh = image.size().y + py;
    short[] np = new short[nw * nh];
    short[] pix = image.pixels();
    int si = 0;
    int di = 0;
    for (int y = 0; y < image.size().y; y++) {
      for (int x = 0; x < image.size().x; x++) {
        np[di + x] = pix[si + x];
      }
      si += image.size().x;
      di += nw;
    }
    return MonoImage.newBuilder().size(new IPoint(nw, nh)).pixels(np).build();
  }

  /**
   * Construct a MonoImage filled with a single value
   */
  public static MonoImage constantImage(IPoint size, int value) {
    MonoImage image = MonoImage.newBuilder() //
        .pixels(new short[size.product()])//
        .size(size)//
        .build();
    Arrays.fill(image.pixels(), (short) value);
    return image;
  }

  /**
   * Perform an erosion of a monochrome image. For each pixel that is zero, its
   * north/south/east/west neighbors are set to zero as well. Repeated calls
   * will erode the image further.
   * 
   * Pixels on the image boundary will not be affected
   * 
   * @param frontierStack
   *          array containing pixels eroded in previous pass, or null if this
   *          is the first pass
   * @param pixels
   *          monochrome pixels
   * @param size
   *          dimensions of image
   * @return frontierStack for next pass
   */
  public static IntArray.Builder erodeMonochromeImage(IntArray.Builder frontierStack, short[] pixels,
      IPoint size) {

    final short ERODE_VALUE = 0;

    checkArgument(pixels.length == size.product(), "unexpected pixel count");
    final int sizeX = size.x;
    final int sizeY = size.y;

    if (frontierStack == null) {
      // Construct a stack of transparent pixels adjacent to non-transparent ones

      frontierStack = IntArray.newBuilder();
      int i = 0;
      for (int y = 0; y < size.y; y++) {
        for (int x = 0; x < size.x; x++, i++) {
          if (pixels[i] != ERODE_VALUE)
            continue;
          if ((x > 0 && pixels[i - 1] != ERODE_VALUE) //
              || (x < sizeX - 1 && pixels[i + 1] != ERODE_VALUE) //
              || (y > 0 && pixels[i - sizeX] != ERODE_VALUE) //
              || (y < sizeY - 1 && pixels[i + sizeX] != ERODE_VALUE) //
          ) {
            frontierStack.add(i);
          }
        }
      }
      //
      //      int i0 = (1) + (1 * size.x);
      //      int trimmedWidth = rowSize - 2;
      //      int trimmedHeight = size.y - 2;
      //      for (int rowCount = trimmedHeight; rowCount > 0; rowCount--, i0 += rowSize) {
      //        int i = i0;
      //        for (int colCount = trimmedWidth; colCount > 0; colCount--, i++) {
      //          if (pixels[i] != ERODE_VALUE)
      //            continue;
      //          if (pixels[i - 1] != ERODE_VALUE || pixels[i + 1] != ERODE_VALUE
      //              || pixels[i - rowSize] != ERODE_VALUE || pixels[i + rowSize] != ERODE_VALUE) {
      //            frontierStack.add(i);
      //          }
      //        }
      //      }
    }

    //    IntArray.Builder auxStack = IntArray.newBuilder();
    //
    //    // Filter out any pixels lying on the image boundary
    //    //
    //    for (int i : frontierStack.array()) {
    //      int y = i / rowSize;
    //      if (y == 0 || y == size.y - 1)
    //        continue;
    //      int x = i % rowSize;
    //      if (x == 0 || x == rowSize - 1)
    //        continue;
    //      auxStack.add(i);
    //    }
    //    int[] frontierPix = auxStack.array();

    int[] frontierPix = frontierStack.array();

    frontierStack = IntArray.newBuilder();
    for (int i : frontierPix)
      pixels[i] = ERODE_VALUE;

    for (int i : frontierPix) {
      int x = i % sizeX;
      int y = i / sizeX;
      if (x > 0) {
        int m = i - 1;
        if (pixels[m] != ERODE_VALUE)
          frontierStack.add(m);
      }
      if (x < sizeX - 1) {
        int m = i + 1;
        if (pixels[m] != ERODE_VALUE)
          frontierStack.add(m);
      }
      if (y > 0) {
        int m = i - sizeX;
        if (pixels[m] != ERODE_VALUE)
          frontierStack.add(m);
      }
      if (y < sizeY - 1) {
        int m = i + sizeX;
        if (pixels[m] != ERODE_VALUE)
          frontierStack.add(m);
      }
    }

    return frontierStack;
  }

}
