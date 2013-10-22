package ch.ivyteam.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.logging.Log;

/**
 * Updates the version of the bundle and the required bundles
 */
class BundleManifestFileUpdater
{
  private static final String REQUIRED_BUNDLE_VERSION = ";bundle-version=\"";
  private static final String REQUIRE_BUNDLE = "Require-Bundle";
  private static final String BUNDLE_VERSION = "Bundle-Version";
  private static final String MANIFEST_MF = "META-INF/MANIFEST.MF";
  private File manifestFile;
  private String bundleVersion;
  private String requiredBundleVersion;
  private Manifest manifest;
  private Log log;
  private File projectDirectory;
  private String manifestContent;

  BundleManifestFileUpdater(File projectDirectory, String newVersion, Log log)
  {
    this.projectDirectory = projectDirectory;
    this.manifestFile = new File(projectDirectory, MANIFEST_MF);

    this.log = log;
    if (newVersion.indexOf('-') >= 0)
    {
      bundleVersion = StringUtils.substringBefore(newVersion, "-");
    }
    else
    {
      bundleVersion = newVersion;
    }
  
    requiredBundleVersion = bundleVersion;    
    bundleVersion += ".qualifier";    
  }

  void update() throws IOException
  {
    if (manifestFile.exists())
    {
      readManifest();
      boolean updated = false;
      
      updated = updateBundleVersion(updated);
      updated = updateRequiredBundleVersions(updated);
      
      if (updated)
      {
        saveManifest();
      }
      else
      {
        log.info("Manifest file "+manifestFile+" is up to date. Nothing to do.");
      }
    }
    else
    {
      log.info("No manifest file found in project "+projectDirectory+". Nothing to do");
    }
  }

  private boolean updateBundleVersion(boolean updated)
  {
    String oldVersion = manifest.getMainAttributes().getValue(BUNDLE_VERSION);      
    if (bundleVersionNeedsUpdate(oldVersion))
    {
      log.info("Replace plugin version "+oldVersion+" with version "+bundleVersion+" in manifest file "+manifestFile.getAbsolutePath());        
      updateBundleVersion(oldVersion);
      return true;
    }
    return updated;
  }

  private boolean updateRequiredBundleVersions(boolean updated)
  {
    String requiredBundle = manifest.getMainAttributes().getValue(REQUIRE_BUNDLE);
    if (requiredBundle != null)
    {
      String[] requiredBundles = splitIntoBundles(requiredBundle);
      
      for (String bundle : requiredBundles)
      {
        if (requiredBundleVersionNeedsUpdate(bundle))
        {
          updateRequiredBundleVersion(bundle);
          updated = true;
        }
      }
    }
  
    return updated;
  }
  
  private String[] splitIntoBundles(String requiredBundle)
  {
    return requiredBundle.split(",");
  }
  
  private void updateRequiredBundleVersion(String oldRequiredBundleSpecification)
  {
    StringBuilder builder = new StringBuilder();
    builder.append(StringUtils.substringBefore(manifestContent, REQUIRE_BUNDLE));
    builder.append(REQUIRE_BUNDLE);
    String requireBundle = StringUtils.substringAfter(manifestContent, REQUIRE_BUNDLE);
    
    String newRequiredBundleSpecification = updateRequiredBundleSpecifiction(oldRequiredBundleSpecification);
    requireBundle = requireBundle.replaceFirst(Pattern.quote(oldRequiredBundleSpecification), newRequiredBundleSpecification);

    log.info("Replace required plugin specification "+oldRequiredBundleSpecification+" with "+newRequiredBundleSpecification+" in manifest file "+manifestFile.getAbsolutePath());        

    builder.append(requireBundle);
    manifestContent = builder.toString();
  }
  
  private String updateRequiredBundleSpecifiction(String oldRequiredBundleSpecification)
  {
    String version = getRequiredBundleVersion(oldRequiredBundleSpecification);
    if (version == null)
    {
      return oldRequiredBundleSpecification+REQUIRED_BUNDLE_VERSION+requiredBundleVersion+"\"";
    }
    return StringUtils.replace(oldRequiredBundleSpecification, version, requiredBundleVersion);
  }

  private boolean requiredBundleVersionNeedsUpdate(String oldRequiredBundleSpecification)
  {
    if (oldRequiredBundleSpecification.startsWith("ch.ivyteam."))
    {
      String oldVersion = getRequiredBundleVersion(oldRequiredBundleSpecification);          
      return !requiredBundleVersion.equals(oldVersion);
    }
    return false;
  }

  private String getRequiredBundleVersion(String oldRequiredBundleSpecification)
  {
    if (!oldRequiredBundleSpecification.contains(REQUIRED_BUNDLE_VERSION))
    {
      return null;
    }
    String version = StringUtils.substringAfter(oldRequiredBundleSpecification, REQUIRED_BUNDLE_VERSION);
    version = StringUtils.substringBefore(version, "\"");
    return version;
  }



  private boolean bundleVersionNeedsUpdate(String oldVersion)
  {
    return !oldVersion.trim().equals(bundleVersion.trim());
  }
  
  private void updateBundleVersion(String oldVersion)
  {
    manifestContent = manifestContent.replaceFirst(Pattern.quote(BUNDLE_VERSION+":")+"\\s"+Pattern.quote(oldVersion), BUNDLE_VERSION+": "+bundleVersion);
  }

  private void readManifest() throws FileNotFoundException, IOException
  {
    FileInputStream fis = new FileInputStream(manifestFile);
    try
    {
      manifest = new Manifest(fis);
    }
    finally
    {
      fis.close();
    }
    manifestContent = FileUtils.readFileToString(manifestFile);
  }
  
  private void saveManifest() throws IOException
  {
    FileUtils.writeStringToFile(manifestFile, manifestContent);
  }

}
