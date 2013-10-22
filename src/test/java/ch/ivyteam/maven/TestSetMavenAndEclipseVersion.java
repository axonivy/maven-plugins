package ch.ivyteam.maven;

import java.io.File;
import java.io.IOException;

import mockit.Mock;
import mockit.MockUp;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.StringUtils;
import org.fest.assertions.Assertions;
import org.junit.Before;
import org.junit.Test;


public class TestSetMavenAndEclipseVersion extends Assertions
{
  private static final File POM_FILE = new File("testProject/pom.xml");

  private SetMavenAndEclipseVersion testee = new SetMavenAndEclipseVersion();
  
  private StringBuilder log = new StringBuilder();
  
  @Before
  public void before() throws IOException
  {
    testee.setPomFile(POM_FILE);
    testee.setLog(new MockUp<Log>(){
      @Mock void info( CharSequence content )
      {
        log.append(content);
        log.append("\r\n");
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
    String referenceLog = FileUtils.readFileToString(new File("referenceProject/log.txt"));
    referenceLog = StringUtils.replace(referenceLog, "C:\\dev\\maven-plugin\\maven-plugin\\testProject\\", POM_FILE.getParentFile().getCanonicalPath()+"\\");
    assertThat(log.toString()).isEqualTo(referenceLog);
  }
}
