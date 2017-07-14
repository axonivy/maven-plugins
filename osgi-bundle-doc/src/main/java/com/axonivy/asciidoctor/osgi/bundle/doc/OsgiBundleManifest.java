package com.axonivy.asciidoctor.osgi.bundle.doc;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Manifest;

public class OsgiBundleManifest
{
  private Manifest manifest;

  public OsgiBundleManifest(Manifest manifest)
  {
    this.manifest = manifest;
  }

  public String getName()
  {
    return getAttributeValue("Bundle-Name");
  }

  private String getAttributeValue(String attributeName)
  {
    return manifest.getMainAttributes().getValue(attributeName);
  }

  public String getSymbolicName()
  {
    String symbolicName = getAttributeValue("Bundle-SymbolicName");
    if (symbolicName.contains(";"))
    {
      symbolicName = symbolicName.substring(0, symbolicName.indexOf(';'));
    }
    return symbolicName;
  }
    

  public String getVersion()
  {
    return getAttributeValue("Bundle-Version");
  }

  public String getVendor()
  {
    return getAttributeValue("Bundle-Vendor");
  }
    
  public boolean isTest()
  {
    return getSymbolicName().endsWith(".tests");
  }

  public List<String> getPublicExportedPackages()
  {
    List<String> packages = new ArrayList<>();
    String exportPackage = getAttributeValue("Export-Package");
    for (String pack : splitEntries(exportPackage))
    {
      if (pack.contains("x-friends"))
      {
        // non public
        continue;
      }
      if (pack.contains(";"))
      {
        pack = pack.substring(0, pack.indexOf(";"));
      }
      packages.add(pack);
    }
    return packages;
  }

  private List<String> splitEntries(String value)
  {
    List<String> entries = new ArrayList<>();
    int start = 0;
    boolean insideString=false;
    if (value == null)
    {
      return entries;
    }
    for (int index = 0; index < value.length(); index++)
    {
      if (value.charAt(index) == ',' && !insideString)
      {
        entries.add(value.substring(start, index));
        start = index+1;
      }
      if (value.charAt(index) == '"')
      {
        if (insideString)
        {
          insideString=false;
        }
        else
        {
          insideString=true;
        }
      }
    }
    entries.add(value.substring(start, value.length()));
    return entries;
  }

  public List<String> getRequireBundles()
  {
    List<String> bundles = new ArrayList<>();
    String requireBundle = getAttributeValue("Require-Bundle");
    for (String bundle : splitEntries(requireBundle))
    {
      if (bundle.contains(";"))
      {
        bundle = bundle.substring(0, bundle.indexOf(";"));
      }
      bundles.add(bundle);
    }
    return bundles;
  }

  public static OsgiBundleManifest read(File bundleDir)
  {
    File manifestFile = new File(bundleDir, "META-INF/MANIFEST.MF");
    if (manifestFile.exists())
    {
      return getManifest(manifestFile);
    }
    return null;
  }

  private static OsgiBundleManifest getManifest(File manifestFile)
  {
    Manifest manifest = new Manifest();
    try (FileInputStream fis = new FileInputStream(manifestFile))
    {
      manifest.read(fis);
      return new OsgiBundleManifest(manifest);
    }
    catch (Exception ex)
    {
      throw new RuntimeException(ex);
    }
  }
}
