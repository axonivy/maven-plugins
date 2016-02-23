package ch.ivyteam.ivy.generator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;

class JarInfo
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

  private String name;
  private String version;
  private String vendor;

  public String getVendor()
  {
    return vendor;
  }
  
  public void setVendor(String vendor)
  {
    this.vendor = vendor;
  }
  
  public String getVersion()
  {
    return version;
  }
  
  public void setVersion(String version)
  {
    this.version = version;
  }

  public String getName()
  {
    return name;
  }
  
  public void setName(String name)
  {
    this.name = name;
  }
  
  public static JarInfo createFor(ZipFile plugin, ZipEntry jarEntry) throws IOException
  {
    try(InputStream jarInputStream = plugin.getInputStream(jarEntry))
    {
      try (ZipInputStream zis = new ZipInputStream(jarInputStream))
      {
        ZipEntry entry;
               
        entry = zis.getNextEntry();
        while (entry != null)
        {
          if (entry.getName().equals("META-INF/MANIFEST.MF"))
          {
            return createFor(plugin, jarEntry, zis);
          }
          entry = zis.getNextEntry();          
        }
      }
    }
    return null;
  }
  
  public static JarInfo createFor(File jarFile) throws IOException
  {
    String manifest = getManifest(jarFile);
    JarInfo info = fromManifest(manifest);
    info = MavenCentral.enhanceInfo(info, jarFile);
    return info;
  }

  public static String getManifest(ZipFile jarZipFile) throws IOException
  {
    ZipEntry metafile = jarZipFile.getEntry("META-INF/MANIFEST.MF");
    if (metafile == null)
    {
      return "";
    }
    try (InputStream is = jarZipFile.getInputStream(metafile))
    {
      return IOUtils.toString(is);
    }
  }

  private static JarInfo createFor(ZipFile plugin, ZipEntry jarEntry, ZipInputStream zis) throws IOException
  {
    String meta = IOUtils.toString(zis);
    JarInfo info  = fromManifest(meta);
    try (InputStream jarInputStream = plugin.getInputStream(jarEntry))
    {
      return MavenCentral.enhanceInfo(info, jarInputStream);
    }
  }
  
  private static String getManifest(File jarFile) throws IOException
  {
    try (ZipFile jarZipFile = new ZipFile(jarFile))
    {
      return getManifest(jarZipFile);
    }
  }
  
  private static JarInfo fromManifest(String manifest)
  {
    JarInfo info = new JarInfo();
    Matcher matcher;
    matcher = JAR_IMPL_NAME_PATTERN.matcher(manifest);    
    if (matcher.find())
    {
      info.setName(matcher.group(1).trim());
    }
    else
    {
      matcher = JAR_SPEC_NAME_PATTERN.matcher(manifest);    
      if (matcher.find())
      {
        info.setName(matcher.group(1).trim());
      }
    }
    matcher = JAR_IMPL_VERSION_PATTERN.matcher(manifest);
    if (matcher.find())
    {
      info.setVersion(matcher.group(1).trim());
    }
    else
    {
      matcher = JAR_SPEC_VERSION_PATTERN.matcher(manifest);    
      if (matcher.find())
      {
        info.setVersion(matcher.group(1).trim());
      }
    }
    matcher = JAR_IMPL_VENDOR_PATTERN.matcher(manifest);
    if (matcher.find())
    {
      info.setVendor(matcher.group(1).trim());
    }
    else
    {
      matcher = JAR_SPEC_VENDOR_PATTERN.matcher(manifest);    
      if (matcher.find())
      {
        info.setVendor(matcher.group(1).trim());
      }      
    }
    return info;
  }

}
