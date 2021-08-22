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

import static js.base.Tools.*;

import java.awt.image.BufferedImage;

import org.junit.Test;

import js.geometry.IPoint;
import js.geometry.Matrix;
import js.geometry.MyMath;
import js.testutil.MyTestCase;

public class PlotterTest extends MyTestCase {

  @Override
  public String testDataName() {
    loadTools();
    return "img_util_test_data";
  }

  @Test
  public void rects() {
    p().withCanvas(200, 100) //
        .drawRect(5, 5, 30, 30)//
        .withRed()//
        .drawRect(8, 8, 35, 20)//
        .withPurple()//
        .fillRect(20, 12, 15, 15)//
        .write();
    assertGenerated();
  }

  @Test
  public void intoImage() {
    p().with(readImage("batman.png")) //
        .drawRect(5, 5, 30, 30)//
        .withRed()//
        .drawRect(8, 8, 35, 20)//
        .withPurple()//
        .fillRect(20, 12, 15, 15)//
        .write();
    assertGenerated();
  }

  @Test
  public void withTransform() {
    p().withCanvas(200, 100);
    IPoint sz = ImgUtil.size(p().image());
    IPoint center = sz.scaledBy(.5f);
    p().with(Matrix.getTranslate(center.negate()).pcat(Matrix.getRotate(MyMath.M_DEG * 30))
        .pcat(Matrix.getTranslate(center)).pcat(Matrix.getFlipVertically(0, sz.y)));
    int pad = 4;
    p()//   
        .drawRect(pad, pad, sz.x - 2 * pad, sz.y - 2 * pad) //
        .withBlue()//
        .drawRect(5, 5, 30, 30)//
        .withRed()//
        .drawRect(8, 8, 35, 20)//
        .withPurple()//
        .fillRect(20, 12, 15, 15)//
        .write();
    assertGenerated();
  }

  @Test
  public void retainOldImages() {
    for (int i = 0; i < 8; i++) {
      mPlotter = null;
      p().withCanvas(50, 50) //
          .drawRect(5 + i, 5 + i, 30, 30)//
          .write();
    }
    assertGenerated();
  }

  @Test
  public void purge() {
    for (int i = 0; i < 8; i++) {
      mPlotter = null;
      if (i == 3)
        p().purge();
      p().withCanvas(50, 50) //
          .drawRect(5 + i, 5 + i, 30, 30)//
          .write();
    }
    assertGenerated();
  }

  private BufferedImage readImage(String name) {
    return ImgUtil.read(testFile(name));
  }

  private Plotter p() {
    if (mPlotter == null) {
      mPlotter = Plotter.build();
      mPlotter.setVerbose(verbose());
      mPlotter.directory(generatedDir());
    }
    return mPlotter;
  }

  private Plotter mPlotter;

}
