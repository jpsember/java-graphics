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

import org.junit.Test;

import static js.base.Tools.*;

import js.testutil.MyTestCase;

public class InspectorTest extends MyTestCase {

  @Test
  public void smallSet() {
    insp().maxSamples(10);
    growPop(100);
    assertGenerated();
  }

  @Test
  public void largeSet() {
    insp().maxSamples(500);
    growPop(5000);
    assertGenerated();
  }

  @Test
  public void largeSetSmallPop() {
    insp().maxSamples(500);
    growPop(100);
    assertGenerated();
  }

  private void growPop(int limit) {
    while (popCount() < limit)
      incPop();
  }

  private void incPop() {

    int i = mPopCount++;

    Inspector x = insp();

    log("pop:", i);
    x.create("alpha");
    if (x.used()) {
      log("...sample is used");
      x.json(map().put("alpha", i));
    }
    x.create("beta");
    if (x.used()) {
      x.json(map().put("beta", i));
    }
  }

  private int popCount() {
    return mPopCount;
  }

  private int mPopCount;

  private Inspector insp() {
    if (mInspector == null) {
      mInspector = Inspector.build(generatedFile("inspection")).seed(1234);
    }
    return mInspector;
  }

  private Inspector mInspector;

}
