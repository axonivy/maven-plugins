package com.axonivy.asciidoctor.osgi.bundle.doc;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class MavenPom
{
  private String description;
  
  public MavenPom(String description)
  {
    this.description = description;
  }

  public static MavenPom read(File bundleProjectDirectory)
  {
    try
    {
      File mavenPomFile = new File(bundleProjectDirectory, "pom.xml");
      if (!mavenPomFile.exists())
      {
        return null;
      }
        
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      Document doc = dBuilder.parse(mavenPomFile);
      NodeList projects = doc.getElementsByTagName("project");
      if (projects.getLength()<1)
      {
        return new MavenPom("");
      }
      Element project = (Element)projects.item(0);
      NodeList descriptions = project.getElementsByTagName("description");
      if (descriptions.getLength()<1)
      {
        return new MavenPom("");
      }
      Element description = (Element)descriptions.item(0);
      return new MavenPom(description.getTextContent());
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
      return null;
    }
  }

  public String getDescription()
  {
    return description;
  }
}
