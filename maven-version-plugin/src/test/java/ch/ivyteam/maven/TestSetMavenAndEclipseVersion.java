package ch.ivyteam.maven;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import mockit.Mock;
import mockit.MockUp;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.fest.assertions.Assertions;
import org.junit.Before;
import org.junit.Test;


public class TestSetMavenAndEclipseVersion extends Assertions
{
  private static final File POM_FILE = new File("testProject/pom.xml");
  
  protected List<String> log = new ArrayList<>();
  protected SetMavenAndEclipseVersion testee = new SetMavenAndEclipseVersion();
  

  @Before
  public void before() throws IOException
  {
    MavenProject project = new MavenProject();
    project.setFile(POM_FILE);
    project.setGroupId(SetMavenAndEclipseVersion.IVY_TOP_LEVEL_ARTIFACT_AND_GROUP_ID);
    project.setArtifactId(SetMavenAndEclipseVersion.IVY_TOP_LEVEL_ARTIFACT_AND_GROUP_ID);
    testee.setProject(project);
    testee.setLog(new MockUp<Log>(){
      @Mock void info( CharSequence content )
      {
        log.add(content.toString());
      }
    }.getMockInstance());
    testee.setVersion("5.1.14-SNAPSHOT");    
    FileUtils.deleteDirectory(new File("testProject"));
    FileUtils.forceDeleteOnExit(new File("testProject"));
    FileUtils.copyDirectory(new File("originalProject"), new File("testProject"));
  }
  
  @Test
  public void testUpdateBundleManifestVersion() throws MojoExecutionException, IOException
  {
    testee.execute();
    compareManifest();
    compareLog();
  }

  @Test
  public void testUpdatePomVersion() throws MojoExecutionException, IOException
  {
    testee.execute();
    comparePom();
    compareLog();
  }

  @Test
  public void testUpdateFeatureVersion() throws MojoExecutionException, IOException
  {
    testee.execute();
    compareFeature();
    compareLog();
  }

  @Test
  public void testUpdateProductVersion() throws MojoExecutionException, IOException
  {
    testee.execute();
    compareProduct();
    compareLog();
  }

  private void compareFeature() throws IOException
  {
    String testeeManifest = FileUtils.readFileToString(new File("testProject/feature.xml"));
    String referenceManifest = FileUtils.readFileToString(new File("referenceProject/feature.xml"));
    assertThat(testeeManifest).isEqualTo(referenceManifest);
  }

  private void comparePom() throws IOException
  {
    String testeeManifest = FileUtils.readFileToString(POM_FILE);
    String referenceManifest = FileUtils.readFileToString(new File("referenceProject/pom.xml"));
    assertThat(testeeManifest).isEqualTo(referenceManifest);
  }

  private void compareManifest() throws IOException
  {
    String testeeManifest = FileUtils.readFileToString(new File("testProject/META-INF/MANIFEST.MF"));
    String referenceManifest = FileUtils.readFileToString(new File("referenceProject/META-INF/MANIFEST.MF"));
    assertThat(testeeManifest).isEqualTo(referenceManifest);
  }
  
  private void compareLog() throws IOException
  {
    List<String> referenceLog = FileUtils.readLines(new File("referenceProject/log.txt"));
    
    List<String> cleanedReferenceLog = new ArrayList<>();
    for (String line : referenceLog)
    {
      line = StringUtils.replace(line, "C:\\dev\\maven-plugin\\maven-plugin\\testProject\\", POM_FILE.getParentFile().getAbsolutePath()+"\\");
      line = StringUtils.replace(line, "\\", File.separator);
      cleanedReferenceLog.add(line);
    }
    assertThat(cleanedReferenceLog).containsOnly(log.toArray());
  }
  
  private void compareProduct() throws IOException
  {
    String testeeProduct = FileUtils.readFileToString(new File("testProject/Designer.product"));
    String referenceProduct = FileUtils.readFileToString(new File("referenceProject/Designer.product"));
    assertThat(testeeProduct).isEqualTo(referenceProduct);
  }

}
