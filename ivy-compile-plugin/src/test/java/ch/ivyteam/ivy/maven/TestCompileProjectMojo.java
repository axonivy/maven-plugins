package ch.ivyteam.ivy.maven;

import static org.fest.assertions.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Test;

public class TestCompileProjectMojo
{
  
  @Test
  @Ignore("needs unpacked engine and project resources...")
  public void buildWithExistingProject() throws Exception
  {
    CompileProjectMojo mojo = new CompileProjectMojo();
    
    mojo.projectToBuild = new File("C:\\temp\\base");
    File classDir = new File(mojo.projectToBuild, "classes");
    FileUtils.deleteDirectory(classDir);
    
    mojo.buildApplicationDirectory = Files.createTempDirectory("MyBuildApplication").toFile();
    mojo.engineDirectory = new File("D:/temp");
    mojo.ivyVersion = "6.0.0";
    
    mojo.execute();
    
    assertThat(FileUtils.listFiles(classDir, new String[]{"class"}, true)).isNotEmpty();
  }

}
