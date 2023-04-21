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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;

import com.pngencoder.PngEncoder;

import js.base.BasePrinter;
import js.data.DataUtil;
import js.file.Files;
import js.geometry.IPoint;
import js.graphics.gen.ImageStats;
import js.graphics.gen.JImage;
import js.graphics.gen.MonoImage;
import js.json.JSMap;
import js.testutil.MyTestCase;

import org.junit.Test;

import static js.base.Tools.*;

public class ImgUtilTest extends MyTestCase {

  @Test
  public void readPNG() {
    assertMessage(ImgUtil.toJson(readImage("batman.png")));
  }

  @Test
  public void mono16bit() {
    BufferedImage img = readImage("mono16bit.png");
    assertMessage(ImgUtil.toJson(img));
  }

  @Test
  public void imageAsType() {
    BufferedImage img = readImage("batman.png");
    img = ImgUtil.imageAsType(img, BufferedImage.TYPE_INT_ARGB);
    assertMessage(ImgUtil.toJson(img));
  }

  @Test
  public void printImageToBuffer() {
    BufferedImage img = readImage("batman.png");
    assertMessage(BasePrinter.toString(img));
  }

  @Test
  public void fontSerialization() {
    JSMap m = ImgUtil.toJson(ImgUtil.FONT_DEFAULT);
    Font f = ImgUtil.parseFont(m);
    JSMap m2 = map();
    m2.put("orig", m);
    m2.put("parsed", ImgUtil.toJson(f));
    assertMessage(m2);
  }

  @Test
  public void problematicMonoImage() {
    // I have solved the problem that resulted from some images
    if (true) {
      return;
    }
    MonoImage img = ImgUtil.decompressRAX(Files.toByteArray(testFile("problem.rax"), "problem.rax"), null);
    int problemCount = 0;
    IPoint sz = img.size();
    int i = 0;

    for (int y = 0; y < sz.y; y++) {
      boolean prevBad = false;
      int prevPixValue = -1;
      for (int x = 0; x < sz.x; x++) {
        short pix = img.pixels()[i];
        boolean newBad = (pix < 0 || pix > 32700);
        if (newBad) {
          if (verbose())
            log("problem pixel:", Integer.toHexString(((int) pix) & 0xffff), "at:", x, y);
          problemCount++;
          if (verbose() && !prevBad)
            pr("prev good pixel:", Integer.toHexString(prevPixValue));
        }
        prevPixValue = pix;
        prevBad = newBad;
        i++;
      }
    }
    checkState(problemCount > 0);

    int cx = 310;
    int cy = 52;
    BufferedImage bi = MonoImageUtil.to8BitRGBBufferedImage(img);
    Graphics2D g = bi.createGraphics();
    g.setColor(Color.GREEN);
    int r = 10;
    g.drawOval(cx - r, cy - r, r, r);
    ImgUtil.writeImage(files(), bi, Files.getDesktopFile("problem_result.png"));
  }

  private float checkpoint() {
    long currentCheckpoint = System.currentTimeMillis();
    long last = mPreviousCheckpoint;
    if (last == 0)
      last = currentCheckpoint;
    long elapsed = currentCheckpoint - last;
    mPreviousCheckpoint = currentCheckpoint;
    return elapsed / 1000f;
  }

  private long mPreviousCheckpoint;

  @Test
  public void pngCompressionLength() {

    if (false) {
      // Issue #8; didn't figure out how to improve png compression
      return;
    }

    File raxFile = testFile("problem.rax");
    byte[] rax = Files.toByteArray(raxFile, "rax file");
    byte[] raxZipped = DataUtil.compress(rax);

    MonoImage mono = ImgUtil.readRax(raxFile);
    BufferedImage bImg = MonoImageUtil.toBufferedImage(mono);

    long lengthPngZip;

    {
      byte[] png1 = ImgUtil.toPNG(bImg);
      byte[] zipped = DataUtil.compress(png1);
      lengthPngZip = zipped.length;
    }

    int reps = 500;

    long lengthRax = 0;
    float timeRax = 0;
    checkpoint();
    {
      byte[] rax2 = null;
      for (int i = 0; i < reps; i++) {
        MonoImage mi = MonoImageUtil.construct(bImg);
        rax2 = ImgUtil.compressRAX(mi);
      }
      timeRax = checkpoint();
      lengthRax = rax2.length;
    }

    float timePng = 0;
    long lengthPng = 0;
    checkpoint();
    {
      byte[] png = null;
      for (int i = 0; i < reps; i++) {
        png = ImgUtil.toPNG(bImg);
      }
      timePng = checkpoint();
      lengthPng = png.length;
      validatePNG(png, mono);
    }

    long lengthPngAlt1 = 0;
    float timePngAlt1 = 0;
    checkpoint();
    {
      byte[] png2 = null;
      for (int i = 0; i < reps; i++) {
        png2 = new PngEncoder().withBufferedImage(bImg)//
            .withCompressionLevel(9) //
            .withTryIndexedEncoding(true) //
            .withPredictorEncoding(true) //
            .toBytes();
      }
      lengthPngAlt1 = png2.length;
      timePngAlt1 = checkpoint();
      validatePNG(png2, mono);
    }

    //    if (false) { // This has no effect on compression size and is slightly slower
    //      {
    //        byte[] png2 = null;
    //        for (int i = 0; i < reps; i++) {
    //          png2 = new PngEncoder().withBufferedImage(bImg)//
    //              .withCompressionLevel(9) //
    //              //.withTryIndexedEncoding(true) //
    //              .withPredictorEncoding(true) //
    //              .toBytes();
    //        }
    //        checkpoint("parms2");
    //        m.put("length, png alt 2", png2.length);
    //        validatePNG(png2, mono);
    //      }
    //    }

    long lengthPngAlt3 = 0;
    float timePngAlt3 = 0;
    checkpoint();
    {
      byte[] png2 = null;
      for (int i = 0; i < reps; i++) {
        png2 = new PngEncoder().withBufferedImage(bImg)//
            .withCompressionLevel(9) //
            .withTryIndexedEncoding(true) //
            //.withPredictorEncoding(true) //
            .toBytes();
      }
      lengthPngAlt3 = png2.length;
      timePngAlt3 = checkpoint();
      validatePNG(png2, mono);
    }

    long lengthJimg = 0;
    float timeJimg = 0;
    {
      int[] jmg = null;
      for (int i = 0; i < reps; i++) {
        JImage jm = JImageUtil.from(bImg);
        jmg = JImageUtil.encode(jm, null);
      }
      lengthJimg = jmg.length * 4; // 4 bytes per int
      JImage jm2 = JImageUtil.decode(jmg);
      validateMonoImage(mono.pixels(), jm2.wPixels());
      timeJimg = checkpoint();
    }

    JSMap m = map();
    
    m.putNumbered("rax length", lengthRax);
    m.putNumbered("rax time", ratio(timeRax, timeRax));

    m.putNumbered("raxz length", ratio(raxZipped.length, lengthRax));

    m.putNumbered("png length", ratio(lengthPng, lengthRax));
    m.putNumbered("png time", ratio(timePng, timeRax));
    m.putNumbered("pngz length", ratio(lengthPngZip, lengthRax));

    m.putNumbered("png1 length", ratio(lengthPngAlt1, lengthRax));
    m.putNumbered("png1 time", ratio(timePngAlt1, timeRax));

    m.putNumbered("png3 length", ratio(lengthPngAlt3, lengthRax));
    m.putNumbered("png3 time", ratio(timePngAlt3, timeRax));

    m.putNumbered("jimg length", ratio(lengthJimg, lengthRax));
    m.putNumbered("jimg time", ratio(timeJimg, timeRax));

    pr(m);
  }

  private static String ratio(double a, double b) {
    return String.format("%6.3f", a / b);
  }

  private void validatePNG(byte[] png, MonoImage monoExpected) {
    BufferedImage bImg2 = ImgUtil.read(png);
    MonoImage mono2 = MonoImageUtil.construct(bImg2);
    validateMonoImage(monoExpected.pixels(), mono2.pixels());
  }

  private void validateMonoImage(short[] pixels, short[] expectedPixels) {
    checkState(Arrays.equals(expectedPixels, pixels), "monochrome pixel arrays are different");
  }

  @Test
  public void monoImageToPNGAndBack() {
    final boolean db = false;
    MonoImage mono = randomImage(new IPoint(8, 8), 0x010, 0xfc00);
    BufferedImage bImg = MonoImageUtil.toBufferedImage(mono);
    if (db) {
      ImageStats stats = MonoImageUtil.constructSmallStats(bImg);
      pr(stats);
    }
    byte[] png = ImgUtil.toPNG(bImg);
    validatePNG(png, mono);
  }

  private MonoImage randomImage(IPoint size, int minPixelValue, int maxPixelValue) {
    if (size == null)
      size = new IPoint(160, 128);
    short[] pixels = new short[size.product()];
    for (int i = 0; i < pixels.length; i++) {
      pixels[i] = (short) (((int) (random().nextGaussian() * (maxPixelValue - minPixelValue)
          + minPixelValue)));
    }
    return MonoImage.newBuilder().size(size).pixels(pixels).build();
  }

  private BufferedImage readImage(String name) {
    return ImgUtil.read(testFile(name));
  }

}
