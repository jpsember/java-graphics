package js.graphics.gen;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import js.data.AbstractData;
import js.data.DataUtil;
import js.geometry.IPoint;
import js.json.JSMap;

public class ImageStats implements AbstractData {

  public String problem() {
    return mProblem;
  }

  public int min() {
    return mMin;
  }

  public int max() {
    return mMax;
  }

  public int mean() {
    return mMean;
  }

  public int median() {
    return mMedian;
  }

  public int range() {
    return mRange;
  }

  public int clippedRange() {
    return mClippedRange;
  }

  public int count() {
    return mCount;
  }

  public IPoint minLoc() {
    return mMinLoc;
  }

  public IPoint maxLoc() {
    return mMaxLoc;
  }

  public int[] histogram() {
    return mHistogram;
  }

  public Map<String, String> histogramPlot() {
    return mHistogramPlot;
  }

  public short[] cdf() {
    return mCdf;
  }

  public Map<String, String> cdfPlot() {
    return mCdfPlot;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "problem";
  protected static final String _1 = "min";
  protected static final String _2 = "max";
  protected static final String _3 = "mean";
  protected static final String _4 = "median";
  protected static final String _5 = "range";
  protected static final String _6 = "clipped_range";
  protected static final String _7 = "count";
  protected static final String _8 = "min_loc";
  protected static final String _9 = "max_loc";
  protected static final String _10 = "histogram";
  protected static final String _11 = "histogram_plot";
  protected static final String _12 = "cdf";
  protected static final String _13 = "cdf_plot";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    m.putUnsafe(_0, mProblem);
    m.putUnsafe(_1, mMin);
    m.putUnsafe(_2, mMax);
    m.putUnsafe(_3, mMean);
    m.putUnsafe(_4, mMedian);
    m.putUnsafe(_5, mRange);
    m.putUnsafe(_6, mClippedRange);
    m.putUnsafe(_7, mCount);
    m.putUnsafe(_8, mMinLoc.toJson());
    m.putUnsafe(_9, mMaxLoc.toJson());
    m.putUnsafe(_10, DataUtil.encodeBase64Maybe(mHistogram));
    {
      JSMap j = new JSMap();
      for (Map.Entry<String, String> e : mHistogramPlot.entrySet())
        j.put(e.getKey(), e.getValue());
      m.put(_11, j);
    }
    m.putUnsafe(_12, DataUtil.encodeBase64Maybe(mCdf));
    {
      JSMap j = new JSMap();
      for (Map.Entry<String, String> e : mCdfPlot.entrySet())
        j.put(e.getKey(), e.getValue());
      m.put(_13, j);
    }
    return m;
  }

  @Override
  public ImageStats build() {
    return this;
  }

  @Override
  public ImageStats parse(Object obj) {
    return new ImageStats((JSMap) obj);
  }

  private ImageStats(JSMap m) {
    mProblem = m.opt(_0, "");
    mMin = m.opt(_1, 0);
    mMax = m.opt(_2, 0);
    mMean = m.opt(_3, 0);
    mMedian = m.opt(_4, 0);
    mRange = m.opt(_5, 0);
    mClippedRange = m.opt(_6, 0);
    mCount = m.opt(_7, 0);
    {
      mMinLoc = IPoint.DEFAULT_INSTANCE;
      Object x = m.optUnsafe(_8);
      if (x != null) {
        mMinLoc = IPoint.DEFAULT_INSTANCE.parse(x);
      }
    }
    {
      mMaxLoc = IPoint.DEFAULT_INSTANCE;
      Object x = m.optUnsafe(_9);
      if (x != null) {
        mMaxLoc = IPoint.DEFAULT_INSTANCE.parse(x);
      }
    }
    {
      mHistogram = DataUtil.EMPTY_INT_ARRAY;
      Object x = m.optUnsafe(_10);
      if (x != null) {
        mHistogram = DataUtil.parseIntsFromArrayOrBase64(x);
      }
    }
    {
      mHistogramPlot = DataUtil.emptyMap();
      {
        JSMap m2 = m.optJSMap("histogram_plot");
        if (m2 != null && !m2.isEmpty()) {
          Map<String, String> mp = new ConcurrentHashMap<>();
          for (Map.Entry<String, Object> e : m2.wrappedMap().entrySet())
            mp.put(e.getKey(), (String) e.getValue());
          mHistogramPlot = DataUtil.immutableCopyOf(mp) /*DEBUG*/ ;
        }
      }
    }
    {
      mCdf = DataUtil.EMPTY_SHORT_ARRAY;
      Object x = m.optUnsafe(_12);
      if (x != null) {
        mCdf = DataUtil.parseShortsFromArrayOrBase64(x);
      }
    }
    {
      mCdfPlot = DataUtil.emptyMap();
      {
        JSMap m2 = m.optJSMap("cdf_plot");
        if (m2 != null && !m2.isEmpty()) {
          Map<String, String> mp = new ConcurrentHashMap<>();
          for (Map.Entry<String, Object> e : m2.wrappedMap().entrySet())
            mp.put(e.getKey(), (String) e.getValue());
          mCdfPlot = DataUtil.immutableCopyOf(mp) /*DEBUG*/ ;
        }
      }
    }
  }

  public static Builder newBuilder() {
    return new Builder(DEFAULT_INSTANCE);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object)
      return true;
    if (object == null || !(object instanceof ImageStats))
      return false;
    ImageStats other = (ImageStats) object;
    if (other.hashCode() != hashCode())
      return false;
    if (!(mProblem.equals(other.mProblem)))
      return false;
    if (!(mMin == other.mMin))
      return false;
    if (!(mMax == other.mMax))
      return false;
    if (!(mMean == other.mMean))
      return false;
    if (!(mMedian == other.mMedian))
      return false;
    if (!(mRange == other.mRange))
      return false;
    if (!(mClippedRange == other.mClippedRange))
      return false;
    if (!(mCount == other.mCount))
      return false;
    if (!(mMinLoc.equals(other.mMinLoc)))
      return false;
    if (!(mMaxLoc.equals(other.mMaxLoc)))
      return false;
    if (!(Arrays.equals(mHistogram, other.mHistogram)))
      return false;
    if (!(mHistogramPlot.equals(other.mHistogramPlot)))
      return false;
    if (!(Arrays.equals(mCdf, other.mCdf)))
      return false;
    if (!(mCdfPlot.equals(other.mCdfPlot)))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      r = r * 37 + mProblem.hashCode();
      r = r * 37 + mMin;
      r = r * 37 + mMax;
      r = r * 37 + mMean;
      r = r * 37 + mMedian;
      r = r * 37 + mRange;
      r = r * 37 + mClippedRange;
      r = r * 37 + mCount;
      r = r * 37 + mMinLoc.hashCode();
      r = r * 37 + mMaxLoc.hashCode();
      r = r * 37 + Arrays.hashCode(mHistogram);
      r = r * 37 + mHistogramPlot.hashCode();
      r = r * 37 + Arrays.hashCode(mCdf);
      r = r * 37 + mCdfPlot.hashCode();
      m__hashcode = r;
    }
    return r;
  }

  protected String mProblem;
  protected int mMin;
  protected int mMax;
  protected int mMean;
  protected int mMedian;
  protected int mRange;
  protected int mClippedRange;
  protected int mCount;
  protected IPoint mMinLoc;
  protected IPoint mMaxLoc;
  protected int[] mHistogram;
  protected Map<String, String> mHistogramPlot;
  protected short[] mCdf;
  protected Map<String, String> mCdfPlot;
  protected int m__hashcode;

  public static final class Builder extends ImageStats {

    private Builder(ImageStats m) {
      mProblem = m.mProblem;
      mMin = m.mMin;
      mMax = m.mMax;
      mMean = m.mMean;
      mMedian = m.mMedian;
      mRange = m.mRange;
      mClippedRange = m.mClippedRange;
      mCount = m.mCount;
      mMinLoc = m.mMinLoc;
      mMaxLoc = m.mMaxLoc;
      mHistogram = m.mHistogram;
      mHistogramPlot = DataUtil.immutableCopyOf(m.mHistogramPlot) /*DEBUG*/ ;
      mCdf = m.mCdf;
      mCdfPlot = DataUtil.immutableCopyOf(m.mCdfPlot) /*DEBUG*/ ;
    }

    @Override
    public Builder toBuilder() {
      return this;
    }

    @Override
    public int hashCode() {
      m__hashcode = 0;
      return super.hashCode();
    }

    @Override
    public ImageStats build() {
      ImageStats r = new ImageStats();
      r.mProblem = mProblem;
      r.mMin = mMin;
      r.mMax = mMax;
      r.mMean = mMean;
      r.mMedian = mMedian;
      r.mRange = mRange;
      r.mClippedRange = mClippedRange;
      r.mCount = mCount;
      r.mMinLoc = mMinLoc;
      r.mMaxLoc = mMaxLoc;
      r.mHistogram = mHistogram;
      r.mHistogramPlot = mHistogramPlot;
      r.mCdf = mCdf;
      r.mCdfPlot = mCdfPlot;
      return r;
    }

    public Builder problem(String x) {
      mProblem = (x == null) ? "" : x;
      return this;
    }

    public Builder min(int x) {
      mMin = x;
      return this;
    }

    public Builder max(int x) {
      mMax = x;
      return this;
    }

    public Builder mean(int x) {
      mMean = x;
      return this;
    }

    public Builder median(int x) {
      mMedian = x;
      return this;
    }

    public Builder range(int x) {
      mRange = x;
      return this;
    }

    public Builder clippedRange(int x) {
      mClippedRange = x;
      return this;
    }

    public Builder count(int x) {
      mCount = x;
      return this;
    }

    public Builder minLoc(IPoint x) {
      mMinLoc = (x == null) ? IPoint.DEFAULT_INSTANCE : x.build();
      return this;
    }

    public Builder maxLoc(IPoint x) {
      mMaxLoc = (x == null) ? IPoint.DEFAULT_INSTANCE : x.build();
      return this;
    }

    public Builder histogram(int[] x) {
      mHistogram = (x == null) ? DataUtil.EMPTY_INT_ARRAY : x;
      return this;
    }

    public Builder histogramPlot(Map<String, String> x) {
      mHistogramPlot = DataUtil.immutableCopyOf((x == null) ? DataUtil.emptyMap() : x) /*DEBUG*/ ;
      return this;
    }

    public Builder cdf(short[] x) {
      mCdf = (x == null) ? DataUtil.EMPTY_SHORT_ARRAY : x;
      return this;
    }

    public Builder cdfPlot(Map<String, String> x) {
      mCdfPlot = DataUtil.immutableCopyOf((x == null) ? DataUtil.emptyMap() : x) /*DEBUG*/ ;
      return this;
    }

  }

  public static final ImageStats DEFAULT_INSTANCE = new ImageStats();

  private ImageStats() {
    mProblem = "";
    mMinLoc = IPoint.DEFAULT_INSTANCE;
    mMaxLoc = IPoint.DEFAULT_INSTANCE;
    mHistogram = DataUtil.EMPTY_INT_ARRAY;
    mHistogramPlot = DataUtil.emptyMap();
    mCdf = DataUtil.EMPTY_SHORT_ARRAY;
    mCdfPlot = DataUtil.emptyMap();
  }

}
