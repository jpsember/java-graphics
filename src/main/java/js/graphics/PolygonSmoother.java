package js.graphics;

import js.base.BaseObject;
import js.geometry.FPoint;
import js.geometry.IPoint;
import js.geometry.MyMath;
import js.geometry.Polygon;

import static js.base.Tools.*;

import java.util.List;

/**
 * <pre>
 * 
 * Smooths a polygon by generating Catmull-Rom splines for its edges
 * 
 * Notes
 * -----------------------------------------------------------------------------------
 * If the polygons are quite small, there can be unwanted visual effects (staircasing)
 * due to quantization of the floating point intermediate results to (integer-valued) Polygons.
 * 
 * Does not yet support open polygons.
 * 
 * </pre>
 */
public final class PolygonSmoother extends BaseObject {

  public PolygonSmoother withPolygon(Polygon poly) {
    mSourcePolygon = poly;
    // Discard any previously calculated results
    mResult = null;
    return this;
  }

  /**
   * (not applicable if doing centripetal version)
   */
  public PolygonSmoother withStepSize(float stepSize) {
    mStepSize = stepSize;
    return this;
  }

  /**
   * (not applicable if doing centripetal version)
   */
  public PolygonSmoother withInsetDistance(float distance) {
    mInsetDistance = distance;
    return this;
  }

  /**
   * (not applicable if doing centripetal version)
   */
  public PolygonSmoother withTau(float tau) {
    mTau = tau;
    return this;
  }

  public PolygonSmoother withCentripetalFactor(float factor, int stepCount) {
    mCentripetalFactor = factor;
    mCentripetalSteps = stepCount;
    return this;
  }

  public Polygon result() {
    if (mResult == null)
      calculateResult();
    return mResult;
  }

  private void calculateResult() {
    Polygon sourcePolygon = checkNotNull(mSourcePolygon, "no polygon provided");
    checkArgument(sourcePolygon.isWellDefined(), "ill defined polygon");
    List<FPoint> src = preprocessVertices(sourcePolygon);
    List<FPoint> target = arrayList();

    if (mCentripetalFactor != 0)
      centripetalCatmullRom(src, target);
    else
      catmullRom(src, target);

    mResult = new Polygon(IPoint.convert(target));
  }

  private void catmullRom(List<FPoint> src, List<FPoint> target) {

    // See: https://www.cs.cmu.edu/~fp/courses/graphics/asst5/catmullRom.pdf

    float t = mTau;

    // Geometry matrix 
    final float c00 = 0, c01 = 1, c02 = 0, c03 = 0;
    final float c10 = -t, c11 = 0, c12 = t, c13 = 0;
    final float c20 = 2 * t, c21 = t - 3, c22 = 3 - 2 * t, c23 = -t;
    final float c30 = -t, c31 = 2 - t, c32 = t - 2, c33 = t;

    for (int segIndex = 0; segIndex < src.size(); segIndex++) {
      FPoint pa = getMod(src, segIndex - 2);
      FPoint pb = getMod(src, segIndex - 1);
      FPoint pc = getMod(src, segIndex);
      FPoint pd = getMod(src, segIndex + 1);

      // make the step size approximately constant by looking at the length of the edge.
      // BUT: is the length of the single segment a good estimate of the length of the curve generated?

      float stepSize = mStepSize;
      float estimatedSegmentLength = MyMath.distanceBetween(pb, pc);
      int stepCount = Math.max(1, Math.round(estimatedSegmentLength / stepSize));
      float stepIncrement = 1f / stepCount;

      // Not sure this has much effect - space steps so we're centered around t= 0.5
      //
      float stepAccum = (1 - stepCount * stepIncrement) / 2;

      for (int stepIndex = 0; stepIndex < stepCount; stepIndex++, stepAccum += stepIncrement) {

        float u0 = 1;
        float u1 = stepAccum;
        float u2 = u1 * u1;
        float u3 = u2 * u1;

        float f0 = u0 * c00 + u1 * c10 + u2 * c20 + u3 * c30;
        float f1 = u0 * c01 + u1 * c11 + u2 * c21 + u3 * c31;
        float f2 = u0 * c02 + u1 * c12 + u2 * c22 + u3 * c32;
        float f3 = u0 * c03 + u1 * c13 + u2 * c23 + u3 * c33;

        float ix = f0 * pa.x + f1 * pb.x + f2 * pc.x + f3 * pd.x;
        float iy = f0 * pa.y + f1 * pb.y + f2 * pc.y + f3 * pd.y;

        target.add(new FPoint(ix, iy));
      }
    }
  }

  private void centripetalCatmullRom(List<FPoint> src, List<FPoint> target2) {
    // See: https://en.wikipedia.org/wiki/Centripetal_Catmull%E2%80%93Rom_spline

    for (int segIndex = 0; segIndex < src.size(); segIndex++) {
      FPoint p0 = getMod(src, segIndex - 2);
      FPoint p1 = getMod(src, segIndex - 1);
      FPoint p2 = getMod(src, segIndex);
      FPoint p3 = getMod(src, segIndex + 1);

      float t0 = 0;
      float t1 = tj(t0, p0, p1);
      float t2 = tj(t1, p1, p2);
      float t3 = tj(t2, p2, p3);

      int stepTotal = mCentripetalSteps;
      float stepSize = (t2 - t1) / stepTotal;
      float t = t1 + stepSize / 2;

      for (int stepIndex = 0; stepIndex < stepTotal; stepIndex++, t += stepSize) {

        float a1p0 = (t1 - t) / (t1 - t0);
        float a1p1 = (t - t0) / (t1 - t0);

        float a2p1 = (t2 - t) / (t2 - t1);
        float a2p2 = (t - t1) / (t2 - t1);

        float a3p2 = (t3 - t) / (t3 - t2);
        float a3p3 = (t - t2) / (t3 - t2);

        float b1a1 = (t2 - t) / (t2 - t0);
        float b1a2 = (t - t0) / (t2 - t0);

        float b2a2 = (t3 - t) / (t3 - t1);
        float b2a3 = (t - t1) / (t3 - t1);

        float cb1 = (t2 - t) / (t2 - t1);
        float cb2 = (t - t1) / (t2 - t1);

        float a1x = a1p0 * p0.x + a1p1 * p1.x;
        float a1y = a1p0 * p0.y + a1p1 * p1.y;

        float a2x = a2p1 * p1.x + a2p2 * p2.x;
        float a2y = a2p1 * p1.y + a2p2 * p2.y;

        float a3x = a3p2 * p2.x + a3p3 * p3.x;
        float a3y = a3p2 * p2.y + a3p3 * p3.y;

        float b1x = b1a1 * a1x + b1a2 * a2x;
        float b1y = b1a1 * a1y + b1a2 * a2y;

        float b2x = b2a2 * a2x + b2a3 * a3x;
        float b2y = b2a2 * a2y + b2a3 * a3y;

        float cx = cb1 * b1x + cb2 * b2x;
        float cy = cb1 * b1y + cb2 * b2y;

        target2.add(new FPoint(cx, cy));
      }
    }
  }

  private float tj(float t, FPoint a, FPoint b) {
    float sqdist = MyMath.squaredDistanceBetween(a, b);
    float alpha = mCentripetalFactor;
    return t + (float) Math.pow(sqdist, alpha);
  }

  private List<FPoint> preprocessVertices(Polygon p) {
    List<FPoint> result = arrayList();

    if (mInsetDistance == 0) {
      for (IPoint v : p.vertices())
        result.add(v.toFPoint());
      return result;
    }

    // If edge length is small, store edge midpoint;
    // Otherwise, the endpoints inset by a constant amount 

    float insetDist = mInsetDistance;
    float minSegLength = 3 * insetDist;

    FPoint p0 = p.lastVertex().toFPoint();
    for (IPoint i1 : p.vertices()) {
      FPoint p1 = i1.toFPoint();
      float segLen = MyMath.distanceBetween(p0, p1);
      if (segLen < minSegLength) {
        result.add(FPoint.midPoint(p0, p1));
      } else {
        float t = insetDist / segLen;
        result.add(FPoint.interpolate(p0, p1, t));
        result.add(FPoint.interpolate(p0, p1, 1 - t));
      }
      p0 = p1;
    }
    return result;
  }

  private Polygon mSourcePolygon;
  private float mTau = 0.5f;
  private float mStepSize = 1f;
  private float mInsetDistance;
  private float mCentripetalFactor;
  private int mCentripetalSteps;
  private Polygon mResult;
}
