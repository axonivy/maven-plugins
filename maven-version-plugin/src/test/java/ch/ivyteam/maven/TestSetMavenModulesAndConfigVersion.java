package ch.ivyteam.maven;

import java.io.File;
import java.io.IOException;

import mockit.Mock;
import mockit.MockUp;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.fest.assertions.Assertions;
import org.junit.Before;
import org.junit.Test;


public class TestSetMavenModulesAndConfigVersion extends Assertions
{
  private static final File POM_FILE = new File("testIvy/pom.xml");

  private SetMavenAndEclipseVersion testee = new SetMavenAndEclipseVersion();
  
  private StringBuilder log = new StringBuilder();
  
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
        log.append(content);
        log.append("\r\n");
      }
    }.getMockInstance());
    testee.setVersion("5.1.14-SNAPSHOT");    
    FileUtils.deleteDirectory(new File("testIvy"));
    FileUtils.forceDeleteOnExit(new File("testIvy"));
    FileUtils.copyDirectory(new File("originalIvy"), new File("testIvy"));
  }
  
  @Test
  public void testUpdatePomVersion() throws MojoExecutionException, IOException
  {
    testee.execute();
    comparePom();
    compareLog();
  }

  @Test
  public void testUpdateConfigAndModulesPomVersion() throws MojoExecutionException, IOException
  {
    testee.execute();
    compareConfigPom();
    compareModulesPom();
    compareLog();
  }

  private void compareModulesPom() throws IOException
  {
    comparePom("development/ch.ivyteam.ivy.build.maven/pom.xml");
  }

  private void compareConfigPom() throws IOException
  {
    comparePom("development/ch.ivyteam.ivy.build.maven/config/pom.xml");    
  }

  private void comparePom() throws IOException
  {
    comparePom("pom.xml");
  }
  
  private void comparePom(String relativePomPath) throws IOException
  {
    String testeePom = FileUtils.readFileToString(new File("testIvy", relativePomPath));
    String referencePom = FileUtils.readFileToString(new File("referenceIvy", relativePomPath));
    assertThat(testeePom).isEqualTo(referencePom);
  }

  
  private void compareLog() throws IOException
  {
    String referenceLog = FileUtils.readFileToString(new File("referenceIvy/log.txt"));
    referenceLog = StringUtils.replace(referenceLog, "C:\\dev\\maven-plugin\\maven-plugin\\testIvy\\", POM_FILE.getParentFile().getAbsolutePath()+"\\");
    assertThat(log.toString()).isEqualTo(referenceLog);
  }
}
