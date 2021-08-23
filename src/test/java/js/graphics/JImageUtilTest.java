/**
 * MIT License
 * 
 * Copyright (c) 2021 Jeff Sember
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * 
 **/
package js.graphics;

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import org.junit.Test;

import js.data.ByteArray;
import js.data.DataUtil;
import js.data.ShortArray;
import js.file.Files;
import js.geometry.IPoint;
import js.graphics.JImageUtil;
import js.graphics.gen.CompressParam;
import js.graphics.gen.JImage;
import js.testutil.MyTestCase;
import static js.base.Tools.*;
import js.graphics.ImgUtil;

public class JImageUtilTest extends MyTestCase {

  @Test
  public void sampleImage0() {
    verify("000");
  }

  @Test
  public void sampleImage1() {
    verify("001");
  }

  @Test
  public void sampleImage2() {
    verify("002");
  }

  @Test
  public void sampleImage3() {
    verify("003");
  }

  @Test
  public void sampleImage4() {
    verify("004");
  }

  @Test
  public void rawImage() {
    // TODO: this test is failing if the 'raw_image.jmg' file is missing,
    // as it fails to build one from a .raw counterpart.
    // Disabling.
    if (false)
      verify("raw_image");
  }

  @Test
  public void lena() {
    verify("lena");
  }

  @Test
  public void bird() {
    verify("bird");
  }

  @Test
  public void boat() {
    verify("boat");
  }

  @Test
  public void smallImage() {
    verify(createSampleImage());
  }

  /**
   * Try a variety of golomb and padding factors to find an optimal set of
   * parameters. Test isn't run unless it's the only one
   */
  @Test
  public void sampleShort() {
    if (!verbose())
      return;
    // The best parameters are:
    //
    // golomb 13 
    // padding 0.31
    // ratio 43.558594
    //
    sampleImages(1);
  }

  /**
   * Try a variety of golomb and padding factors to find an optimal set of
   * parameters, for an image with a higher bit depth. Test isn't run unless
   * it's the only one
   * 
   * NO LONGER SUPPORTED (for now)
   */
  @Test
  public void sampleExpanded() {
    if (!verbose())
      return;
    // The best parameters are:
    //
    // golomb 13312 
    // padding 0.36
    // ratio 53.030518
    //
    sampleImages(1024);
  }

  private void sampleImages(int scale) {
    if (scale != 1)
      throw notSupported("scale factor no longer supported");
    List<JImage> images = arrayList();
    String[] names = { "000.jmg", "001.jmg", "002.jmg", "003.jmg", "005.raw" };
    for (String name : names) {
      JImage b = readSampleImage(name);
      images.add(b.build());
    }

    int counter = 0;
    for (int golomb = 8; golomb < 20; golomb++) {
      mParam.golomb(golomb * scale);
      for (int pf = 20; pf < 50; pf++) {
        mParam.padding(pf / 100f);
        long origLength = 0;
        long compLength = 0;

        for (JImage image : images) {
          int[] compressed = JImageUtil.encode(image, mParam);

          counter++;
          if (counter % 13 == 0) {
            JImage decomp = JImageUtil.decode(compressed);
            if (!image.equals(decomp))
              throw die("did not decompress");
          }
          // If we're not actually scaling the short pixels, assume uncompressed size was a short
          int pixelSizeBytes = Short.BYTES;
          origLength += image.wPixels().length * pixelSizeBytes;
          compLength += compressed.length * Integer.BYTES;
        }
        mParam.ratio((compLength * 100f) / origLength);

        boolean best = (mBestParam == null) || mParam.ratio() < mBestParam.ratio();
        if (best)
          mBestParam = mParam.build();
        if (verbose())
          log(best ? "*" : "_", "g:", golomb, "p:", (int) (mParam.padding() * 100), "ratio:", mParam.ratio());
      }
    }
    log("*** Best:", INDENT, mBestParam);
    assertMessage(mBestParam);
  }

  /**
   * Convert a 16-bit depth image to an 8-bit one, and recompress
   */
  @Test
  public void smallPixels() {
    JImage sourcePix = readSampleImage("003.jmg");
    JImage shrunken = shrinkShortPixelsByte(sourcePix);
    verify(shrunken);
  }

  @Test
  public void convertToFrom16BitBufferedImage() {
    JImage source = readSampleImage("003.jmg");
    BufferedImage img = JImageUtil.toBufferedImage(source);
    if (verbose())
      ImgUtil.writeImage(Files.S, img, generatedFile("image.png"));
    JImage result = JImageUtil.from(img);
    assertEquals(source, result);
  }

  @Test
  public void convertToFrom8BitBufferedImage() {
    JImage source = readSampleImage("003.jmg");
    source = shrinkShortPixelsByte(source);

    BufferedImage img = JImageUtil.toBufferedImage(source);
    if (verbose())
      ImgUtil.writeImage(Files.S, img, generatedFile("image.png"));

    JImage result = JImageUtil.from(img);
    assertEquals(source, result);
  }

  private void verify(String imageName) {
    JImage image = readSampleImage(imageName);
    verify(image);
  }

  private void verify(JImage image) {
    int[] compressed = JImageUtil.encode(image, mParam);

    // Decompress the image, and verify we get the original pixels back

    JImage decoded = JImageUtil.decode(compressed);

    checkArgument(image.depth() == 1);
    int decompSize;
    if (image.wPixels() != null)
      decompSize = Short.BYTES * image.size().product();
    else
      decompSize = Byte.BYTES * image.size().product();

    // int bytesPerPixel = image.wPixels() != null ? Short.BYTES : Byte.BYTES;

    if (verbose() && image.wPixels() != null && image.size().product() < 100) {
      log("original pixels:", INDENT, image.wPixels());
      log("comp/decomp pixels:", INDENT, decoded.wPixels());
    }
    assertEquals(image, decoded);
    mParam.ratio((compressed.length * Integer.BYTES * 100f) / decompSize);
    log("param:", INDENT, mParam);

    if (mBestParam == null || mBestParam.ratio() > mParam.ratio())
      mBestParam = mParam.build();
  }

  /**
   * Read sample image. If no image with .jmg extension exists, attempts to
   * create one from either a .raw or a .png file
   * 
   */
  private JImage readSampleImage(String filename) {
    File imageBaseFile = testFile(filename);

    JImage output;

    File imageFile = Files.setExtension(imageBaseFile, ImgUtil.JMG_EXT);
    if (imageFile.exists()) {
      output = JImageUtil.decode(Files.toByteArray(imageFile));
    } else {
      log("read sample image; not found:", imageBaseFile.getName());

      // If a .raw file exists, use that as the source image
      //
      File imageSource = Files.setExtension(imageBaseFile, "raw");
      if (imageSource.exists()) {
        log("...converting from raw file");

        final IPoint RAW_IMAGE_SIZE_DEFAULT = new IPoint(320, 256);

        JImage.Builder img = JImage.newBuilder()//
            .depth(1);
        img.size(RAW_IMAGE_SIZE_DEFAULT);
        img.wPixels(DataUtil.bytesToShortsBigEndian(Files.toByteArray(imageSource)));
        output = img.build();
      } else {
        imageSource = Files.setExtension(imageBaseFile, ImgUtil.PNG_EXT);
        log("...converting from png");
        BufferedImage srcImg = ImgUtil.read(imageSource);
        output = JImageUtil.from(srcImg);
      }
      int[] compressed = JImageUtil.encode(output, mParam);
      log("...writing results to", imageFile.getName());
      Files.S.write(DataUtil.intsToBytesBigEndian(compressed), imageFile);
    }
    return output;
  }

  private JImage createSampleImage() {

    JImage.Builder img = JImage.newBuilder();
    img.depth(1);

    float scale = 8;
    float mid = 500;
    int w = 12;
    int h = 12;

    img.size(new IPoint(w, h));
    ShortArray.Builder pix = ShortArray.newBuilder();
    for (int y = 0; y < h; y++) {
      for (int x = 0; x < w; x++) {
        float dx = (w / 2f) - x;
        float dy = (h / 2f) - y;
        float v = (dx * dx + dy * dy) * scale + mid;
        pix.add((short) v);
      }
    }
    img.wPixels(pix.array());
    return img.build();
  }

  private JImage shrinkShortPixelsByte(JImage source) {
    JImage.Builder out = source.build().toBuilder();
    out.wPixels(null);

    short[] pix = source.wPixels();
    int min = pix[0];
    int max = min;
    for (short s : pix) {
      int x = ((int) s) & 0xffff;
      min = Math.min(min, x);
      max = Math.max(max, x);
    }
    int newMin = 5;
    int newMax = 120;
    float scale = (newMax - newMin + 1) / (float) (max + 1 - min);
    float offset = newMin - scale * min;
    ByteArray.Builder bytes = ByteArray.newBuilder();
    for (int i = 0; i < pix.length; i++) {
      bytes.add((byte) (pix[i] * scale + offset));
    }
    out.bPixels(bytes.array());
    return out;
  }

  private CompressParam.Builder mParam = CompressParam.newBuilder()//
      .golomb(13)//
      .padding(0.31f)//
  ;

  private CompressParam mBestParam;
}
