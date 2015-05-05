package ch.ivyteam.ivy.maven;

import static org.fest.assertions.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;

import org.apache.maven.model.FileSet;
import org.apache.maven.plugin.testing.MojoRule;
import org.junit.Rule;
import org.junit.Test;

public class TestEclipseHelpMojo
{
  @Rule
  public MojoRule rule = new MojoRule();

  @Test
  public void modifyContext() throws Exception
  {
    File project =  new File("src/test/resources");
    EclipseHelpMojo mojo = (EclipseHelpMojo) rule.lookupConfiguredMojo(project, "modify-help-context");
    mojo.sourceContext = new File(project, "context.xml");
    mojo.targetContext = Files.createTempFile("generatedContext", ".xml").toFile();
    
    FileSet helpFs = new FileSet();
    helpFs.setDirectory(project.getAbsolutePath());
    helpFs.setIncludes(Arrays.asList("DesignerGuide/**/*.html"));
    mojo.helpFiles = helpFs;
    
    mojo.execute();
    
    assertThat(mojo.targetContext)
      .hasContentEqualTo(new File(project, "expectedContext.xml"));
  }

}
