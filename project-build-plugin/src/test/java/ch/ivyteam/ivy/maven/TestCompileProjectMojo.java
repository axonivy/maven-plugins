package ch.ivyteam.ivy.maven;

import static org.fest.assertions.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class TestCompileProjectMojo
{
  @Rule
  public ProjectMojoRule<CompileProjectMojo> rule = new ProjectMojoRule<CompileProjectMojo>(
          new File("src/test/resources/base"), CompileProjectMojo.GOAL);
  
  @Test
  @Ignore("needs unpacked engine and is more like an integration test...")
  public void buildWithExistingProject() throws Exception
  {
    CompileProjectMojo mojo = rule.getMojo();
    
    File dataClassDir = new File(mojo.project.getBasedir(), "src_dataClasses");
    File wsProcDir = new File(mojo.project.getBasedir(), "src_wsproc");
    File classDir = new File(mojo.project.getBasedir(), "classes");
    
    assertThat(findFiles(wsProcDir, "java")).isEmpty();
    assertThat(findFiles(dataClassDir, "java")).isEmpty();
    assertThat(findFiles(classDir, "class")).isEmpty();
    
    mojo.buildApplicationDirectory = Files.createTempDirectory("MyBuildApplication").toFile();
    mojo.engineCacheDirectory = new File("C:/Users/rew.SORECO/.m2/repository/.cache/ivy");
    mojo.execute();
    
    assertThat(findFiles(dataClassDir, "java")).hasSize(2);
    assertThat(findFiles(wsProcDir, "java")).hasSize(1);
    assertThat(findFiles(classDir, "class")).hasSize(5);
  }
  
  private static Collection<File> findFiles(File dir, String fileExtension)
  {
    if (!dir.exists())
    {
      return Collections.emptyList();
    }
    return FileUtils.listFiles(dir, new String[]{fileExtension}, true);
  }

}
