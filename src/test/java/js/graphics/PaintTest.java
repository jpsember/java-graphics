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

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.junit.Test;

import js.base.BasePrinter;
import js.file.Files;
import js.json.JSMap;
import js.testutil.MyTestCase;
import static js.base.Tools.*;

public class PaintTest extends MyTestCase {

  @Override
  public String testDataName() {
    loadTools();
    return "img_util_test_data";
  }

  @Test
  public void serialization() {
    JSMap m = map();
    m.put("default", Paint.DEFAULT_INSTANCE);
    pt().font(new Font("Courier New", Font.BOLD, 20), 1f);
    m.put("constructed", pt());
    assertMessage(m);
  }

  @Test
  public void plotText() {
    pt().font(1f);
    pt().apply(g());
    // TODO: Why is drawString so slow (first time at least)? 
    g().drawString("Hello", 20, 40);
    assertImage();
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

  private void assertImage() {
    if (verbose())
      ImgUtil.writeImage(Files.S, img(), Files.getDesktopFile("_test_result_" + name() + ".png"));
    ImgUtil.writeImage(Files.S, img(), generatedFile("output.png"));
    assertGenerated();
  }

  private BufferedImage readImage(String name) {
    return ImgUtil.read(testFile(name));
  }

  private Graphics2D g() {
    if (mGraphics == null) {
      mGraphics = img().createGraphics();
    }
    return mGraphics;
  }

  private BufferedImage img() {
    if (mBufferedImage == null) {
      setTestDataName("img_util_test_data");
      mBufferedImage = ImgUtil.read(testFile("batman.png"));
    }
    return mBufferedImage;
  }

  private Paint.Builder pt() {
    if (mPaintBuilder == null)
      mPaintBuilder = Paint.newBuilder();
    return mPaintBuilder;
  }

  private BufferedImage mBufferedImage;
  private Graphics2D mGraphics;
  private Paint.Builder mPaintBuilder;

}
