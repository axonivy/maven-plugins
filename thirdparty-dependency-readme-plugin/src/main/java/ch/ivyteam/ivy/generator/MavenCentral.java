package ch.ivyteam.ivy.generator;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;

import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class MavenCentral
{
  
  public static CentralResponse getInfo(String sha1JarHash) throws IOException
  {
    URL url = new URL("http://search.maven.org/solrsearch/select?q=1:"+sha1JarHash+"&rows=10&wt=json");
    String answer = IOUtils.toString(url.openStream());
    ObjectMapper mapper = new ObjectMapper();
    JsonNode root =  mapper.readTree(answer);
    return new CentralResponse(root);
  }
  
  public static class CentralResponse
  {
    private String name;
    private String version;
    private String vendor;

    public CentralResponse(JsonNode answer)
    {
      JsonNode result = answer.at("/response");
      if (result.get("numFound").asInt() == 1)
      {
        JsonNode docs = result.get("docs");
        JsonNode doc = docs.get(0);
        Iterator<String> fields = doc.fieldNames();
        while (fields.hasNext())
        {
          String field = fields.next();
          String value = doc.get(field).asText();
          if (field.equals("v"))
          {
            version = value;
          }
          if (field.equals("g"))
          {
            vendor = getVendorInfoFor(value);
          }
          if (field.equals("a"))
          {
            name = value;
          }
        }
      }
    }
    
    public String getName()
    {
      return name;
    }
    
    public String getVersion()
    {
      return version;
    }
    
    public String getVendor()
    {
      return vendor;
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
  }

}
