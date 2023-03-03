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

import js.base.BasePrinter;
import js.data.DataUtil;
import js.file.Files;
import js.geometry.IPoint;
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
          pr("problem pixel:", Integer.toHexString(((int) pix) & 0xffff), "at:", x, y);
          problemCount++;
          if (!prevBad)
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

  private BufferedImage readImage(String name) {
    return ImgUtil.read(testFile(name));
  }

}
