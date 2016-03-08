package ch.ivyteam.ivy.generator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;

class JarInfo
{
  private final String name;
  private final String version;
  private final String vendor;
  private final String sha1Hash;
  
  public JarInfo(String name, String version, String vendor, String sha1Hash)
  {
    this.name = name;
    this.version = version;
    this.vendor = vendor;
    this.sha1Hash = sha1Hash;
  }
  
  public static JarInfo from(Manifest manifest, String sha1Hash)
  {
    return new JarInfo(manifest.getName(), manifest.getVersion(), manifest.getVendor(), sha1Hash);
  }

  public String getVendor()
  {
    return vendor;
  }
  
  public String getVersion()
  {
    return version;
  }

  public String getName()
  {
    return name;
  }
  
  public boolean isComplete()
  {
    return name!=null && version!=null && vendor!=null;
  }
  
  public String getSha1Hash()
  {
    return sha1Hash;
  }
  
  public static JarInfo createFor(ZipFile plugin, ZipEntry jarEntry) throws IOException
  {
    try(InputStream jarInputStream = plugin.getInputStream(jarEntry))
    {
      try (ZipInputStream zis = new ZipInputStream(jarInputStream))
      {
        ZipEntry entry = zis.getNextEntry();
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
    Manifest manifest = Manifest.forJar(jarFile);
    return JarInfo.from(manifest, Sha1Hash.forJar(jarFile));
  }

  private static JarInfo createFor(ZipFile plugin, ZipEntry jarEntry, ZipInputStream zis) throws IOException
  {
    Manifest manifest = new Manifest(IOUtils.toString(zis));
    try (InputStream jarInputStream = plugin.getInputStream(jarEntry))
    {
      return JarInfo.from(manifest, Sha1Hash.forJar(jarInputStream));
    }
  }
  
  
  public static class Sha1Hash
  {
    public static String forJar(File jar) throws IOException
    {
      try(FileInputStream fis = new FileInputStream(jar))
      {
        return build(fis);
      }
    }
    
    public static String forJar(InputStream jarStream) throws IOException
    {
      return build(jarStream);
    }
    
    public static String build(InputStream inputStream) throws IOException
    {
      try
      {
        MessageDigest cript = MessageDigest.getInstance("SHA-1");
        cript.reset();
        int n = 0;
        byte[] buffer = new byte[8192];
        do 
        {
          n = inputStream.read(buffer);
          if (n > 0)
          {
            cript.update(buffer, 0, n);
          }
        } while (n >= 0);
        String digest = Hex.encodeHexString(cript.digest());
        return digest;
      }
      catch (NoSuchAlgorithmException ex)
      {
        throw new IOException(ex);
      }
    }
  }

}
