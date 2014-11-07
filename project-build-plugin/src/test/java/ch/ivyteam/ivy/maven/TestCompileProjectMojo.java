package ch.ivyteam.ivy.maven;

import static org.fest.assertions.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.DefaultArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.plugin.internal.DefaultLegacySupport;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import ch.ivyteam.ivy.maven.log.LogCollector;

public class TestCompileProjectMojo
{
  private static final String LOCAL_REPOSITORY = getLocalRepoPath();
  private static final String CACHE_DIR = LOCAL_REPOSITORY + "/.cache/ivy";
  
  private static String getLocalRepoPath()
  {
    String locaRepoGlobalProperty = System.getProperty("maven.repo.local");
    if (locaRepoGlobalProperty != null)
    {
      return locaRepoGlobalProperty;
    }
    
    StringBuilder defaultHomePath = new StringBuilder(SystemUtils.USER_HOME)
      .append(File.separatorChar).append(".m2")
      .append(File.separatorChar).append("repository");
    return defaultHomePath.toString();
  }
  
  @Rule
  public ProjectMojoRule<CompileProjectMojo> rule = new ProjectMojoRule<CompileProjectMojo>(
          new File("src/test/resources/base"), CompileProjectMojo.GOAL){
    @Override
    protected void before() throws Throwable {
      super.before();
      
      getMojo().localRepository = provideLocalRepository();
    }

    /**
     * maven-plugin-testing-harness can not inject local repositories (though the real runtime supports it).
     * and the default stubs have no sufficient implementation of getPath(): 
     * @see "http://maven.apache.org/plugin-testing/maven-plugin-testing-harness/examples/repositories.html"
     */
    private ArtifactRepository provideLocalRepository() throws IllegalAccessException
    {
      DefaultArtifactRepositoryFactory factory = new DefaultArtifactRepositoryFactory();
      setVariableValueToObject(factory, "factory", new org.apache.maven.repository.legacy.repository.DefaultArtifactRepositoryFactory());
      
      LegacySupport legacySupport = new DefaultLegacySupport();
      setVariableValueToObject(factory, "legacySupport", legacySupport);
      
      ArtifactRepository localRepository = factory.createArtifactRepository("local", "http://localhost", 
              new DefaultRepositoryLayout(), new ArtifactRepositoryPolicy(), new ArtifactRepositoryPolicy());
      
      setVariableValueToObject(localRepository, "basedir", LOCAL_REPOSITORY);
      
      return localRepository;
    }
  };
  
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
    mojo.engineCacheDirectory = new File(CACHE_DIR);
    mojo.execute();
    
    assertThat(findFiles(dataClassDir, "java")).hasSize(2);
    assertThat(findFiles(wsProcDir, "java")).hasSize(1);
    assertThat(findFiles(classDir, "class")).hasSize(5);
  }
  
  @Test
  public void testLogging() throws Exception
  {
    LogCollector logCollector = new LogCollector();
    CompileProjectMojo mojo = rule.getMojo();
    mojo.setLog(logCollector);
    mojo.engineCacheDirectory = new File(CACHE_DIR);
    mojo.execute();
    assertThat(logCollector.getLogs())
      .as("Logs from engine must be forwarded")
      .isNotEmpty();
    
    System.out.println(logCollector.getWarnings().get(0));
    assertThat(logCollector.getWarnings()).isEmpty();
    assertThat(logCollector.getErrors()).isEmpty();
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
