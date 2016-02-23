package ch.ivyteam.ivy.generator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class MavenCentral
{

  public static JarInfo enhanceInfo(JarInfo info, File jarFile) throws FileNotFoundException, IOException
  {
    try(FileInputStream fis = new FileInputStream(jarFile))
    {
      return enhanceInfo(info, fis);
    }
  }

  public static JarInfo enhanceInfo(JarInfo info, InputStream inputStream) throws IOException
  {
    if (info.getName() != null && info.getVersion() != null && info.getVendor() != null)
    {
       return info;
    }
    String digest = buildDigest(inputStream);
    JsonNode answer = requestInfo(digest);
    return readInfo(info, answer);
  }

  private static JarInfo readInfo(JarInfo info, JsonNode answer)
  {
    JsonNode result = answer.at("/response");
    int numFound = result.get("numFound").asInt();
    if (numFound == 1)
    {
      JsonNode docs = result.get("docs");
      JsonNode doc = docs.get(0);
      Iterator<String> fields = doc.fieldNames();
      while (fields.hasNext())
      {
        String field = fields.next();
        String value = doc.get(field).asText();
        if (field.equals("v") && info.getVersion() == null)
        {
          info.setVersion(value);
        }
        if (field.equals("g") && info.getVendor() == null)
        {
          info.setVendor(getVendorInfoFor(value));
        }
        if (field.equals("a") && info.getName() == null)
        {
          info.setName(value);
        }
      }
    }
    return info;
  }

  private static String getVendorInfoFor(String value)
  {
    if (value.startsWith("org.apache"))
    {
      return "Apache Software Foundation";
    }
    if (value.startsWith("org.eclipse"))
    {
      return "Eclipse Foundation";
    }
    if (value.startsWith("com.google"))
    {
      return "Google";
    }
    if (value.startsWith("org.springframework"))
    {
      return "Spring Framework";
    }
    return value;
  }

  private static JsonNode requestInfo(String jarDigest) throws IOException
  {
    URL url = new URL("http://search.maven.org/solrsearch/select?q=1:\""+jarDigest+"\"&rows=10&wt=json");
    String answer = IOUtils.toString(url.openStream());
    ObjectMapper mapper = new ObjectMapper();
    JsonNode root =  mapper.readTree(answer);
    return root;
  }

  private static String buildDigest(InputStream inputStream) throws IOException
  {
    try
    {
      MessageDigest cript;
      cript = MessageDigest.getInstance("SHA-1");
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
