package ch.ivyteam.ivy.maven;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.apache.maven.model.FileSet;
import org.junit.Test;

public class TestMergeFilesMojo
{
  @Test
  public void runMavenTestWithRealProject() throws Exception
  {
    MergeFilesMojo mojo = new MergeFilesMojo();
    mojo.inputFiles = new FileSet();
    mojo.inputFiles.setDirectory("src/test/resources");
    mojo.outputFile = new File("target/test.txt");
    mojo.inputFiles.addInclude("file*.txt");
    mojo.ascending = false;
    mojo.execute();

    assertThat(mojo.outputFile).isNotNull();
  }
}
