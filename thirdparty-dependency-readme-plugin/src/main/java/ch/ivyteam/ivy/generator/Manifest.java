package ch.ivyteam.ivy.generator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;

public class Manifest
{
  /** Pattern to extract the Implementation-Title part of a manifest file */
  private static final Pattern JAR_IMPL_NAME_PATTERN =  Pattern.compile("Implementation-Title:(.*)");
  /** Pattern to extract the Implementation-Version part of a manifest file */
  private static final Pattern JAR_IMPL_VERSION_PATTERN =  Pattern.compile("Implementation-Version:(.*)");
  /** Pattern to extract the Implementation-Vendor part of a manifest file */
  private static final Pattern JAR_IMPL_VENDOR_PATTERN =  Pattern.compile("Implementation-Vendor:(.*)");
  /** Pattern to extract the Specification-Title part of a manifest file */
  private static final Pattern JAR_SPEC_NAME_PATTERN =  Pattern.compile("Specification-Title:(.*)");
  /** Pattern to extract the Implementation-Version part of a manifest file */
  private static final Pattern JAR_SPEC_VERSION_PATTERN =  Pattern.compile("Specification-Version:(.*)");
  /** Pattern to extract the Implementation-Version part of a manifest file */
  private static final Pattern JAR_SPEC_VENDOR_PATTERN =  Pattern.compile("Specification-Vendor:(.*)");
  /** Pattern to extract the Bundle-Vendor part of a manifest file */
  private static final Pattern BUNDLE_VENDOR_PATTERN =  Pattern.compile("Bundle-Vendor:(.*)");
  
  private final String manifestContent;
  
  public static Manifest forZip(ZipFile jarZipFile) throws IOException
  {
    ZipEntry metafile = jarZipFile.getEntry("META-INF/MANIFEST.MF");
    if (metafile == null)
    {
      return new Manifest("");
    }
    try (InputStream is = jarZipFile.getInputStream(metafile))
    {
      return new Manifest(IOUtils.toString(is));
    }
  }
  
  public static Manifest forJar(File jarFile) throws IOException
  {
    try (ZipFile jarZipFile = new ZipFile(jarFile))
    {
      return forZip(jarZipFile);
    }
  }
  
  public Manifest(String manifestContent)
  {
    this.manifestContent = manifestContent;
  }
  
  public String getName()
  {
    Matcher matcher = JAR_IMPL_NAME_PATTERN.matcher(manifestContent);    
    if (matcher.find())
    {
      return matcher.group(1).trim();
    }
    else
    {
      matcher = JAR_SPEC_NAME_PATTERN.matcher(manifestContent);    
      if (matcher.find())
      {
        return matcher.group(1).trim();
      }
    }
    return null;
  }
  
  public String getVersion()
  {
    Matcher matcher = JAR_IMPL_VERSION_PATTERN.matcher(manifestContent);
    if (matcher.find())
    {
      return matcher.group(1).trim();
    }
    else
    {
      matcher = JAR_SPEC_VERSION_PATTERN.matcher(manifestContent);    
      if (matcher.find())
      {
        return matcher.group(1).trim();
      }
    }
    return null;
  }
  
  public String getVendor()
  {
    Matcher matcher = JAR_IMPL_VENDOR_PATTERN.matcher(manifestContent);
    if (matcher.find())
    {
      return matcher.group(1).trim();
    }
    else
    {
      matcher = JAR_SPEC_VENDOR_PATTERN.matcher(manifestContent);    
      if (matcher.find())
      {
        return matcher.group(1).trim();
      }      
    }
    return null;
  }
  
  public String getBundleVendor()
  {
    Matcher matcher = BUNDLE_VENDOR_PATTERN.matcher(manifestContent);    
    if (matcher.find())
    {
      return matcher.group(1);
    }
    return "";
  }
  
}