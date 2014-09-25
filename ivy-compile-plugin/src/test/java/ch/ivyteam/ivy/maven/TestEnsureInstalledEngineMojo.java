package ch.ivyteam.ivy.maven;

import static org.fest.assertions.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;

import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.project.MavenProject;
import org.junit.Rule;
import org.junit.Test;

public class TestEnsureInstalledEngineMojo
{

  @Rule
  public MojoRule rule = new MojoRule();
  
  @Test
  public void testMojoWithPomConfiguration() throws Exception
  {
    MavenProject project = rule.readMavenProject(new File("src/test/resources"));
    EnsureInstalledEngineMojo mojo = (EnsureInstalledEngineMojo) rule.lookupConfiguredMojo(project, "ensureInstalledEngine");
    
    mojo.engineDirectory = Files.createTempDirectory("tmpEngine").toFile();
    mojo.engineDirectory.deleteOnExit();
    assertThat(mojo.engineDirectory).isDirectory();
    assertThat(mojo.engineDirectory.listFiles()).isEmpty();
    
    mojo.execute();
    assertThat(mojo.engineDirectory.listFiles()).isNotEmpty();
  }
  
}
