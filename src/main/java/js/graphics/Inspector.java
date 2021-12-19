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
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import js.graphics.gen.Script;

import js.base.BaseObject;
import js.data.AbstractData;
import js.file.Files;
import js.geometry.IPoint;
import js.json.JSList;
import js.json.JSMap;
import js.json.JSObject;
import js.graphics.gen.MonoImage;

/**
 * Generate images (with optional annotations) for development / debug purposes
 */
public final class Inspector extends BaseObject {

  public static final Inspector NULL_INSPECTOR = new Inspector();

  // ------------------------------------------------------------------
  // Initialization
  // ------------------------------------------------------------------

  /**
   * Return inspector, or the NULL_INSPECTOR if null
   */
  public static Inspector orNull(Inspector inspector) {
    return nullTo(inspector, NULL_INSPECTOR);
  }

  /**
   * Build an inspector to generate images within a directory. If directory is
   * null, returns NULL_INSPECTOR (an inspector that does nothing, by ignoring
   * most calls)
   */
  public static Inspector build(File directory) {
    if (Files.empty(directory))
      return NULL_INSPECTOR;
    return new Inspector(directory);
  }

  /**
   * Constructor for NULL_INSPECTOR
   */
  private Inspector() {
  }

  private Inspector(File directory) {
    mDirectory = directory;
    log("constructed Inspector:", INDENT, this);
  }

  public Inspector imageSize(IPoint size) {
    if (!used())
      return this;
    assertNoImageSizeYet();
    mImageSize = size;
    return this;
  }

  public Inspector maxSamples(int maxSamples) {
    if (!isNull())
      mMaxSamples = maxSamples;
    return this;
  }

  public Inspector channels(int channels) {
    if (!used())
      return this;
    mImageChannels = channels;
    return this;
  }

  public Inspector normalize() {
    assertNoItemSpecifiedYet();
    mNormalize = true;
    return this;
  }

  public Inspector sharpen() {
    assertNoItemSpecifiedYet();
    mSharpen = true;
    return this;
  }

  private boolean isNull() {
    return this == NULL_INSPECTOR;
  }

  @Override
  public JSMap toJson() {
    JSMap m = super.toJson();
    if (isNull())
      m.put("", "NULL_INSPECTOR");
    else {
      m.put("directory", mDirectory.toString());
    }
    return m;
  }

  // ------------------------------------------------------------------
  // Adding records
  // ------------------------------------------------------------------

  /**
   * Create a new inspection image for subsequent calls to work with, one with
   * an empty prefix
   */
  public Inspector create() {
    return create(null);
  }

  /**
   * Create a new inspection image for subsequent calls to work with.
   * 
   * The ImageSet that will contain this image might end up not being used
   * 
   * @param prefix
   *          an optional prefix; consecutive images that have unique prefixes
   *          will be stored in the same ImageSet
   */
  public Inspector create(String prefix) {
    if (!isNull()) {

      prefix = nullToEmpty(prefix);
      log("create image, prefix:", quote(prefix));

      flushCurrentItem();

      // If there's no active set, or the active set already contains this prefix, start a new one
      if (mActiveItemSet == null || mPrefixSet.contains(prefix)) {
        startNewSet();
      }
      mPrefixSet.add(prefix);

      // Add characters to the front of the prefix so its sorted order agrees with the order it was generated
      if (!prefix.isEmpty()) {
        prefix = Integer.toString(mPrefixSet.size()) + "_" + prefix;
      }

      mCurrentItemPrefix = prefix;
      log("prefix set:", mPrefixSet);

      // Discard ephemeral state 
      //
      mElements.clear();
      mBufferedImage = null;
      mJsonObject = null;
      mMonoImage = null;
      mImageFloats = null;
      mImageSize = null;
      mNormalize = false;
      mSharpen = false;
    }
    return this;
  }

  private void startNewSet() {
    log("start new ImageSet");
    mPrefixSet.clear();

    int sampleSlot = -1;
    int rval = random().nextInt(1 + sampleCount());
    if (rval < maxSamples())
      sampleSlot = rval;

    ItemSet newSet = new ItemSet(sampleSlot, mSampleCount);
    mActiveItemSet = newSet;
    mSampleCount++;

    if (newSet.isUsed()) {
      ItemSet oldSet = mSamples.put(newSet.sampleSlot(), newSet);
      if (oldSet != null)
        discard(oldSet);
    }
    log("sample count:", mSampleCount, INDENT, newSet);
  }

  private void discard(ItemSet imageSet) {
    log("discarding:", INDENT, imageSet);
    for (File file : imageSet.files())
      files().deleteFile(file);
  }

  /**
   * Returns true if current image is to be kept. Otherwise, no image will
   * actually be generated. For efficiency, client shouldn't bother doing
   * further work with it if false; and some calls may fail if image isn't used.
   */
  public boolean used() {
    if (isNull())
      return false;
    return mActiveItemSet != null && mActiveItemSet.isUsed();
  }

  /**
   * Flush current item (if one exists) to filesystem
   */
  private void flushCurrentItem() {
    String prefix = mCurrentItemPrefix;
    if (prefix == null)
      return;
    if (used()) {
      String baseName = String.format("%07d", mActiveItemSet.sampleNumber());
      if (!prefix.isEmpty())
        baseName = baseName + "_" + prefix;

      File filename = new File(directory(), baseName);

      // Handle the item differently if it is an image vs a json object
      if (mJsonObject != null) {
        File jsonFile = Files.setExtension(filename, Files.EXT_JSON);
        log("write:", jsonFile.getName());
        files().writePretty(jsonFile, mJsonObject);
        mActiveItemSet.addFile(jsonFile);
      } else {
        File imageFile;
        if (mMonoImage != null) {
          imageFile = Files.setExtension(filename, ImgUtil.RAX_EXT);
          log("write image:", imageFile.getName());
          files().write(ImgUtil.compressRAX(mMonoImage), imageFile);
        } else {
          BufferedImage img = bufferedImage();
          imageFile = Files.setExtension(filename, ImgUtil.PNG_EXT);
          log("write image:", imageFile.getName());
          ImgUtil.writeImage(files(), img, imageFile);
        }
        mActiveItemSet.addFile(imageFile);
        File scriptFile = ScriptUtil.scriptPathForImage(imageFile);
        ScriptUtil.writeIfUseful(files(), script(), scriptFile);
        if (scriptFile.exists())
          mActiveItemSet.addFile(scriptFile);
      }
    }
    mCurrentItemPrefix = null;
  }

  public Inspector image(float[] image) {
    if (used()) {
      assertNoItemSpecifiedYet();
      mImageFloats = image;
    }
    return this;
  }

  private void assertNoItemSpecifiedYet() {
    if (mImageFloats != null || mBufferedImage != null || mJsonObject != null || mMonoImage != null)
      badState("Inspector item already specified");
  }

  public Inspector image(BufferedImage image) {
    if (used()) {
      assertNoItemSpecifiedYet();
      mBufferedImage = applyImageEffects(ImgUtil.deepCopy(image));
    }
    return this;
  }

  public Inspector json(JSObject jsonObject) {
    if (used()) {
      assertNoItemSpecifiedYet();
      mJsonObject = jsonObject;
    }
    return this;
  }

  public Inspector image(MonoImage monoImage) {
    if (used()) {
      assertNoItemSpecifiedYet();
      mMonoImage = monoImage;
      mImageSize = mMonoImage.size();
    }
    return this;
  }

  public Inspector abstractData(AbstractData abstractData) {
    if (used()) {
      json(abstractData.toJson());
    }
    return this;
  }

  public IPoint imageSize() {
    assertUsed();
    if (mImageSize == null)
      badState("no image size defined");
    return mImageSize;
  }

  private void assertNoImageSizeYet() {
    if (mImageSize != null)
      badState("image size is already defined");
  }

  public List<ScriptElement> elements() {
    assertValidInspector();
    return mElements;
  }

  public Inspector elements(Iterable<ScriptElement> elements) {
    if (used()) {
      mElements.clear();
      for (ScriptElement e : elements)
        mElements.add(e);
    }
    return this;
  }

  public BufferedImage bufferedImage() {
    assertValidInspector();
    if (mBufferedImage == null) {
      if (mImageFloats != null) {
        // Construct an 8-bit, RGB image from image floats
        mBufferedImage = ImgUtil.floatsToBufferedImage(mImageFloats, imageSize(), mImageChannels);
      }
      if (mBufferedImage == null)
        throw badState("no BufferedImage available");
      mBufferedImage = applyImageEffects(mBufferedImage);
    }
    return mBufferedImage;
  }

  private BufferedImage applyImageEffects(BufferedImage sourceImage) {
    BufferedImage img = sourceImage;
    if (mNormalize) {
      if (img.getType() == BufferedImage.TYPE_USHORT_GRAY) {
        MonoImage monoImage = MonoImageUtil.construct(img);
        monoImage = MonoImageUtil.normalizedImageMagick(monoImage, null);
        img = MonoImageUtil.to8BitRGBBufferedImage(monoImage);
      } else
        alert("Unsupported normalizing for image type:", img.getType());
    }

    if (mSharpen)
      img = ImgEffects.sharpen(img);
    return img;
  }

  private void assertValidInspector() {
    if (isNull())
      badState("Illegal method call for NULL_INSPECTOR");
  }

  private void assertUsed() {
    if (!used())
      badState("Illegal method call for inactive image");
  }

  private int sampleCount() {
    return mSampleCount;
  }

  private int maxSamples() {
    return mMaxSamples;
  }

  private File directory() {
    if (mPreparedDirectory == null)
      prepareDirectory();
    return mPreparedDirectory;
  }

  private void prepareDirectory() {
    File dir = mDirectory;
    if (dir == null)
      dir = Files.getDesktopFile("_SKIP_inspection");
    checkArgument(dir.getName().endsWith("inspection"),
        "For safetly, inspection directory must end with 'inspection'");
    files().rebuild(dir, "scredit_project.txt");
    mPreparedDirectory = dir;
  }

  private Files files() {
    return Files.S;
  }

  private Random random() {
    if (mRandom == null)
      mRandom = new Random(System.currentTimeMillis());
    return mRandom;
  }

  private Script script() {
    Script.Builder script = Script.newBuilder();
    script.items(mElements);
    return script.build();
  }

  /**
   * A set of items comprising a single sample
   * 
   * (an ItemSet may be 'unused', which means this sample is not going to be
   * saved)
   */
  private static class ItemSet extends BaseObject {

    public ItemSet(int slot, int sampleNumber) {
      mSampleSlot = slot;
      mSampleNumber = sampleNumber;
    }

    public int sampleSlot() {
      return mSampleSlot;
    }

    public int sampleNumber() {
      return mSampleNumber;
    }

    boolean isUsed() {
      return sampleSlot() >= 0;
    }

    @Override
    public JSMap toJson() {
      JSMap m = super.toJson();
      m.put("slot", sampleSlot());
      m.put("sample_number", sampleNumber());
      m.put("used", isUsed());
      m.put("files", JSList.withStringRepresentationsOf(files()));
      return m;
    }

    public void addFile(File file) {
      mFiles.add(file);
    }

    public List<File> files() {
      return mFiles;
    }

    private final int mSampleSlot;
    private int mSampleNumber;
    private List<File> mFiles = arrayList();
  }

  private File mDirectory;
  private File mPreparedDirectory;
  private int mSampleCount;
  private Random mRandom;

  private int mImageChannels = 1;
  private float[] mImageFloats;
  private IPoint mImageSize;
  private boolean mNormalize;
  private boolean mSharpen;
  private List<ScriptElement> mElements = arrayList();
  private BufferedImage mBufferedImage;
  private JSObject mJsonObject;
  private MonoImage mMonoImage;
  private Set<String> mPrefixSet = hashSet();
  private Map<Integer, ItemSet> mSamples = hashMap();
  private ItemSet mActiveItemSet;
  // If not null, this is the prefix for the active image
  private String mCurrentItemPrefix;
  private int mMaxSamples = 20;
}
