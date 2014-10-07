package ch.ivyteam.ivy.maven;

import static org.fest.assertions.api.Assertions.assertThat;

import java.io.File;
import java.util.Collection;

import net.lingala.zip4j.core.ZipFile;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;

public class TestIarPackagingMojo
{
  
  @Rule
  public ProjectMojoRule<IarPackagingMojo> rule = 
    new ProjectMojoRule<>(new File("src/test/resources/base"), "pack-iar");
  
  @Test
  public void testArchiveCreation() throws Exception
  {
    IarPackagingMojo mojo = rule.getMojo();
    mojo.execute();
    Collection<File> iarFiles = FileUtils.listFiles(new File(mojo.project.getBasedir(), "target"), new String[]{"iar"}, false);
    assertThat(iarFiles).hasSize(1);
    
    File iarFile = iarFiles.iterator().next();
    assertThat(iarFile.getName()).isEqualTo("base-0.0.1-SNAPSHOT.iar");
    assertThat(mojo.project.getArtifact().getFile())
      .as("Created IAR must be registered as artifact for later repository installation.").isEqualTo(iarFile);
    ZipFile archive = new ZipFile(iarFile);
    
    assertThat(archive.getFileHeader(".classpath")).as(".classpath must be packed for internal binary retrieval").isNotNull();
    assertThat(archive.getFileHeader("target/sampleOutput.txt")).as("'target' folder should not be packed").isNull();
  }
  
  @Test
  public void testCanDefineCustomExclusions() throws Exception
  {
    IarPackagingMojo mojo = rule.getMojo();
    String filterCandidate = "private/notPublic.txt";
    assertThat(new File(mojo.project.getBasedir(), filterCandidate)).exists();
    
    mojo.excludes = new String[]{"private/**/*"};
    mojo.execute();
    ZipFile archive = new ZipFile(mojo.project.getArtifact().getFile());
    
    assertThat(archive.getFileHeader(filterCandidate)).as("Custom exclusion must be filtered").isNull();
    assertThat(archive.getFileHeaders().size()).isGreaterThan(50).as("archive must contain content");
  }
  
}
