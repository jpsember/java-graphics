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

import java.io.File;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import js.file.Files;
import js.geometry.IRect;
import js.geometry.Matrix;
import js.geometry.MyMath;
import js.geometry.Polygon;
import js.graphics.gen.ElementProperties;
import js.graphics.gen.Script;
import js.graphics.gen.ScriptElementList;
import js.graphics.gen.ScriptFileEntry;
import js.json.JSMap;

import static js.base.Tools.*;

/**
 * Utilities for working with ScriptData objects
 */
public final class ScriptUtil {

  // This is a global variable for now... hacky, I know
  public static boolean sAllowEmptyScripts;

  // ------------------------------------------------------------------
  // File utilities
  // ------------------------------------------------------------------

  private static final String SCRIPTS_SUBDIRECTORY = "annotations";
  public static final String SCRIPT_PROJECT_FILENAME = "scredit_project.txt";

  /**
   * Get annotation directory for a project directory
   */
  public static File scriptDirForProject(File projectDirectory) {
    return scriptDirForProject(projectDirectory, true);
  }

  /**
   * Get annotation directory for a project directory
   */
  public static File scriptDirForProject(File projectDirectory, boolean projectIncludesImages) {
    if (!projectIncludesImages)
      return projectDirectory;
    return new File(projectDirectory, SCRIPTS_SUBDIRECTORY);
  }

  /**
   * Get the script file corresponding to an image
   */
  private static File scriptPathForImage(File projectDirectory, String imageName) {
    return new File(scriptDirForProject(projectDirectory), Files.setExtension(imageName, Files.EXT_JSON));
  }

  /**
   * Get the script file corresponding to an image
   */
  public static File scriptPathForImage(File imageFile) {
    return scriptPathForImage(imageFile.getParentFile(), Files.basename(imageFile));
  }

  public static Script from(File scriptFile) {
    return validate(Files.parseAbstractDataOpt(Script.DEFAULT_INSTANCE, scriptFile));
  }

  public static Script from(JSMap jsonMap) {
    return validate(Files.parseAbstractDataOpt(Script.DEFAULT_INSTANCE, jsonMap));
  }

  private static Script validate(Script script) {
    if (false && alert("performing validation")) {
      Script.Builder b = script.build().toBuilder();
      List<ScriptElement> elems = arrayList();
      for (ScriptElement elem : script.items()) {
        if (elem.is(PolygonElement.DEFAULT_INSTANCE)) {
          PolygonElement poly = (PolygonElement) elem;
          Polygon p = poly.polygon();
          if (p.numVertices() < (p.isClosed() ? 3 : 2)) {
            pr("illformed polygon found in script:", INDENT, elem);
            continue;
          }

        }
        elems.add(elem);
      }
      b.items(elems);
      return b.build();
    }
    return script;
  }

  /**
   * Get an array of all script elements of a particular type
   */
  public static <T extends ScriptElement> List<T> elements(Script script, T elementType) {
    List<T> result = elements(script.items(), elementType);
    return result;
  }

  public static <T extends ScriptElement> List<T> elements(List<ScriptElement> elements, T elementType) {
    List<T> result = arrayList();
    for (ScriptElement elem : elements) {
      if (elem.is(elementType))
        result.add((T) elem);
    }
    return result;
  }

  /**
   * Get an array of all RectElements
   */
  public static List<RectElement> rectElements(Script script) {
    return elements(script, RectElement.DEFAULT_INSTANCE);
  }

  public static <T extends ScriptElement> int elementCount(List<ScriptElement> elements,
      ScriptElement elementType) {
    return elements(elements, elementType).size();
  }

  /**
   * Get an array of all PolygonElements
   */
  public static List<PolygonElement> polygonElements(Script script) {
    return elements(script, PolygonElement.DEFAULT_INSTANCE);
  }

  /**
   * Determine which image files correspond to this script (there should be at
   * most one)
   */
  public static List<File> findImagePathsForScript(File scriptFile) {
    List<File> result = arrayList();
    String annotationDir = scriptFile.getParent();
    String imageDir = chomp(annotationDir, "/" + SCRIPTS_SUBDIRECTORY);
    checkArgument(imageDir != annotationDir, scriptFile);
    String baseName = Files.basename(scriptFile);
    String imageBase = imageDir + "/" + baseName + ".";
    for (String ext : ImgUtil.IMAGE_EXTENSIONS) {
      File candidate = new File(imageBase + ext);
      if (candidate.exists())
        result.add(candidate);
    }
    return result;
  }

  // ------------------------------------------------------------------
  // Project utilities
  // ------------------------------------------------------------------

  private static String[] sFileExtImages = { ImgUtil.EXT_JPEG, "jpeg", ImgUtil.EXT_PNG, ImgUtil.EXT_RAX };
  private static String[] sFileExtAnnotation = { Files.EXT_JSON };

  @Deprecated
  public static List<ScriptFileEntry> buildScriptList(File projectDirectory) {
    return buildScriptList(projectDirectory, true);
  }

  public static List<ScriptFileEntry> buildScriptList(File projectDirectory, boolean includesImages) {

    Map<String, ScriptFileEntry> fileRootSet = hashMap();
    if (includesImages) {
      for (File imageFile : FileUtils.listFiles(projectDirectory, sFileExtImages, false)) {
        String key = Files.basename(imageFile);
        File scriptFile = ScriptUtil.scriptPathForImage(imageFile);
        ScriptFileEntry entry = ScriptFileEntry.newBuilder() //
            .imageName(imageFile.getName()) //
            .scriptName(scriptFile.getName()) //
            .build();
        ScriptFileEntry dup = fileRootSet.put(key, entry);
        if (dup != null)
          throw badState("Multiple images with name:", key);
      }
    }
    File scriptsDir = ScriptUtil.scriptDirForProject(projectDirectory, includesImages);

    if (scriptsDir.exists()) {
      for (File scriptFile : FileUtils.listFiles(scriptsDir, sFileExtAnnotation, false)) {
        String key = Files.basename(scriptFile);
        if (fileRootSet.containsKey(key))
          continue;
        fileRootSet.put(key, ScriptFileEntry.newBuilder() //
            .scriptName(key + "." + Files.EXT_JSON) //
            .build());
      }
    }

    // Sort the scripts by filename 
    List<ScriptFileEntry> entries = arrayList();
    entries.addAll(fileRootSet.values());
    entries.sort((a, b) -> a.scriptName().compareTo(b.scriptName()));
    return entries;
  }

  public static List<ScriptElement> transform(List<ScriptElement> elements, Matrix transform) {
    List<ScriptElement> result = arrayList();
    for (ScriptElement elem : elements)
      result.add(elem.applyTransform(transform));
    return result;
  }

  public static ScriptElementList transform(ScriptElementList elements, Matrix transform) {
    return ScriptElementList.newBuilder().elements(transform(elements.elements(), transform)).build();
  }

  /**
   * Construct a list of polygons representing BoxElements rotated according to
   * their rotation parameter
   */
  public static List<ScriptElement> constructRotatedBoxes(List<ScriptElement> elements) {
    List<ScriptElement> out = arrayList();
    for (RectElement rect : elements(elements, RectElement.DEFAULT_INSTANCE)) {
      IRect bounds = rect.bounds();
      Polygon poly = Polygon.with(bounds);
      int deg = ScriptUtil.rotationDegreesOrZero(rect);
      Matrix m1 = Matrix.getTranslate(bounds.midPoint().negate());
      Matrix m2 = Matrix.getRotate(MyMath.M_DEG * deg);
      Matrix m3 = Matrix.getTranslate(bounds.midPoint());
      Matrix mCombined = Matrix.preMultiply(m1, m2, m3);
      Polygon polyTransformed = poly.applyTransform(mCombined);
      PolygonElement polyElement = new PolygonElement(rect.properties(), polyTransformed);
      out.add(polyElement);
    }
    return out;
  }

  /**
   * Write a script to a file
   */
  public static void write(Files fileManager, Script scriptData, File destinationFile) {
    Script built = scriptData.build();
    fileManager.write(destinationFile, built);
  }

  /**
   * Write a script to a file, if it is useful; otherwise, delete any existing
   * file at that location. A script is useful if it is different than the
   * default instance
   */
  public static boolean writeIfUseful(Files fileManager, Script scriptData, File destinationFile) {
    Script built = scriptData.build();
    boolean useful = isUseful(built);
    if (useful) {
      fileManager.write(destinationFile, built);
    } else {
      fileManager.deleteFile(destinationFile);
    }
    return useful;
  }

  public static boolean isUseful(Script script) {
    return sAllowEmptyScripts || !script.equals(Script.DEFAULT_INSTANCE);
  }

  /**
   * Made private since we probably want categoryOrZero in most cases
   */
  private static int category(ScriptElement element) {
    Integer val = element.properties().category();
    return (val == null) ? -1 : val.intValue();
  }

  private static ElementProperties.Builder toBuilder(ElementProperties propertiesOrNull) {
    ElementProperties prop = nullTo(propertiesOrNull, ElementProperties.DEFAULT_INSTANCE);
    return prop.toBuilder();
  }

  public static ElementProperties.Builder setCategory(ElementProperties propertiesOrNull, int category) {
    return toBuilder(propertiesOrNull).category(category);
  }

  public static int categoryOrZero(ScriptElement element) {
    int c = category(element);
    return (c >= 0) ? c : 0;
  }

  public static int rotationDegrees(ScriptElement element) {
    Integer rot = element.properties().rotation();
    if (rot == null)
      throw badArg("element has no rotation:", element);
    return rot;
  }

  public static int rotationDegreesOrZero(ScriptElement element) {
    Integer rot = element.properties().rotation();
    return (rot == null) ? 0 : rot;
  }

  public static boolean hasRotation(ScriptElement element) {
    Integer rot = element.properties().rotation();
    return rot != null;
  }

  /**
   * Read confidence from element's properties; if missing, return -1
   */
  public static int confidence(ScriptElement element) {
    Integer val = element.properties().confidence();
    return (val == null) ? -1 : val.intValue();
  }

  public static boolean hasConfidence(ScriptElement element) {
    return element.properties().confidence() != null;
  }

  public static boolean hasCategory(ScriptElement element) {
    return element.properties().category() != null;
  }

  /**
   * Read anchor from element's properties; if missing, return -1
   * 
   * --- not yet implemented ---
   */
  public static int anchor(ScriptElement element) {
    todo("add an anchor property");
    return -1;
  }

  public static final int ROT_MIN = -90;
  public static final int ROT_MAX = 90;

  public static final String TAG_KEY = "";
  public static final String TAG_PROPERTIES = "p";

  public static void assertNoMixing(List<ScriptElement> elements) {
    int rect = 0;
    int poly = 0;
    for (ScriptElement elem : elements) {
      if (elem.is(RectElement.DEFAULT_INSTANCE))
        rect++;
      if (elem.is(PolygonElement.DEFAULT_INSTANCE))
        poly++;
    }
    if (rect != 0 && poly != 0)
      throw badArg("Attempt to mix rectangles and polygons");
  }

  public static ScriptElementList extractScriptElementList(Script script) {
    ScriptElementList.Builder b = ScriptElementList.newBuilder();
    todo("revert to old code to see if still a problem");
    // The new datagen classes fail here; I am not constructing a copy of this array!
    List<ScriptElement> arr = arrayList();
    arr.addAll(script.items());
    b.elements(arr);
    return b.build();
  }

}
