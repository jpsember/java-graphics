package js.graphics.gen;

import java.util.List;
import js.data.AbstractData;
import js.data.DataUtil;
import js.graphics.ScriptElement;
import js.json.JSList;
import js.json.JSMap;

public class Script implements AbstractData {

  public ScriptUsage usage() {
    return mUsage;
  }

  public List<ScriptElement> items() {
    return mItems;
  }

  public ScriptMetadata metadata() {
    return mMetadata;
  }

  public Boolean omit() {
    return mOmit;
  }

  public Boolean retain() {
    return mRetain;
  }

  public JSMap widgets() {
    return mWidgets;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "usage";
  protected static final String _1 = "items";
  protected static final String _2 = "metadata";
  protected static final String _3 = "omit";
  protected static final String _4 = "retain";
  protected static final String _5 = "widgets";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    m.putUnsafe(_0, mUsage.toString().toLowerCase());
    {
      JSList j = new JSList();
      for (ScriptElement x : mItems)
        j.add(x.toJson());
      m.put(_1, j);
    }
    if (mMetadata != null) {
      m.putUnsafe(_2, mMetadata.toJson());
    }
    if (mOmit != null) {
      m.putUnsafe(_3, mOmit);
    }
    if (mRetain != null) {
      m.putUnsafe(_4, mRetain);
    }
    m.putUnsafe(_5, mWidgets);
    return m;
  }

  @Override
  public Script build() {
    return this;
  }

  @Override
  public Script parse(Object obj) {
    return new Script((JSMap) obj);
  }

  private Script(JSMap m) {
    {
      String x = m.opt(_0, "");
      mUsage = x.isEmpty() ? ScriptUsage.DEFAULT_INSTANCE : ScriptUsage.valueOf(x.toUpperCase());
    }
    mItems = DataUtil.parseListOfObjects(ScriptElement.DEFAULT_INSTANCE, m.optJSList(_1), false);
    {
      Object x = m.optUnsafe(_2);
      if (x != null) {
        mMetadata = ScriptMetadata.DEFAULT_INSTANCE.parse(x);
      }
    }
    mOmit = m.opt(_3, (Boolean) null);
    mRetain = m.opt(_4, (Boolean) null);
    {
      mWidgets = JSMap.DEFAULT_INSTANCE;
      JSMap x = m.optJSMap(_5);
      if (x != null) {
        mWidgets = x.lock();
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
    if (object == null || !(object instanceof Script))
      return false;
    Script other = (Script) object;
    if (other.hashCode() != hashCode())
      return false;
    if (!(mUsage.equals(other.mUsage)))
      return false;
    if (!(mItems.equals(other.mItems)))
      return false;
    if ((mMetadata == null) ^ (other.mMetadata == null))
      return false;
    if (mMetadata != null) {
      if (!(mMetadata.equals(other.mMetadata)))
        return false;
    }
    if ((mOmit == null) ^ (other.mOmit == null))
      return false;
    if (mOmit != null) {
      if (!(mOmit.equals(other.mOmit)))
        return false;
    }
    if ((mRetain == null) ^ (other.mRetain == null))
      return false;
    if (mRetain != null) {
      if (!(mRetain.equals(other.mRetain)))
        return false;
    }
    if (!(mWidgets.equals(other.mWidgets)))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      r = r * 37 + mUsage.ordinal();
      for (ScriptElement x : mItems)
        if (x != null)
          r = r * 37 + x.hashCode();
      if (mMetadata != null) {
        r = r * 37 + mMetadata.hashCode();
      }
      if (mOmit != null) {
        r = r * 37 + (mOmit ? 1 : 0);
      }
      if (mRetain != null) {
        r = r * 37 + (mRetain ? 1 : 0);
      }
      r = r * 37 + mWidgets.hashCode();
      m__hashcode = r;
    }
    return r;
  }

  protected ScriptUsage mUsage;
  protected List<ScriptElement> mItems;
  protected ScriptMetadata mMetadata;
  protected Boolean mOmit;
  protected Boolean mRetain;
  protected JSMap mWidgets;
  protected int m__hashcode;

  public static final class Builder extends Script {

    private Builder(Script m) {
      mUsage = m.mUsage;
      mItems = DataUtil.immutableCopyOf(m.mItems) /*DEBUG*/ ;
      mMetadata = m.mMetadata;
      mOmit = m.mOmit;
      mRetain = m.mRetain;
      mWidgets = m.mWidgets;
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
    public Script build() {
      Script r = new Script();
      r.mUsage = mUsage;
      r.mItems = mItems;
      r.mMetadata = mMetadata;
      r.mOmit = mOmit;
      r.mRetain = mRetain;
      r.mWidgets = mWidgets;
      return r;
    }

    public Builder usage(ScriptUsage x) {
      mUsage = (x == null) ? ScriptUsage.DEFAULT_INSTANCE : x;
      return this;
    }

    public Builder items(List<ScriptElement> x) {
      mItems = DataUtil.immutableCopyOf((x == null) ? DataUtil.emptyList() : x) /*DEBUG*/ ;
      return this;
    }

    public Builder metadata(ScriptMetadata x) {
      mMetadata = (x == null) ? null : x.build();
      return this;
    }

    public Builder omit(Boolean x) {
      mOmit = x;
      return this;
    }

    public Builder retain(Boolean x) {
      mRetain = x;
      return this;
    }

    public Builder widgets(JSMap x) {
      mWidgets = (x == null) ? JSMap.DEFAULT_INSTANCE : x;
      return this;
    }

  }

  public static final Script DEFAULT_INSTANCE = new Script();

  private Script() {
    mUsage = ScriptUsage.DEFAULT_INSTANCE;
    mItems = DataUtil.emptyList();
    mWidgets = JSMap.DEFAULT_INSTANCE;
  }

}
