package js.graphics.gen;

import js.data.AbstractData;
import js.json.JSMap;

public class ScriptFileEntry implements AbstractData {

  public String imageName() {
    return mImageName;
  }

  public String scriptName() {
    return mScriptName;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "image_name";
  protected static final String _1 = "script_name";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    m.putUnsafe(_0, mImageName);
    m.putUnsafe(_1, mScriptName);
    return m;
  }

  @Override
  public ScriptFileEntry build() {
    return this;
  }

  @Override
  public ScriptFileEntry parse(Object obj) {
    return new ScriptFileEntry((JSMap) obj);
  }

  private ScriptFileEntry(JSMap m) {
    mImageName = m.opt(_0, "");
    mScriptName = m.opt(_1, "");
  }

  public static Builder newBuilder() {
    return new Builder(DEFAULT_INSTANCE);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object)
      return true;
    if (object == null || !(object instanceof ScriptFileEntry))
      return false;
    ScriptFileEntry other = (ScriptFileEntry) object;
    if (other.hashCode() != hashCode())
      return false;
    if (!(mImageName.equals(other.mImageName)))
      return false;
    if (!(mScriptName.equals(other.mScriptName)))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      r = r * 37 + mImageName.hashCode();
      r = r * 37 + mScriptName.hashCode();
      m__hashcode = r;
    }
    return r;
  }

  protected String mImageName;
  protected String mScriptName;
  protected int m__hashcode;

  public static final class Builder extends ScriptFileEntry {

    private Builder(ScriptFileEntry m) {
      mImageName = m.mImageName;
      mScriptName = m.mScriptName;
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
    public ScriptFileEntry build() {
      ScriptFileEntry r = new ScriptFileEntry();
      r.mImageName = mImageName;
      r.mScriptName = mScriptName;
      return r;
    }

    public Builder imageName(String x) {
      mImageName = (x == null) ? "" : x;
      return this;
    }

    public Builder scriptName(String x) {
      mScriptName = (x == null) ? "" : x;
      return this;
    }

  }

  public static final ScriptFileEntry DEFAULT_INSTANCE = new ScriptFileEntry();

  private ScriptFileEntry() {
    mImageName = "";
    mScriptName = "";
  }

}
