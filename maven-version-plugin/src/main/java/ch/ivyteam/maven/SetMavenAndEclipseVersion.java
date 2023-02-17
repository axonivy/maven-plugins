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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.FileSet;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;
import org.xml.sax.SAXException;

/**
 * See development/ch.ivyteam.ivy.build.maven/manual/update-version for sample usage.
 */
@Mojo(name="setMavenAndEclipseVersion",
  defaultPhase=LifecyclePhase.PROCESS_RESOURCES)
public class SetMavenAndEclipseVersion extends AbstractMojo
{
  @Parameter(property="ivy-version")
  String version;

  /** POM project files of an eclipse artifact.
   * Eclipse descriptors like
   * <code>META-INF/MANIFEST.MF</code>,
   * <code>feature.xml</code>,
   * <code>category.xml</code>,
   * <code>*.product</code> will be updated as well.
   * */
  @Parameter
  FileSet[] eclipseArtifactPoms;

  /** MANIFEST.MF file of an eclipse artifact.
   * Other descriptors of this project like
   * <code>pom.xml</code>,
   * <code>feature.xml</code>,
   * <code>category.xml</code>,
   * <code>*.product</code> will be updated as well.
   * */
  @Parameter
  FileSet[] eclipseManifests;

  /** Maven project POMs to update.
   * No other resource in the same project will be touched than the POM itself.
   */
  @Parameter
  FileSet[] pomsToUpdate;

  /** will only update the version of the parent pom  */
  @Parameter
  FileSet[] parentPomsToUpdate;

  /**
   * Describes artifacts that should not be touched by this version change
   */
  @Parameter(property="external-artifacts")
  List<String> externalBuiltArtifacts;


  @Parameter
  boolean stripBundleVersionOfDependencies;

  @Parameter
  boolean skip;

  @Override
  public void execute() throws MojoExecutionException
  {
    if (skip)
    {
      getLog().info("Skipping MavenAndEclipseVersion update");
      return;
    }

    try
    {
      checkParameters();
      for(File eclipseProjectPom : getFiles(eclipseArtifactPoms))
      {
        updateVersion(eclipseProjectPom);
      }
      for(File eclipseManifest : getFiles(eclipseManifests))
      {
        updateVersion(eclipseManifest.getParentFile());
      }
      for(File pomXml : getFiles(pomsToUpdate))
      {
        updateVersionsInPom(pomXml.getParentFile());
      }
      for (File pomXml : getFiles(parentPomsToUpdate))
      {
        updateParentVersionInPom(pomXml.getParentFile());
      }
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
    if (externalBuiltArtifacts == null)
    {
      externalBuiltArtifacts = Collections.emptyList();
    }
  }

  private void updateVersion(File pom) throws Exception
  {
    File projectDirectory = pom.getParentFile();
    getLog().debug("updating versions in '"+projectDirectory.getAbsolutePath()+"'.");
    updateVersionsInPom(projectDirectory);
    updateVersionsInBundleManifest(projectDirectory);
    updateVersionInFeatureXml(projectDirectory);
    updateVersionInProductXml(projectDirectory);
    updateVersionsInCategoryXml(projectDirectory);
  }

  private void updateVersionInProductXml(File projectDirectory) throws XPathExpressionException, SAXException, IOException
  {
    new ProductXmlFileUpdater(projectDirectory, version, getLog(), externalBuiltArtifacts).update();
  }

  private void updateVersionsInBundleManifest(File projectDirectory) throws IOException
  {
    new BundleManifestFileUpdater(projectDirectory, version, getLog(),
            externalBuiltArtifacts, stripBundleVersionOfDependencies).update();
  }

  private void updateVersionsInPom(File projectDirectory) throws Exception
  {
    new PomXmlFileUpdater(new File(projectDirectory, "pom.xml"), version, getLog(), externalBuiltArtifacts).update();
  }

  private void updateParentVersionInPom(File projectDirectory) throws Exception
  {
    new PomXmlFileUpdater(new File(projectDirectory, "pom.xml"), version, getLog(), externalBuiltArtifacts).updateParentVersoin();
  }

  private void updateVersionInFeatureXml(File projectDirectory) throws Exception
  {
    new FeatureXmlFileUpdater(projectDirectory, version, getLog(), externalBuiltArtifacts).update();
  }

  private void updateVersionsInCategoryXml(File projectDirectory) throws Exception
  {
    new CategoryXmlFileUpdater(projectDirectory, version, getLog(), externalBuiltArtifacts).update();
  }

  private List<File> getFiles(FileSet[] fileSets)
  {
    if (ArrayUtils.isEmpty(fileSets))
    {
      return Collections.emptyList();
    }

    List<File> files = new ArrayList<>();
    for(FileSet fs : fileSets)
    {
      files.addAll(getFiles(fs));
    }
    return files;
  }

  private List<File> getFiles(FileSet fs) {
    File directory = new File(fs.getDirectory());
    String includes = StringUtils.join(fs.getIncludes(), ",");
    String excludes = StringUtils.join(fs.getExcludes(), ",");
    try {
      var files = FileUtils.getFiles(directory, includes, excludes);
      if (files.isEmpty()) {
        getLog().debug("FileSet did not match any file in the file system: " + fs);
      }
      return files;
    } catch (IOException ex) {
      getLog().error(ex);
      return Collections.emptyList();
    }
  }
}
