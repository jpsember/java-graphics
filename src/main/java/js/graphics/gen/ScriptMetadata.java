package js.graphics.gen;

import java.io.File;
import js.data.AbstractData;
import js.geometry.Matrix;
import js.json.JSMap;

public class ScriptMetadata implements AbstractData {

  public File sourceImageFile() {
    return mSourceImageFile;
  }

  public Matrix sourceTransform() {
    return mSourceTransform;
  }

  public JSMap userMap() {
    return mUserMap;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "source_image_file";
  protected static final String _1 = "source_transform";
  protected static final String _2 = "user_map";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    if (mSourceImageFile != null) {
      m.putUnsafe(_0, mSourceImageFile.toString());
    }
    if (mSourceTransform != null) {
      m.putUnsafe(_1, mSourceTransform.toJson());
    }
    if (mUserMap != null) {
      m.putUnsafe(_2, mUserMap);
    }
    return m;
  }

  @Override
  public ScriptMetadata build() {
    return this;
  }

  @Override
  public ScriptMetadata parse(Object obj) {
    return new ScriptMetadata((JSMap) obj);
  }

  private ScriptMetadata(JSMap m) {
    {
      String x = m.opt(_0, (String) null);
      if (x != null) {
        mSourceImageFile = new File(x);
      }
    }
    {
      Object x = m.optUnsafe(_1);
      if (x != null) {
        mSourceTransform = Matrix.DEFAULT_INSTANCE.parse(x);
      }
    }
    {
      JSMap x = m.optJSMap(_2);
      if (x != null) {
        mUserMap = x.lock();
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
    if (object == null || !(object instanceof ScriptMetadata))
      return false;
    ScriptMetadata other = (ScriptMetadata) object;
    if (other.hashCode() != hashCode())
      return false;
    if ((mSourceImageFile == null) ^ (other.mSourceImageFile == null))
      return false;
    if (mSourceImageFile != null) {
      if (!(mSourceImageFile.equals(other.mSourceImageFile)))
        return false;
    }
    if ((mSourceTransform == null) ^ (other.mSourceTransform == null))
      return false;
    if (mSourceTransform != null) {
      if (!(mSourceTransform.equals(other.mSourceTransform)))
        return false;
    }
    if ((mUserMap == null) ^ (other.mUserMap == null))
      return false;
    if (mUserMap != null) {
      if (!(mUserMap.equals(other.mUserMap)))
        return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      if (mSourceImageFile != null) {
        r = r * 37 + mSourceImageFile.hashCode();
      }
      if (mSourceTransform != null) {
        r = r * 37 + mSourceTransform.hashCode();
      }
      if (mUserMap != null) {
        r = r * 37 + mUserMap.hashCode();
      }
      m__hashcode = r;
    }
    return r;
  }

  protected File mSourceImageFile;
  protected Matrix mSourceTransform;
  protected JSMap mUserMap;
  protected int m__hashcode;

  public static final class Builder extends ScriptMetadata {

    private Builder(ScriptMetadata m) {
      mSourceImageFile = m.mSourceImageFile;
      mSourceTransform = m.mSourceTransform;
      mUserMap = m.mUserMap;
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
    public ScriptMetadata build() {
      ScriptMetadata r = new ScriptMetadata();
      r.mSourceImageFile = mSourceImageFile;
      r.mSourceTransform = mSourceTransform;
      r.mUserMap = mUserMap;
      return r;
    }

    public Builder sourceImageFile(File x) {
      mSourceImageFile = x;
      return this;
    }

    public Builder sourceTransform(Matrix x) {
      mSourceTransform = (x == null) ? null : x.build();
      return this;
    }

    public Builder userMap(JSMap x) {
      mUserMap = x;
      return this;
    }

  }

  public static final ScriptMetadata DEFAULT_INSTANCE = new ScriptMetadata();

  private ScriptMetadata() {
  }

}
