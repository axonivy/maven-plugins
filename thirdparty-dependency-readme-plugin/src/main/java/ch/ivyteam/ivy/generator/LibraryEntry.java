package ch.ivyteam.ivy.generator;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import ch.ivyteam.ivy.generator.MavenCentral.CentralResponse;
import ch.ivyteam.xml.XmlUtil;

class LibraryEntry
{
  public final String pluginName;
  public final String jarName;
  private JarInfo info;
  
  public LibraryEntry(String pluginName, String jarName, JarInfo info)
  {
    this.pluginName = pluginName;
    this.jarName = jarName;
    this.info = info;
  }
  
  void enhanceFromMavenCentral() throws IOException
  {
    if (!info.isComplete())
    {
      try 
      {
        CentralResponse response = MavenCentral.getInfo(info.getSha1Hash());
        this.info = new JarInfo(
              StringUtils.defaultIfBlank(info.getName(), response.getName()),
              StringUtils.defaultIfBlank(info.getVersion(), response.getVersion()),
              StringUtils.defaultIfBlank(info.getVendor(), response.getVendor()),
              info.getSha1Hash());
      }
      catch(IOException ex)
      {
        throw new IOException("Could not get information for "+this, ex);
      }
    }
  }
  
  public JarInfo getInfo()
  {
    return info;
  }
  
  String toHtmlRow()
  {
    StringBuilder rowHtml = new StringBuilder();
    rowHtml.append("    <tr>\n");
    if (pluginName != null)
    {
    //  rowHtml.append("      <td>");
    //  rowHtml.append(XmlUtil.escapeHtmlAndConvertNewline(pluginName));
    //  rowHtml.append("</td>\n");
    }
    rowHtml.append("      <td>");
    rowHtml.append(XmlUtil.escapeHtmlAndConvertNewline(jarName) +  (pluginName == null ? "" : "<br /><i>" + pluginName + "</i>"));
    rowHtml.append("</td>\n");
    rowHtml.append("      <td>");
    
    rowHtml.append(clean(info.getName()));
    rowHtml.append("<br /><i>by " + clean(info.getVendor()) + "</i>");
    
    rowHtml.append("</td>\n");
    rowHtml.append("      <td>");
    rowHtml.append(clean(info.getVersion()));
    rowHtml.append("</td>\n");    
    rowHtml.append("    </tr>");
    
    return rowHtml.toString();
  }
  
  private static String clean(String value)
  {
    if (value == null)
    {
      value = "n.a.";
    }
    return XmlUtil.escapeHtmlAndConvertNewline(
            removeAllQoutes(value));
  }

  private static String removeAllQoutes(String name)
  {
    if (name.startsWith("\"") || name.startsWith("'"))
    {
      name = name.substring(1, name.length());
    }
    if (name.endsWith("\"") || name.endsWith("'"))
    {
      name = name.substring(0, name.length() - 1);
    }
    return name;
  }
  
  @Override
  public String toString()
  {
    return pluginName+"/"+jarName;
  }
  
  static void enhanceConcurrent(List<LibraryEntry> dependencies) throws InterruptedException
  {
    ExecutorService executorService = Executors.newFixedThreadPool(30);
    for(int i=0; i<dependencies.size(); i++)
    {
      final LibraryEntry dependency = dependencies.get(i);
      executorService.execute(new Runnable()
        {
          @Override
          public void run()
          {
            try
            {
              dependency.enhanceFromMavenCentral();
            }
            catch (IOException ex)
            {
              ex.printStackTrace();
            }
          }
        });
    }
    executorService.shutdown();
    executorService.awaitTermination(30, TimeUnit.SECONDS);
  }
  
  static void enhanceSerial(List<LibraryEntry> dependencies) throws IOException
  {
    for(LibraryEntry dependency : dependencies)
    {
      dependency.enhanceFromMavenCentral();
    }
  }
}