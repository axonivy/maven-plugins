package ch.ivyteam.maven;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.FileSet;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestSetMavenAndEclipseVersion {

  private static final File POM_FILE = new File("testProject/pom.xml");
  protected List<String> log = new ArrayList<>();
  protected SetMavenAndEclipseVersion testee = new SetMavenAndEclipseVersion();

  @BeforeEach
  void before() throws IOException {
    FileSet testProjectFs = new FileSet();
    testProjectFs.setDirectory(new File("testProject").getAbsolutePath());
    testProjectFs.setIncludes(Arrays.asList("pom.xml"));
    testee.eclipseArtifactPoms = new FileSet[] {testProjectFs};
    testee.setLog(new Log() {

      @Override
      public void warn(CharSequence content, Throwable error) {}

      @Override
      public void warn(Throwable error) {}

      @Override
      public void warn(CharSequence content) {}

      @Override
      public boolean isWarnEnabled() {
        return false;
      }

      @Override
      public boolean isInfoEnabled() {
        return false;
      }

      @Override
      public boolean isErrorEnabled() {
        return false;
      }

      @Override
      public boolean isDebugEnabled() {
        return false;
      }

      @Override
      public void info(CharSequence content, Throwable error) {}

      @Override
      public void info(Throwable error) {}

      @Override
      public void info(CharSequence content) {
        log.add(content.toString());
      }

      @Override
      public void error(CharSequence content, Throwable error) {}

      @Override
      public void error(Throwable error) {}

      @Override
      public void error(CharSequence content) {}

      @Override
      public void debug(CharSequence content, Throwable error) {}

      @Override
      public void debug(Throwable error) {}

      @Override
      public void debug(CharSequence content) {}
    });
    testee.version = "5.1.14-SNAPSHOT";
    testee.externalBuiltArtifacts = Arrays.asList("ch.ivyteam.ulc.base", "ch.ivyteam.ulc.extension",
            "ch.ivyteam.ivy.richdialog.components",
            "ch.ivyteam.ivy.designer.cm.ui", "ch.ivyteam.vn.feature", "ch.ivyteam.ulc.base.source",
            "ch.ivyteam.ulc.extension.source", "ch.ivyteam.ivy.richdialog.components.source");
    FileUtils.deleteDirectory(new File("testProject"));
    FileUtils.forceDeleteOnExit(new File("testProject"));
    FileUtils.copyDirectory(new File("src/test/projects/originalProject"), new File("testProject"));
  }

  @Test
  void testUpdateBundleManifestVersion() throws MojoExecutionException, IOException {
    testee.execute();
    compareManifest();
    compareLog();
  }

  @Test
  void testUpdatePomVersion() throws MojoExecutionException, IOException {
    testee.execute();
    comparePom();
    compareLog();
  }

  @Test
  void testUpdateFeatureVersion() throws MojoExecutionException, IOException {
    testee.execute();
    compareFeature();
    compareLog();
  }

  @Test
  void testUpdateProductVersion() throws MojoExecutionException, IOException {
    testee.execute();
    compareProduct();
    compareLog();
  }

  private void compareFeature() throws IOException {
    String testeeManifest = Files.readString(new File("testProject/feature.xml").toPath());
    String referenceManifest = Files.readString(new File("src/test/projects/referenceProject/feature.xml").toPath());
    assertThat(testeeManifest).isEqualTo(referenceManifest);
  }

  private void comparePom() throws IOException {
    String testeeManifest = Files.readString(POM_FILE.toPath());
    String referenceManifest = Files.readString(new File("src/test/projects/referenceProject/pom.xml").toPath());
    assertThat(testeeManifest).isEqualTo(referenceManifest);
  }

  private void compareManifest() throws IOException {
    String testeeManifest = Files.readString(new File("testProject/META-INF/MANIFEST.MF").toPath());
    String referenceManifest = Files.readString(new File("src/test/projects/referenceProject/META-INF/MANIFEST.MF").toPath());
    assertThat(StringUtils.deleteWhitespace(testeeManifest))
            .isEqualTo(StringUtils.deleteWhitespace(referenceManifest));
  }

  private void compareLog() throws IOException {
    List<String> referenceLog = Files.readAllLines(new File("src/test/projects/referenceProject/log.txt").toPath());
    List<String> cleanedReferenceLog = new ArrayList<>();
    for (String line : referenceLog) {
      line = StringUtils.replace(line, "C:\\dev\\maven-plugin\\maven-plugin\\testProject\\",
              POM_FILE.getParentFile().getAbsolutePath() + "\\");
      line = StringUtils.replace(line, "\\", File.separator);
      cleanedReferenceLog.add(line);
    }
    assertThat(cleanedReferenceLog).containsOnly(log.toArray(new String[log.size()]));
  }

  private void compareProduct() throws IOException {
    String testeeProduct = Files.readString(new File("testProject/Designer.product").toPath());
    String referenceProduct = Files.readString(new File("src/test/projects/referenceProject/Designer.product").toPath());
    assertThat(testeeProduct).isEqualTo(referenceProduct);
  }
}
