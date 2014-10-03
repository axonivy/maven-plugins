package ch.ivyteam.ivy.maven;

import static org.fest.assertions.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;

import net.lingala.zip4j.core.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.project.MavenProject;
import org.junit.Rule;
import org.junit.Test;

public class TestIarPackagingMojo
{
  private File base;
  private IarPackagingMojo mojo;
  
  @Rule
  public MojoRule rule = new MojoRule()
  {
    @Override
    protected void before() throws Throwable 
    {
      base = Files.createTempDirectory("MyBaseProject").toFile();
      FileUtils.copyDirectory(new File("src/test/resources/base"), base);
      MavenProject project = rule.readMavenProject(base);
      mojo = (IarPackagingMojo) rule.lookupConfiguredMojo(project, "pack-iar");
    }
    
    @Override
    protected void after() 
    {
      try
      {
        FileUtils.deleteDirectory(base);
      }
      catch (IOException ex)
      {
        throw new RuntimeException(ex);
      }  
    }
  };
  
  @Test
  public void testArchiveCreation() throws Exception
  {
    mojo.execute();
    Collection<File> iarFiles = FileUtils.listFiles(new File(base, "target"), new String[]{"iar"}, false);
    assertThat(iarFiles).hasSize(1);
    
    File iarFile = iarFiles.iterator().next();
    assertThat(iarFile.getName()).isEqualTo("base-0.0.1-SNAPSHOT.iar");
    assertThat(mojo.project.getArtifact().getFile())
      .as("Created IAR must be registered as artifact for later repository installation.").isEqualTo(iarFile);
    ZipFile archive = new ZipFile(iarFile);
    
    assertThat(archive.getFileHeader(".classpath")).as(".classpath must be packed for internal binary retrieval").isNotNull();
    assertThat(archive.getFileHeader("pom.xml")).as("pom.xml should not be packed").isNull();
    assertThat(archive.getFileHeader("target/sampleOutput.txt")).as("'target' folder should not be packed").isNull();
  }
  
  @Test
  public void testCanDefineCustomExclusions() throws Exception
  {
    String filterCandidate = "private/notPublic.txt";
    assertThat(new File(base, filterCandidate)).exists();
    
    mojo.excludes = new String[]{"private/**/*"};
    mojo.execute();
    ZipFile archive = new ZipFile(mojo.project.getArtifact().getFile());
    
    assertThat(archive.getFileHeader(filterCandidate)).as("Custom exclusion must be filtered").isNull();
    assertThat(archive.getFileHeader("pom.xml")).as("Default exclusion must be filtered").isNull();
    assertThat(archive.getFileHeaders().size()).isGreaterThan(50).as("archive must contain content");
  }
  
}
