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
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;

import js.base.BasePrinter;
import js.data.DataUtil;
import js.data.ShortArray;
import js.file.Files;
import js.geometry.IPoint;
import js.graphics.gen.ImageStats;
import js.graphics.gen.MonoImage;
import js.json.JSMap;
import js.testutil.MyTestCase;

import org.junit.Test;

import static js.base.Tools.*;
import static org.junit.Assert.*;

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

  @Test
  public void pngCompressionLength() {

    JSMap m = map();

    File raxFile = testFile("problem.rax");
    byte[] rax = Files.toByteArray(raxFile, "rax file");
    m.put("length, rax", rax.length);
    byte[] raxZipped = DataUtil.compress(rax);
    m.put("length, rax+zip", raxZipped.length);

    MonoImage mono = ImgUtil.readRax(raxFile);
    BufferedImage bImg = MonoImageUtil.toBufferedImage(mono);

    byte[] png1 = ImgUtil.toPNG(bImg);
    m.put("length, png", png1.length);

    byte[] zipped = DataUtil.compress(png1);
    m.put("length, png+zip", zipped.length);

    pr(m);
    if (false) { // Can't seem to find an import for PNGMetadata

      final String formatName = "png";

      for (Iterator<ImageWriter> iw = ImageIO.getImageWritersByFormatName(formatName); iw.hasNext();) {
        ImageWriter writer = iw.next();
        ImageWriteParam writeParam = writer.getDefaultWriteParam();
        ImageTypeSpecifier typeSpecifier = ImageTypeSpecifier
            .createFromBufferedImageType(BufferedImage.TYPE_INT_RGB);
        IIOMetadata metadata = writer.getDefaultImageMetadata(typeSpecifier, writeParam);

        pr("metadata:", metadata);
        // PNGMetadata png = (PNGMetadata) metadata;

        // IIOMetadataNode stdComp = png.getStandardCompressionNode();
        //        NamedNodeMap nnMap = stdComp.getFirstChild().getAttributes();
        //        String nnValue = nnMap.item(0).getNodeValue();
        //        System.out.println("Compression name "+nnValue);
      }
    }
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
    BufferedImage bImg2 = ImgUtil.read(png);
    MonoImage mono2 = MonoImageUtil.construct(bImg2);
    if (db) {
      JSMap m = map().put("pix1:", ShortArray.with(mono.pixels()));
      m.put("pix2:", ShortArray.with(mono2.pixels()));
      pr(m);
    }
    boolean equal = Arrays.equals(mono.pixels(), mono2.pixels());
    assertTrue(equal);
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
