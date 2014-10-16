package ch.ivyteam.ivy.maven;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;
import static org.fest.assertions.api.Assertions.failBecauseExceptionWasNotThrown;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;

import mockit.Mock;
import mockit.MockUp;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.project.MavenProject;
import org.junit.Rule;
import org.junit.Test;

import ch.ivyteam.ivy.maven.EnsureInstalledEngineMojo.EngineDownloader;

public class TestEnsureInstalledEngineMojo
{
  private EnsureInstalledEngineMojo mojo;

  @Rule
  public MojoRule rule = new MojoRule()
  {
    @Override
    protected void before() throws Throwable 
    {
      MavenProject project = rule.readMavenProject(new File("src/test/resources"));
      mojo = (EnsureInstalledEngineMojo) rule.lookupConfiguredMojo(project, "ensureInstalledEngine");
    }
  };
  
  @Test
  public void testEngineDownload_ifNotExisting() throws Exception
  {
    mojo.engineDirectory = Files.createTempDirectory("tmpEngine").toFile();
    mojo.engineDirectory.deleteOnExit();
    assertThat(mojo.engineDirectory).isDirectory();
    assertThat(mojo.engineDirectory.listFiles()).isEmpty();
    
    mojo.ivyVersion = "5.1.0";
    mojo.autoInstallEngine = true;
    mojo.engineDownloadUrl = new MockedIvyEngineDownloadUrl("5.1.0").getMockInstance();
    
    mojo.execute();
    assertThat(mojo.engineDirectory.listFiles()).isNotEmpty();
  }
  
  @Test
  public void testEngineDownload_validatesDownloadedVersion() throws Exception
  {
    mojo.engineDirectory = Files.createTempDirectory("tmpEngine").toFile();
    mojo.engineDirectory.deleteOnExit();
    
    mojo.ivyVersion = "5.1.0";
    mojo.autoInstallEngine = true;
    mojo.engineDownloadUrl = new MockedIvyEngineDownloadUrl("6.0.0").getMockInstance();
    
    try
    {
      mojo.execute();
      failBecauseExceptionWasNotThrown(MojoExecutionException.class);
    }
    catch(MojoExecutionException ex)
    {
      assertThat(ex).hasMessageStartingWith("Automatic installation of an ivyEngine failed.");
    }
  }
  
  @Test
  public void testEngineDownload_canDisableAutoDownload() throws Exception
  {
    mojo.engineDirectory = Files.createTempDirectory("tmpEngine").toFile();
    mojo.engineDirectory.deleteOnExit();
    
    mojo.ivyVersion = "5.1.0";
    mojo.autoInstallEngine = false;
    
    try
    {
      mojo.execute();
      failBecauseExceptionWasNotThrown(MojoExecutionException.class);
    }
    catch(MojoExecutionException ex)
    {
      assertThat(ex).hasMessageContaining("no valid ivy Engine is available");
    }
  }
  
  private static class MockedIvyEngineDownloadUrl extends MockUp<java.net.URL>
  {
    private String ivyVersion;

    private MockedIvyEngineDownloadUrl(String ivyVersion)
    {
      this.ivyVersion = ivyVersion;
    }
    
    @Mock
    public InputStream openStream()
    {
      try
      {
        return new FileInputStream(createFakeZip());
      }
      catch (Exception ex)
      {
        fail("Mock URL error", ex);
        return null;
      }
    }

    private File createFakeZip() throws IOException, ZipException
    {
      File zipDir = Files.createTempDirectory("zip").toFile();
      zipDir.deleteOnExit();
      File fakeLibToDeclareVersion = new File(zipDir, "lib/ivy/ch.ivyteam.fake-"+ivyVersion+"-server.jar");
      fakeLibToDeclareVersion.getParentFile().mkdirs();
      fakeLibToDeclareVersion.createNewFile();
      File zipFile = new File(zipDir, "fake.zip");
      ZipFile zip = new ZipFile(zipFile);
      zip.createZipFileFromFolder(new File(zipDir, "lib"), new ZipParameters(), false, 0);
      return zipFile;
    }
    
    @Mock
    public String toExternalForm()
    {
      return "http://localhost/fakeEngine.zip";
    }
  }
  
  @Test
  public void testEngineLinkFinder_absolute() throws Exception
  {
    mojo.ivyVersion = "5.1.0";
    mojo.osArchitecture = "Windows_x86";
    assertThat(findLink("<a href=\"http://developer.axonivy.com/download/5.1.0/AxonIvyEngine5.1.0.46949_Windows_x86.zip\">the latest engine</a>"))
      .isEqualTo("http://developer.axonivy.com/download/5.1.0/AxonIvyEngine5.1.0.46949_Windows_x86.zip");
  }
  
  @Test
  public void testEngineLinkFinder_relative() throws Exception
  {
    mojo.ivyVersion = "5.1.0";
    mojo.osArchitecture = "Windows_x86";
    mojo.engineListPageUrl = new URL("http://localhost/");
    assertThat(findLink("<a href=\"5.1.0/AxonIvyEngine5.1.0.46949_Windows_x86.zip\">the latest engine</a>"))
      .isEqualTo("http://localhost/5.1.0/AxonIvyEngine5.1.0.46949_Windows_x86.zip");
  }
  
  @Test
  public void testEngineLinkFinder_wrongVersion() throws Exception
  {
    mojo.ivyVersion = "6.0.0";
    mojo.osArchitecture = "Windows_x86";
    try
    {
      findLink("<a href=\"5.1.0/AxonIvyEngine5.1.0.46949_Windows_x86.zip\">the latest engine</a>");
      failBecauseExceptionWasNotThrown(MojoExecutionException.class);
    }
    catch(MojoExecutionException ex)
    {
      assertThat(ex).hasMessageStartingWith("Could not find a link to engine in version '6.0.0'");
    }
  }
  
  @Test
  public void testEngineLinkFinder_wrongArchitecture() throws Exception
  {
    mojo.ivyVersion = "5.1.0";
    mojo.osArchitecture = "Linux_x86";
    try
    {
      findLink("<a href=\"5.1.0/AxonIvyEngine5.1.0.46949_Windows_x86.zip\">the latest engine</a>");
      failBecauseExceptionWasNotThrown(MojoExecutionException.class);
    }
    catch(MojoExecutionException ex)
    {
      assertThat(ex).hasMessageStartingWith("Could not find a link to engine in version '5.1.0'");
    }
  }
  
  @Test
  public void testEngineLinkFinder_multipleLinks() throws Exception
  {
    mojo.ivyVersion = "5.1.0";
    mojo.osArchitecture = "Linux_x86";
    mojo.engineListPageUrl = new URL("http://localhost/");

    assertThat(findLink(
            "<a href=\"5.1.0/AxonIvyEngine5.1.0.46949_Windows_x86.zip\">the latest engine</a>" // windows
          + "<a href=\"5.1.0/AxonIvyEngine5.1.0.46949_Linux_x86.zip\">the latest engine</a>")) // linux
             .isEqualTo("http://localhost/5.1.0/AxonIvyEngine5.1.0.46949_Linux_x86.zip");
  }
  
  private String findLink(String html) throws MojoExecutionException, MalformedURLException
  {
    EngineDownloader engineDownloader = mojo.new EngineDownloader();
    return engineDownloader.findEngineDownloadUrl(IOUtils.toInputStream(html)).toExternalForm();
  }
  
}
