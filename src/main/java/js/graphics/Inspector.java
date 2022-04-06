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

import java.awt.Color;
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
import js.geometry.Polygon;
import js.json.JSList;
import js.json.JSMap;
import js.json.JSObject;
import js.graphics.gen.ElementProperties;
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

  public Inspector withBackup(boolean f) {
    checkState(mPreparedDirectory == null, "directory already prepared");
    mWithBackup = f;
    return this;
  }

  public Inspector seed(long randomSeed) {
    if (!isNull()) {
      assertNotStarted();
      mRandomSeed = randomSeed;
    }
    return this;
  }

  public Inspector imageSize(IPoint size) {
    if (!used())
      return this;
    checkState(mImageSize == null, "image size is already defined");
    mImageSize = size;
    return this;
  }

  public Inspector maxSamples(int maxSamples) {
    if (!isNull()) {
      assertNotStarted();
      mMaxSamples = maxSamples;
      int offset = 0;
      if (maxSamples >= 30) {
        // Assume we will only want to generate 1 of every 50 population elements
        offset = 50 * maxSamples;
      }
      mStartSampleOffset = offset;
    }
    return this;
  }

  /**
   * Set a minimum number of samples. Useful e.g. to always generate samples,
   * for debug purposes.
   */
  public Inspector minSamples(int minSamples) {
    if (!isNull()) {
      assertNotStarted();
      mMinSamples = minSamples;
      if (mMaxSamples < minSamples)
        maxSamples(minSamples);
    }
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

  public boolean isNull() {
    return this == NULL_INSPECTOR;
  }

  private void assertNotStarted() {
    if (mSampleCount > 0)
      throw badState("Inspector samples have already started");
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

      flush();

      // If there's no active set, or the active set already contains this prefix, start a new one
      if (mActiveSample == null || mPrefixSet.contains(prefix)) {
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
      mPlotter = null;
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

    int sampleSlot;
    if (mSampleCount < mMinSamples) {
      sampleSlot = mSampleCount;
    } else {
      sampleSlot = -1;
      int rval = random().nextInt(1 + mStartSampleOffset + sampleCount());
      if (rval < maxSamples())
        sampleSlot = rval;
    }

    Sample newSet = new Sample(sampleSlot, mSampleCount);
    mActiveSample = newSet;
    mSampleCount++;

    if (newSet.isUsed()) {
      Sample oldSet = mSamples.put(newSet.sampleSlot(), newSet);
      if (oldSet != null)
        discard(oldSet);
    }
    log("sample count:", mSampleCount, INDENT, newSet);
  }

  private void discard(Sample imageSet) {
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
    return mActiveSample != null && mActiveSample.isUsed();
  }

  /**
   * Flush current item (if one exists) to filesystem
   */
  public void flush() {
    String prefix = mCurrentItemPrefix;
    mCurrentItemPrefix = null;
    if (prefix != null && used()) {
      String baseName = String.format("%07d", mActiveSample.sampleNumber());
      if (!prefix.isEmpty())
        baseName = baseName + "_" + prefix;

      File filename = new File(directory(), baseName);

      // Handle the item differently if it is an image vs a json object
      if (mJsonObject != null) {
        File jsonFile = Files.setExtension(filename, Files.EXT_JSON);
        log("write:", jsonFile.getName());
        files().writePretty(jsonFile, mJsonObject);
        mActiveSample.addFile(jsonFile);
      } else {
        File imageFile;
        if (mMonoImage != null) {
          imageFile = Files.setExtension(filename, ImgUtil.EXT_RAX);
          log("write image:", imageFile.getName());
          files().write(ImgUtil.compressRAX(mMonoImage), imageFile);
        } else {
          imageFile = Files.setExtension(filename, ImgUtil.EXT_PNG);
          // There may not be an image, e.g. if just adding script elements
          BufferedImage img = optBufferedImage();
          if (img != null) {
            log("write image:", imageFile.getName());
            ImgUtil.writeImage(files(), img, imageFile);
          }
        }
        mActiveSample.addFile(imageFile);
        File scriptFile = ScriptUtil.scriptPathForImage(imageFile);
        if (ScriptUtil.writeIfUseful(files(), script(), scriptFile)) {
          mActiveSample.addFile(scriptFile);
        }
      }
    }
  }

  // ------------------------------------------------------------------
  // Defining and working with images
  // ------------------------------------------------------------------

  public Plotter plotter() {
    if (mPlotter == null) {
      assertUsed();
      Plotter p = Plotter.build();
      // Provide an image to the plotter.  If none exists, create one
      boolean createdImage = false;
      if (optBufferedImage() == null) {
        mBufferedImage = ImgUtil.build(imageSize(), Plotter.PREFERRED_IMAGE_TYPE);
        createdImage = true;
      }
      p.into(optBufferedImage());
      if (createdImage)
        p.with(Color.lightGray).fillRect(0, 0, mImageSize.x, mImageSize.y);
      mPlotter = p;
    }
    return mPlotter;
  }
  // ------------------------------------------------------------------

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

  @Deprecated
  public Inspector add(Polygon polygon, int category) {
    if (used()) {
      mElements.add(new PolygonElement(ElementProperties.newBuilder().category(category), polygon));
    }
    return this;
  }

  public BufferedImage bufferedImage() {
    optBufferedImage();
    if (mBufferedImage == null)
      throw badState("no BufferedImage available");
    return mBufferedImage;
  }

  private BufferedImage optBufferedImage() {
    assertValidInspector();
    if (mBufferedImage == null) {
      if (mImageFloats != null) {
        // Construct an 8-bit, RGB image from image floats
        mBufferedImage = ImgUtil.floatsToBufferedImage(mImageFloats, imageSize(), mImageChannels);
      }
      if (mBufferedImage != null)
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
        "For safety, inspection directory must end with 'inspection'");
    if (mWithBackup)
      files().backupAndRemake(dir);
    else {
      files().deleteDirectory(dir);
      files().mkdirs(dir);
    }
    mPreparedDirectory = dir;
  }

  private Files files() {
    return Files.S;
  }

  private Random random() {
    if (mRandom == null) {
      long randomSeed = mRandomSeed;
      if (randomSeed <= 0)
        randomSeed = System.currentTimeMillis();
      mRandom = new Random(randomSeed);
    }
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
   * (it may be 'unused', which means this sample is not going to be saved)
   */
  private static class Sample extends BaseObject {

    public Sample(int slot, int sampleNumber) {
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
    private final int mSampleNumber;
    private final List<File> mFiles = arrayList();
  }

  private File mDirectory;
  private File mPreparedDirectory;
  private Random mRandom;
  private long mRandomSeed;
  private int mSampleCount;

  // To avoid generating a flurry of samples initially when the desired sample count is large,
  // we act as if we have already generated a number of samples by setting this offset to a value > 0
  private int mStartSampleOffset;

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
  private Map<Integer, Sample> mSamples = hashMap();
  private Sample mActiveSample;
  // If not null, this is the prefix for the active image
  private String mCurrentItemPrefix;
  private int mMaxSamples = 20;
  private int mMinSamples;
  private boolean mWithBackup;
  private Plotter mPlotter;
}
