package ch.ivyteam.maven;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.FileSet;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.fest.assertions.api.Assertions;
import org.junit.Before;
import org.junit.Test;


public class TestSetVersionOnNotReferencedProjects extends Assertions
{
  private SetMavenAndEclipseVersion testee = new SetMavenAndEclipseVersion();
  
  protected List<String> log = new ArrayList<>();
  
  @Before
  public void before() throws IOException
  {
    FileSet testProjectFs = new FileSet();
    testProjectFs.setDirectory(new File("testIvy").getAbsolutePath());
    testProjectFs.setIncludes(Arrays.asList("**/pom.xml"));
    testProjectFs.setExcludes(Arrays.asList(
            "**/ch.ivyteam.ivy.another.feature/pom.xml", 
            "**/ch.ivyteam.ivy.build.maven/*"));
    testee.eclipseArtifactPoms = new FileSet[]{testProjectFs};
    
    FileSet testPoms = new FileSet();
    testPoms.setDirectory(new File("testIvy").getAbsolutePath());
    testPoms.setIncludes(Arrays.asList("development/ch.ivyteam.ivy.build.maven/**/pom.xml"));
    testee.pomsToUpdate = new FileSet[]{testPoms};
    
    testee.setLog(new Log()
    {
      @Override
      public void warn(CharSequence content, Throwable error)
      {
      }
      
      @Override
      public void warn(Throwable error)
      {
      }
      
      @Override
      public void warn(CharSequence content)
      {
      }
      
      @Override
      public boolean isWarnEnabled()
      {
        return false;
      }
      
      @Override
      public boolean isInfoEnabled()
      {
        return false;
      }
      
      @Override
      public boolean isErrorEnabled()
      {
        return false;
      }
      
      @Override
      public boolean isDebugEnabled()
      {
        return false;
      }
      
      @Override
      public void info(CharSequence content, Throwable error)
      {
      }
      
      @Override
      public void info(Throwable error)
      {
      }
      
      @Override
      public void info(CharSequence content)
      {
        log.add(content.toString());
      }
      
      @Override
      public void error(CharSequence content, Throwable error)
      {
      }
      
      @Override
      public void error(Throwable error)
      {
      }
      
      @Override
      public void error(CharSequence content)
      {
      }
      
      @Override
      public void debug(CharSequence content, Throwable error)
      {
      }
      
      @Override
      public void debug(Throwable error)
      {
      }

      @Override
      public void debug(CharSequence content)
      {
      }
    });
    testee.version = "5.1.14-SNAPSHOT";    
    testee.externalBuiltArtifacts = Arrays.asList("ch.ivyteam.ulc.base", "ch.ivyteam.ulc.extension", "ch.ivyteam.ivy.richdialog.components",
          "ch.ivyteam.ivy.designer.cm.ui", "ch.ivyteam.vn.feature", "ch.ivyteam.ulc.base.source",
          "ch.ivyteam.ulc.extension.source", "ch.ivyteam.ivy.richdialog.components.source");
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
    assertThat(testeePom).as("Content of '"+ relativePomPath+"' is wrong")
      .isEqualTo(referencePom);
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
      line = StringUtils.replace(line, "C:\\dev\\maven-plugin\\maven-plugin\\testIvy\\", testee.eclipseArtifactPoms[0].getDirectory()+"\\");
      line = StringUtils.replace(line, "\\", File.separator);
      cleanedReferenceLog.add(line);
    }
    assertThat(cleanedReferenceLog).containsOnly(log.toArray(new String[log.size()]));
  }
}
