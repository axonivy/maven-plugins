package ch.ivyteam.ivy.maven;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.util.List;

import org.apache.maven.model.FileSet;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TestMergeFilesMojo
{
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();
  
  @Test
  public void runMavenTestWithRealProject() throws Exception
  {
    MergeFilesMojo mojo = new MergeFilesMojo();
    mojo.inputFiles = new FileSet();
    mojo.inputFiles.setDirectory("src/test/resources");
    mojo.outputFile = folder.newFile();
    mojo.inputFiles.addInclude("file*.txt");
    mojo.ascending = false;
    mojo.separator = "---";
    mojo.execute();

    List<String> lines = Files.readAllLines(mojo.outputFile.toPath());
    assertThat(lines).containsExactly(
            "This is file nr 3---This is file nr 2---This is file nr 1"
    );
    
    Files.delete(mojo.outputFile.toPath());
  }
}
