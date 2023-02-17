package ch.ivyteam.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.logging.Log;

/**
 * Updates the version of the bundle and the required bundles
 */
public class BundleManifestFileUpdater {

  private static final String REQUIRED_BUNDLE_VERSION = ";bundle-version=\"";
  private static final String REQUIRE_BUNDLE = "Require-Bundle";
  private static final String BUNDLE_VERSION = "Bundle-Version";
  public static final String MANIFEST_MF = "META-INF/MANIFEST.MF";
  private File manifestFile;
  private String bundleVersion;
  private String requiredBundleVersion;
  private Manifest manifest;
  private File projectDirectory;
  private String manifestContent;
  private final UpdateRun update;
  private final boolean stripBundleVersionOfDependencies;

  public BundleManifestFileUpdater(File projectDirectory, String newVersion, Log log,
          List<String> externalBuiltArtifacts, boolean stripBundleVersionOfDependencies) {
    this.projectDirectory = projectDirectory;
    this.stripBundleVersionOfDependencies = stripBundleVersionOfDependencies;
    this.manifestFile = new File(projectDirectory, MANIFEST_MF);
    update = new UpdateRun(manifestFile.getName(), newVersion, log, externalBuiltArtifacts);
    bundleVersion = update.versionEclipseQualified();
    requiredBundleVersion = update.versionNoMavenQualifier();
  }

  public void update() throws IOException {
    if (!manifestFile.exists()) {
      update.log.debug("No manifest file found in project " + projectDirectory + ". Nothing to do");
      return;
    }
    readManifest();
    boolean updated = false;
    updated = updateBundleVersion(updated);
    updated = updateRequiredBundleVersions(updated);
    if (updated) {
      saveManifest();
    } else {
      update.log.info("Manifest file " + manifestFile + " is up to date. Nothing to do.");
    }
  }

  private boolean updateBundleVersion(boolean updated) {
    String oldVersion = manifest.getMainAttributes().getValue(BUNDLE_VERSION);
    if (bundleVersionNeedsUpdate(oldVersion)) {
      update.log.info("Replace plugin version " + oldVersion + " with version " + bundleVersion
              + " in manifest file " + manifestFile.getAbsolutePath());
      updateBundleVersion(oldVersion);
      return true;
    }
    return updated;
  }

  private boolean updateRequiredBundleVersions(boolean updated) {
    String requiredBundle = manifest.getMainAttributes().getValue(REQUIRE_BUNDLE);
    if (requiredBundle != null) {
      List<String> requiredBundles = splitIntoBundles(requiredBundle);
      for (String bundle : requiredBundles) {
        if (requiredBundleVersionNeedsUpdate(bundle)) {
          updateRequiredBundleVersion(bundle);
          updated = true;
        }
      }
    }
    return updated;
  }

  static List<String> splitIntoBundles(String requiredBundle) {
    List<String> bundles = new ArrayList<>();
    Matcher matcher = Pattern.compile(",[a-zA-Z]+[a-zA-Z0-9\r\n]+\\.").matcher(requiredBundle);
    int lastCut = 0;
    while (matcher.find()) {
      int newCut = matcher.start();
      String bundle = requiredBundle.substring(lastCut, newCut);
      bundles.add(bundle);
      lastCut = newCut + 1;
    }
    String lastBundle = requiredBundle.substring(lastCut);
    bundles.add(lastBundle);
    return bundles;
  }

  private void updateRequiredBundleVersion(String oldRequiredBundleSpecification) {
    StringBuilder builder = new StringBuilder();
    builder.append(StringUtils.substringBefore(manifestContent, REQUIRE_BUNDLE));
    builder.append(REQUIRE_BUNDLE + ": ");
    String requireBundle = StringUtils.substringAfter(manifestContent, REQUIRE_BUNDLE + ": ");
    String requireBundleOnly = requireBundle;
    Matcher nextAttrMatcher = Pattern.compile("\n[A-Z]").matcher(requireBundle);
    if (nextAttrMatcher.find()) {
      requireBundleOnly = requireBundle.substring(0, nextAttrMatcher.start());
    }
    String end = requireBundle.substring(requireBundleOnly.length());
    requireBundleOnly = StringUtils.deleteWhitespace(requireBundleOnly); // remove
                                                                         // line
                                                                         // breaks
    String newRequiredBundleSpecification = updateRequiredBundleSpecifiction(oldRequiredBundleSpecification);
    requireBundleOnly = StringUtils.replaceOnce(requireBundleOnly, oldRequiredBundleSpecification,
            newRequiredBundleSpecification);
    update.log.info("Replace required plugin specification " + oldRequiredBundleSpecification + " with "
            + newRequiredBundleSpecification + " in manifest file " + manifestFile.getAbsolutePath());
    requireBundleOnly = StringUtils.join(splitIntoBundles(requireBundleOnly), ",\r\n ");
    builder.append(requireBundleOnly);
    if (end.isEmpty()) {
      builder.append("\r\n");
    } else {
      builder.append(end);
    }
    manifestContent = builder.toString();
  }

  private String updateRequiredBundleSpecifiction(String oldRequiredBundleSpecification) {
    String version = getRequiredBundleVersion(oldRequiredBundleSpecification);
    if (version == null) {
      return oldRequiredBundleSpecification + REQUIRED_BUNDLE_VERSION + requiredBundleVersion + "\"";
    }
    if (stripBundleVersionOfDependencies) {
      return StringUtils.substringBefore(oldRequiredBundleSpecification, REQUIRED_BUNDLE_VERSION);
    }
    return StringUtils.replace(oldRequiredBundleSpecification, version, requiredBundleVersion);
  }

  private boolean requiredBundleVersionNeedsUpdate(String oldRequiredBundleSpecification) {
    String bundle = StringUtils.substringBefore(oldRequiredBundleSpecification, ";");
    if (update.isLocalBuiltArtifact(bundle)) {
      String oldVersion = getRequiredBundleVersion(oldRequiredBundleSpecification);
      if (oldVersion == null) {
        return false;
      }
      if (stripBundleVersionOfDependencies) {
        return true;
      }
      if (isRange(oldVersion)) {
        return false;
      }
      return !requiredBundleVersion.equals(oldVersion);
    }
    return false;
  }

  private static boolean isRange(String version) {
    return StringUtils.contains(version, ",");
  }

  private String getRequiredBundleVersion(String oldRequiredBundleSpecification) {
    if (!oldRequiredBundleSpecification.contains(REQUIRED_BUNDLE_VERSION)) {
      return null;
    }
    String version = StringUtils.substringAfter(oldRequiredBundleSpecification, REQUIRED_BUNDLE_VERSION);
    version = StringUtils.substringBefore(version, "\"");
    return version;
  }

  private boolean bundleVersionNeedsUpdate(String oldVersion) {
    if (StringUtils.isBlank(oldVersion)) {
      return false;
    }
    return !oldVersion.trim().equals(bundleVersion.trim());
  }

  private void updateBundleVersion(String oldVersion) {
    manifestContent = manifestContent.replaceFirst(
            Pattern.quote(BUNDLE_VERSION + ":") + "\\s" + Pattern.quote(oldVersion),
            BUNDLE_VERSION + ": " + bundleVersion);
  }

  private void readManifest() throws FileNotFoundException, IOException {
    try (var fis = new FileInputStream(manifestFile)) {
      manifest = new Manifest(fis);
    }
    manifestContent = Files.readString(manifestFile.toPath());
  }

  private void saveManifest() throws IOException {
    Files.writeString(manifestFile.toPath(), manifestContent);
  }
}
