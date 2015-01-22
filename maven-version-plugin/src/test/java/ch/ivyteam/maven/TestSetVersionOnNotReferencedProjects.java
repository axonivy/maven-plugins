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


public class TestSetVersionOnNotReferencedProjects extends Assertions
{
  private static final File POM_FILE = new File("testIvy/pom.xml");

  private SetMavenAndEclipseVersion testee = new SetMavenAndEclipseVersion();
  
  protected List<String> log = new ArrayList<>();
  
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
    File testIvy = new File("testIvy");
    FileUtils.deleteDirectory(testIvy);
    FileUtils.forceDeleteOnExit(testIvy);
    FileUtils.copyDirectory(new File("originalIvy"), testIvy);
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
    compareMavenConfigPom();
    compareMavenModulesPom();
    compareLog();
  }
  
  @Test
  public void testUpdateNotReferencedTestPomAndFeature() throws MojoExecutionException, IOException
  {
    testee.execute();
    comparePom("development/features/ch.ivyteam.ivy.another.feature/pom.xml");
    compareFeature("development/features/ch.ivyteam.ivy.another.feature/feature.xml");
    comparePom("development/features/ch.ivyteam.ivy.test.feature/pom.xml");
    compareFeature("development/features/ch.ivyteam.ivy.test.feature/feature.xml");
    comparePom("development/updatesites/ch.ivyteam.ivy.test.p2/pom.xml");
    compareCategory("development/updatesites/ch.ivyteam.ivy.test.p2/category.xml");
    compareLog();
  }

  private void compareMavenModulesPom() throws IOException
  {
    comparePom("development/ch.ivyteam.ivy.build.maven/pom.xml");
  }

  private void compareMavenConfigPom() throws IOException
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
  
  private void compareFeature(String relativeFeatureXmlPath) throws IOException
  {
    String testeeManifest = FileUtils.readFileToString(new File("testIvy", relativeFeatureXmlPath));
    String referenceManifest = FileUtils.readFileToString(new File("referenceIvy", relativeFeatureXmlPath));
    assertThat(testeeManifest).isEqualTo(referenceManifest);
  }
  
  private void compareCategory(String relativeCategoryXmlPath) throws IOException
  {
    String testeeCatInfo = FileUtils.readFileToString(new File("testIvy", relativeCategoryXmlPath));
    String referenceCatInfo = FileUtils.readFileToString(new File("referenceIvy", relativeCategoryXmlPath));
    assertThat(testeeCatInfo).isEqualTo(referenceCatInfo);
  }
  
  private void compareLog() throws IOException
  {
    List<String> referenceLog = FileUtils.readLines(new File("referenceIvy/log.txt"));
    
    List<String> cleanedReferenceLog = new ArrayList<>();
    for (String line : referenceLog)
    {
      line = StringUtils.replace(line, "C:\\dev\\maven-plugin\\maven-plugin\\testIvy\\", POM_FILE.getParentFile().getAbsolutePath()+"\\");
      line = StringUtils.replace(line, "\\", File.separator);
      cleanedReferenceLog.add(line);
    }
    assertThat(cleanedReferenceLog).containsOnly(log.toArray());
  }
}
