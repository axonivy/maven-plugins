package ch.ivyteam.ivy.maven;

import static org.fest.assertions.api.Assertions.assertThat;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import net.lingala.zip4j.core.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.FileSet;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Reguel Wermelinger
 * @since 03.11.2014
 */
public class TestIarPackagingMojo
{
  
  @Rule
  public ProjectMojoRule<IarPackagingMojo> rule = 
    new ProjectMojoRule<>(new File("src/test/resources/base"), IarPackagingMojo.GOAL);
  
  /**
   * Happy path creation tests
   * @throws Exception
   */
  @Test
  public void archiveCreationDefault() throws Exception
  {
    IarPackagingMojo mojo = rule.getMojo();
    mojo.execute();
    Collection<File> iarFiles = FileUtils.listFiles(new File(mojo.project.getBasedir(), "target"), new String[]{"iar"}, false);
    assertThat(iarFiles).hasSize(1);
    
    File iarFile = iarFiles.iterator().next();
    assertThat(iarFile.getName()).isEqualTo("base-1.0.0.iar");
    assertThat(mojo.project.getArtifact().getFile())
      .as("Created IAR must be registered as artifact for later repository installation.").isEqualTo(iarFile);
    
    try(java.util.zip.ZipFile archive = new java.util.zip.ZipFile(iarFile))
    {
      assertThat(archive.getEntry(".classpath")).as(".classpath must be packed for internal binary retrieval").isNotNull();
      assertThat(archive.getEntry("src_hd")).as("Empty directories should be included (by default) "
              + "so that the IAR can be re-imported into the designer").isNotNull();
      assertThat(archive.getEntry("target/sampleOutput.txt")).as("'target' folder should not be packed").isNull();
    }
  }
  
  @Test
  public void canDefineCustomExclusions() throws Exception
  {
    IarPackagingMojo mojo = rule.getMojo();
    String filterCandidate = "private/notPublic.txt";
    assertThat(new File(mojo.project.getBasedir(), filterCandidate)).exists();
    
    mojo.iarExcludes = new String[]{"private/**/*"};
    mojo.execute();
    ZipFile archive = new ZipFile(mojo.project.getArtifact().getFile());
    
    assertThat(archive.getFileHeader(filterCandidate)).as("Custom exclusion must be filtered").isNull();
    assertThat(archive.getFileHeaders().size()).isGreaterThan(50).as("archive must contain content");
  }
  
  @Test
  public void canDefineCustomInclusions() throws Exception
  {
    IarPackagingMojo mojo = rule.getMojo();
    File outputDir = new File(mojo.project.getBasedir(), "target");
    File customPomXml = new File(outputDir, "myCustomPom.xml");
    FileUtils.write(customPomXml, "customPomContent");
    
    String relativeCustomIncludePath = "target/"+customPomXml.getName();
    FileSet fs = new FileSet();
    fs.setIncludes(Arrays.asList(relativeCustomIncludePath));
    mojo.iarFileSets = new FileSet[]{fs};
    
    mojo.execute();
    ZipFile archive = new ZipFile(mojo.project.getArtifact().getFile());
    
    assertThat(archive.getFileHeader(relativeCustomIncludePath)).as("Custom inclusions must be included").isNotNull();
  }
  
  @Test
  public void canExcludeEmptyDirectories() throws Exception
  {
    IarPackagingMojo mojo = rule.getMojo();
    mojo.iarIncludesEmptyDirs = false;
    mojo.execute();
    
    try(java.util.zip.ZipFile archive = new java.util.zip.ZipFile(mojo.project.getArtifact().getFile()))
    {
      assertThat(archive.getEntry("src_hd")).as("Empty directory should be excluded by mojo configuration").isNull();
    }
  }
  
}
