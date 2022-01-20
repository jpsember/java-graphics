package js.graphics;

import js.base.BaseObject;
import js.geometry.IPoint;
import js.geometry.Polygon;

import static js.base.Tools.*;

import java.util.List;

/**
 * Smooths a polygon by generating Catmull-Rom splines for its edges
 */
public final class PolygonSmoother extends BaseObject {

  public PolygonSmoother withPolygon(Polygon poly) {
    mOrigPoly = poly;
    return this;
  }

  public Polygon result() {
    if (mResult == null)
      calculateResult();
    return mResult;
  }

  private void calculateResult() {
    Polygon src = mOrigPoly;
    checkNotNull(src, "no polygon provided");
    checkArgument(src.numVertices() >= 3, "insufficient number of vertices");

    // See: https://www.cs.cmu.edu/~fp/courses/graphics/asst5/catmullRom.pdf

    float t = 0.5f;

    // Geometry matrix 
    final float c00 = 0, c01 = 1, c02 = 0, c03 = 0;
    final float c10 = -t, c11 = 0, c12 = t, c13 = 0;
    final float c20 = 2 * t, c21 = t - 3, c22 = 3 - 2 * t, c23 = -t;
    final float c30 = -t, c31 = 2 - t, c32 = t - 2, c33 = t;

    List<IPoint> ivertices = arrayList();

    for (int segIndex = 0; segIndex < src.numVertices(); segIndex++) {

      IPoint pa = src.vertexMod(segIndex - 2);
      IPoint pb = src.vertexMod(segIndex - 1);
      IPoint pc = src.vertexMod(segIndex);
      IPoint pd = src.vertexMod(segIndex + 1);

      int nsteps = 10;
      for (int step = 0; step < nsteps; step++) {
        float u0 = 1;
        float u1 = (step / (float) nsteps);
        float u2 = u1 * u1;
        float u3 = u2 * u1;

        float f0 = u0 * c00 + u1 * c10 + u2 * c20 + u3 * c30;
        float f1 = u0 * c01 + u1 * c11 + u2 * c21 + u3 * c31;
        float f2 = u0 * c02 + u1 * c12 + u2 * c22 + u3 * c32;
        float f3 = u0 * c03 + u1 * c13 + u2 * c23 + u3 * c33;

        float ix = f0 * pa.x + f1 * pb.x + f2 * pc.x + f3 * pd.x;
        float iy = f0 * pa.y + f1 * pb.y + f2 * pc.y + f3 * pd.y;
        IPoint ipt = new IPoint(ix, iy);
        ivertices.add(ipt);
      }
    }
    mResult = src.withVertices(ivertices);
  }

  private Polygon mOrigPoly;
  private Polygon mResult;
}
