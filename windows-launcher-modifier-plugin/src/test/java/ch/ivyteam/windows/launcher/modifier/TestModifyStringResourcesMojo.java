package ch.ivyteam.windows.launcher.modifier;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Arrays;

import org.apache.maven.model.FileSet;
import org.apache.maven.plugin.testing.MojoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TestModifyStringResourcesMojo
{
  @Rule
  public MojoRule rule = new MojoRule();
  
  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void execute() throws Exception
  {
    ModifyStringResourcesMojo mojo = new ModifyStringResourcesMojo();
    mojo.inputFiles = new FileSet();
    mojo.inputFiles.setDirectory("src/test/resources/testLaunchers");
    mojo.inputFiles.setIncludes(Arrays.asList("*.exe", "*.dll"));
    mojo.outputDirectory = tempFolder.getRoot();
    mojo.productVersion="7.2.0.62d6898";
    mojo.execute();
    assertThat(new File(tempFolder.getRoot(), "AxonIvyEngine.exe")).exists();
    assertThat(new File(tempFolder.getRoot(), "JVMLauncher.dll")).exists();
  }  
}
