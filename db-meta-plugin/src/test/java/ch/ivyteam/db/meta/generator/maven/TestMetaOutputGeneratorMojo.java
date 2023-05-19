package ch.ivyteam.db.meta.generator.maven;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Date;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.assertj.core.api.AbstractFileAssert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import ch.ivyteam.db.meta.generator.internal.HtmlDocGenerator;
import ch.ivyteam.db.meta.generator.internal.oracle.OracleSqlScriptGenerator;

public class TestMetaOutputGeneratorMojo {

  private static final long RECENT_TIME_STAMP = new Date().getTime() - 10000L;
  private static final long OLD_TIME_STAMP = 0L;
  @Rule
  public ProjectMojoRule<MetaOutputGeneratorMojo> mojoRule = new ProjectMojoRule<>(
          new File("src/test/resources/base"), MetaOutputGeneratorMojo.GOAL);
  private MetaOutputGeneratorMojo mojo;

  @Before
  public void before() throws IllegalAccessException {
    mojo = mojoRule.getMojo();
    mojoRule.setVariableValueToObject(mojo, "outputDirectory",
            new File(mojoRule.getProject().getBasedir(), "generated"));
    mojoRule.setVariableValueToObject(mojo, "inputDirectory",
            new File(mojoRule.getProject().getBasedir(), "meta"));
  }

  @Test
  public void executeWithOutputDir()
          throws IllegalAccessException, MojoExecutionException, MojoFailureException {
    mojoRule.setVariableValueToObject(mojo, "generatorClass", HtmlDocGenerator.class.getName());
    assertThat(getProjectFile("generated")).doesNotExist();
    mojo.execute();
    assertThatProjectFile("generated/IWA_ClusterHost.html").exists();
  }

  @Test
  public void directoryNotUpToDate()
          throws IllegalAccessException, MojoExecutionException, MojoFailureException, IOException {
    mojoRule.setVariableValueToObject(mojo, "generatorClass", HtmlDocGenerator.class.getName());
    File file = getProjectFile("generated/blah.html");
    Files.createDirectories(file.getParentFile().toPath());
    Files.createFile(file.toPath());
    file.setLastModified(OLD_TIME_STAMP);
    mojo.execute();
    assertThatProjectFile("generated/IWA_ClusterHost.html").exists();
  }

  @Test
  public void executeWithFile() throws IllegalAccessException, MojoExecutionException, MojoFailureException {
    mojoRule.setVariableValueToObject(mojo, "generatorClass", OracleSqlScriptGenerator.class.getName());
    mojoRule.setVariableValueToObject(mojo, "outputFile", "oracle.sql");
    mojo.execute();
    assertThatProjectFile("generated/oracle.sql").exists();
  }

  @Test
  public void fileNotUpToDate()
          throws IllegalAccessException, MojoExecutionException, MojoFailureException, IOException {
    mojoRule.setVariableValueToObject(mojo, "generatorClass", OracleSqlScriptGenerator.class.getName());
    mojoRule.setVariableValueToObject(mojo, "outputFile", "oracle.sql");
    File sqlFile = getProjectFile("generated/oracle.sql");
    Files.createDirectories(sqlFile.getParentFile().toPath());
    Files.createFile(sqlFile.toPath());
    sqlFile.setLastModified(OLD_TIME_STAMP);
    mojo.execute();
    assertThat(getProjectFile("generated/oracle.sql").lastModified()).isGreaterThan(RECENT_TIME_STAMP);
  }

  private AbstractFileAssert<?> assertThatProjectFile(String path) {
    return assertThat(getProjectFile(path));
  }

  private File getProjectFile(String path) {
    return new File(mojoRule.getProject().getBasedir(), path);
  }
}
