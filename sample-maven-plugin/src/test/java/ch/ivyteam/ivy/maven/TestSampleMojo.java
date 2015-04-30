package ch.ivyteam.ivy.maven;

import java.io.File;
import java.nio.file.Files;

import org.apache.maven.plugin.testing.MojoRule;
import org.junit.Rule;
import org.junit.Test;

public class TestSampleMojo
{
  @Rule
  public MojoRule rule = new MojoRule();

  @Test
  public void runMavenTestWithRealProject() throws Exception
  {
    File pom =  new File("src/test/resources");
    SampleMojo mojo = (SampleMojo) rule.lookupConfiguredMojo(pom, "sample");
    mojo.buildApplicationDirectory = Files.createTempDirectory("myTempDir").toFile();
    mojo.execute();
  }

}
