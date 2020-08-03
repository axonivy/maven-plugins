package ch.ivyteam.maven;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.junit.Before;
import org.junit.Test;

public class TestSetMavenAndEclipseVersion
{
  private static final File POM_FILE = new File("testProject/pom.xml");
  
  protected List<String> log = new ArrayList<>();
  protected SetMavenAndEclipseVersion testee = new SetMavenAndEclipseVersion();
  

  @Before
  public void before() throws IOException
  {
    FileSet testProjectFs = new FileSet();
    testProjectFs.setDirectory(new File("testProject").getAbsolutePath());
    testProjectFs.setIncludes(Arrays.asList("pom.xml"));
    testee.eclipseArtifactPoms = new FileSet[]{testProjectFs};
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
    assertThat(StringUtils.deleteWhitespace(testeeManifest))
    .isEqualTo(StringUtils.deleteWhitespace(referenceManifest));
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
    
    assertThat(cleanedReferenceLog).containsOnly(log.toArray(new String[log.size()]));
  }
  
  private void compareProduct() throws IOException
  {
    String testeeProduct = FileUtils.readFileToString(new File("testProject/Designer.product"));
    String referenceProduct = FileUtils.readFileToString(new File("referenceProject/Designer.product"));
    assertThat(testeeProduct).isEqualTo(referenceProduct);
  }

}
