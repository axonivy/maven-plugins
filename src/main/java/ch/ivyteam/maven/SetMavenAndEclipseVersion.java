package ch.ivyteam.maven;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Goal which replaces project/version and project/parent/version in all *.pom files and the Bundle-Version in all bundle MANIFEST.MF files
 * with the given <code>ivy-version</code>.<BR>
 * 
 * Usage: <code>mvn ch.ivyteam:maven-plugin:setMavenAndEclipseVersion -Divy-version=5.0.1-SNAPSHOT</code>
 * 
 * @goal setMavenAndEclipseVersion
 * 
 * @phase validate
 */
public class SetMavenAndEclipseVersion extends AbstractMojo
{
  /**
   * @parameter expression="${project}"
   * @required
   */
  private MavenProject project;

  /**
   * @parameter expression="${ivy-version}
   */
  private String version;

  private String bundleVersion;

  public void execute() throws MojoExecutionException
  {
    try
    {
      checkParameters();
      replaceVersion();
    }
    catch (Exception ex)
    {
      throw new MojoExecutionException("Could not replace version", ex);
    }
  }

  private void checkParameters() throws MojoExecutionException
  {
    if (StringUtils.isEmpty(version))
    {
      throw new MojoExecutionException("No ivy-version parameter defined. Please define it as command line parameter. E.g. \"-Divy-version=5.0.1-Snapshot\"");
    }
    if (version.indexOf('-') >= 0)
    {
      bundleVersion = StringUtils.substringAfter(version, "-");
    }
    else
    {
      bundleVersion = version;
    }
    
    bundleVersion += ".qualifier";    
  }

  private void replaceVersion() throws Exception
  {
    File pom = project.getFile();
    File projectDirectory = pom.getParentFile();
    replaceVersionInPom(pom);
    replaceVersionInPluginManifest(projectDirectory);
  }

  private void replaceVersionInPluginManifest(File projectDirectory) throws IOException
  {
    File manifestFile = new File(projectDirectory, "META-INF/MANIFEST.MF");

    Manifest manifest = readManifest(manifestFile);
    if (manifest != null)
    {
      String oldVersion = manifest.getMainAttributes().getValue("Bundle-Version");
      if (needsUpdate(oldVersion))
      {
        getLog().info("Replace plugin version "+oldVersion+" with version "+bundleVersion+" in manifest file "+manifestFile.getAbsolutePath());        
        replaceVersionInPluginManifest(manifestFile, oldVersion);
      }
      else
      {
        getLog().info("Plugin version of project "+projectDirectory+" is up to date. Nothing to do.");
      }
    }
    else
    {
      getLog().info("No manifest file found in project "+projectDirectory+". Nothing to do");
    }
  }

  private void replaceVersionInPluginManifest(File manifestFile, String oldVersion) throws IOException
  {
    String content = FileUtils.readFileToString(manifestFile);
    content = content.replaceFirst(Pattern.quote("Bundle-Version:")+"\\s"+Pattern.quote(oldVersion), "Bundle-Version: "+bundleVersion);
    FileUtils.writeStringToFile(manifestFile,  content);
  }

  private boolean needsUpdate(String oldVersion)
  {
    return !oldVersion.trim().equals(bundleVersion.trim());
  }

  private Manifest readManifest(File manifestFile) throws FileNotFoundException, IOException
  {
    if (manifestFile.exists())
    {
      FileInputStream fis = new FileInputStream(manifestFile);
      try
      {
        Manifest manifest = new Manifest(fis);
        return manifest;
      }
      finally
      {
        fis.close();
      }
    }
    return null;
  }

  private void replaceVersionInPom(File pom) throws Exception
  {
    boolean changed = false;
    Document pomDocument = readPom(pom);

    changed = replaceVersionInNode(pom, changed, pomDocument, "/project/version/text()");
    changed = replaceVersionInNode(pom, changed, pomDocument, "/project/parent/version/text()");

    if (!changed)
    {
      getLog().info("Artifact versions declared in pom "+pom.getAbsolutePath()+" are up to date. Nothing to do.");
    }
  }

  private boolean replaceVersionInNode(File pom, boolean changed, Document pomDocument, String xPathStr)
          throws XPathExpressionException, IOException
  {
    Node versionNode = findNode(pomDocument, xPathStr);
    if (needsUpdate(versionNode))
    {
      replaceTextInXml(pom, xPathStr, versionNode.getNodeValue(), version);
      getLog().info("Replace version "+versionNode.getNodeValue()+" with version "+version+" in node "+xPathStr+" of pom file "+pom.getAbsolutePath());
      changed = true;
    }
    return changed;
  }

  private Node findNode(Document pomDocument, String xPathStr) throws XPathExpressionException
  {
    XPath xpath = XPathFactory.newInstance().newXPath();
    return ((NodeList) xpath.compile(xPathStr).evaluate(pomDocument, XPathConstants.NODESET)).item(0);
  }

  private Document readPom(File pom) throws SAXException, IOException, ParserConfigurationException
  {
    DocumentBuilderFactory domFactory = DocumentBuilderFactory
            .newInstance();
    Document doc = domFactory.newDocumentBuilder().parse(
            pom.getAbsolutePath());
    return doc;
  }

  private boolean needsUpdate(Node node)
  {
    return node != null && !node.getTextContent().trim().equals(version.trim());
  }

  private void replaceTextInXml(File pom, String xPathStr, String oldVersion, String newVersion) throws IOException
  {
    int pos=0;
    String content = FileUtils.readFileToString(pom);
    StringBuffer newContent = new StringBuffer();
    for (String tag : xPathStr.split("/"))
    {
      if (tag.equals("text()"))
      {
        newContent.append(content.substring(0, pos));
        content = content.substring(pos);
        newContent.append(content.replaceFirst(Pattern.quote(oldVersion), newVersion));
        FileUtils.writeStringToFile(pom, newContent.toString());
      }
      else
      {
        pos = content.indexOf("<"+tag+">", pos);
      }
    }
  }
}
