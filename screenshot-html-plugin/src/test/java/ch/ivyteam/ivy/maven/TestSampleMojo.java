package ch.ivyteam.ivy.maven;

import java.io.File;

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
    CompareImagesMojo mojo = (CompareImagesMojo) rule.lookupConfiguredMojo(pom, CompareImagesMojo.GOAL);
    mojo.execute();
  }

}
