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
import java.io.IOException;

import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.xml.sax.SAXException;

/**
 * Goal which replaces project/version and project/parent/version in all *.pom files and the Bundle-Version in all bundle MANIFEST.MF files
 * with the given <code>ivy-version</code>.<BR>
 * 
 * Usage: <code>mvn ch.ivyteam:maven-version-plugin:setMavenAndEclipseVersion -Divy-version=5.0.1-SNAPSHOT</code>
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
  
  private File pomFile;

  @Override
  public void execute() throws MojoExecutionException
  {
    try
    {
      checkParameters();
      updateVersion();
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
  }

  private void updateVersion() throws Exception
  {
    File pom = getPomFile();
    File projectDirectory = pom.getParentFile();
    updateVersionsInPom(pom);
    updateVersionsInBundleManifest(projectDirectory);
    updateVersionInFeatureXml(projectDirectory);
    updateVersionInProductXml(projectDirectory);
  }

  private void updateVersionInProductXml(File projectDirectory) throws XPathExpressionException, SAXException, IOException
  {
    ProductXmlFileUpdater updater = new ProductXmlFileUpdater(projectDirectory, version, getLog());
    updater.update();
  }

  private void updateVersionsInBundleManifest(File projectDirectory) throws IOException
  {
    BundleManifestFileUpdater updater = new BundleManifestFileUpdater(projectDirectory, version, getLog());
    updater.update();
  }

  private void updateVersionsInPom(File pom) throws Exception
  {
    PomXmlFileUpdater updater = new PomXmlFileUpdater(pom, version, getLog());
    updater.update();
  }

  private void updateVersionInFeatureXml(File projectDirectory) throws Exception
  {
    FeatureXmlFileUpdater updater = new FeatureXmlFileUpdater(projectDirectory, version, getLog());
    updater.update();
  }

  void setVersion(String version)
  {
    this.version = version;
  }
  
  private File getPomFile()
  {
    if (project != null)
    {
      return project.getFile();
    }
    return pomFile;
  }

  void setPomFile(File pomFile)
  {
    this.pomFile = pomFile;
  }
}
