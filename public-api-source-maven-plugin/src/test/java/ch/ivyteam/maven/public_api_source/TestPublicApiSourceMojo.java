package ch.ivyteam.maven.public_api_source;

import static org.eclipse.tycho.PackagingType.TYPE_ECLIPSE_PLUGIN;
import static org.fest.assertions.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.DefaultArtifactKey;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.shared.BuildPropertiesParser;
import org.eclipse.tycho.source.AbstractSourceJarMojo;
import org.junit.Rule;
import org.junit.Test;
public class TestPublicApiSourceMojo
{
  @Rule
  public PublicApiSourceMojoRule rule = new PublicApiSourceMojoRule();
  
  @Test
  public void sourceJarCreated() throws Exception
  {
    PublicApiSourceMojo mojo = rule.getMojo();
    assertThat(mojo).isNotNull();
    mojo.execute();
    File basedir = mojo.getProject().getBasedir();
    File sourceJar = new File(basedir, "target/test-project-1.0.0-SNAPSHOT-sources.jar");
    assertThat(sourceJar).exists();
  }
  
  @Test
  public void sourceJarContents() throws Exception
  {
    PublicApiSourceMojo mojo = rule.getMojo();
    assertThat(mojo).isNotNull();
    mojo.execute();
    File basedir = mojo.getProject().getBasedir();
    File sourceJar = new File(basedir, "target/test-project-1.0.0-SNAPSHOT-sources.jar");
    ZipFile sourceJarZip = new ZipFile(sourceJar);
    try
    {
      assertThat(sourceJarZip.getEntry("ch/ivyteam/test/ClassWithPublicApi.java"))
        .as("ClassWithPublicApi.java must exist in source jar.").isNotNull();
      
      assertThat(sourceJarZip.getEntry("ch/ivyteam/another/test/AnotherClassWithPublicApi.java"))
        .as("AnotherClassWithPublicApi.java (from second source directory) must exist in source jar.").isNotNull();
      
      assertThat(sourceJarZip.getEntry("ch/ivyteam/test/ClassInIncludes.java"))
        .as("ClassInIncludes.java must exist in source jar.").isNotNull();
      
      assertThat(sourceJarZip.getEntry("ch/ivyteam/test/ClassWithoutSourceGeneration.java"))
        .as("ClassWithoutSourceGeneration.java must not exist in source jar.").isNull();
    }
    finally
    {
      sourceJarZip.close();
    }
  }

  @Test
  public void dontIncludePublicApiSourceProperty() throws Exception
  {
    PublicApiSourceMojo mojo = rule.getMojo();
    assertThat(mojo).isNotNull();
    
    mojo.includePublicApiSource = false;
    
    mojo.execute();
    File basedir = mojo.getProject().getBasedir();
    File sourceJar = new File(basedir, "target/test-project-1.0.0-SNAPSHOT-sources.jar");
    ZipFile sourceJarZip = new ZipFile(sourceJar);
    try
    {
      assertThat(sourceJarZip.getEntry("ch/ivyteam/test/ClassWithPublicApi.java"))
        .as("ClassWithPublicApi.java must not exist in source jar, since includePublicApiSource is set to false.")
        .isNull();
      
      assertThat(sourceJarZip.getEntry("ch/ivyteam/test/ClassInIncludes.java"))
        .as("ClassInIncludes.java must exist in source jar.").isNotNull();
      
      assertThat(sourceJarZip.getEntry("ch/ivyteam/test/ClassWithoutSourceGeneration.java"))
        .as("ClassWithoutSourceGeneration.java must not exist in source jar.").isNull();
    }
    finally
    {
      sourceJarZip.close();
    }
  }

  @Test
  public void onlyIncludePublicApiWhenIncludesPropertyEmpty() throws Exception
  {
    PublicApiSourceMojo mojo = rule.getMojo();
    assertThat(mojo).isNotNull();
    
    setIncludesPropertyToEmpty(mojo);
    
    mojo.execute();
    File basedir = mojo.getProject().getBasedir();
    File sourceJar = new File(basedir, "target/test-project-1.0.0-SNAPSHOT-sources.jar");
    ZipFile sourceJarZip = new ZipFile(sourceJar);
    try
    {
      assertThat(sourceJarZip.getEntry("ch/ivyteam/test/ClassWithPublicApi.java"))
        .as("ClassWithPublicApi.java must exist in source jar.").isNotNull();
      
      assertThat(sourceJarZip.getEntry("ch/ivyteam/another/test/AnotherClassWithPublicApi.java"))
        .as("AnotherClassWithPublicApi.java (from second source directory) must exist in source jar.").isNotNull();
      
      assertThat(sourceJarZip.getEntry("ch/ivyteam/test/ClassInIncludes.java"))
        .as("ClassInIncludes.java must not exist in source jar.").isNull();
      
      assertThat(sourceJarZip.getEntry("ch/ivyteam/test/ClassWithoutSourceGeneration.java"))
        .as("ClassWithoutSourceGeneration.java must not exist in source jar.").isNull();
      
    }
    finally
    {
      sourceJarZip.close();
    }
  }
  
  private void setIncludesPropertyToEmpty(PublicApiSourceMojo mojo) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
  {
    Field field = AbstractSourceJarMojo.class.getDeclaredField("includes");
    try
    {
      field.setAccessible(true);
      field.set(mojo, new String[0]);
    }
    finally
    {
      field.setAccessible(false);
    }
  }

  private class PublicApiSourceMojoRule extends MojoRule
  {
    File templateProjectDir = new File("src/test/resources/test-project-template");
    PublicApiSourceMojo mojo;
    File projectDir;
    
    public PublicApiSourceMojoRule()
    {
      super();
    }
    
    @Override
    protected void before() throws Throwable 
    {
      projectDir = Files.createTempDirectory("test-project").toFile();
      FileUtils.copyDirectory(templateProjectDir, projectDir);
      MavenProject project = readMavenProject(projectDir);
      addEclipseBundleInfoToProject(project);
      mojo = (PublicApiSourceMojo) lookupConfiguredMojo(project, PublicApiSourceMojo.GOAL);
    }

    /**
     * Since during testing, tycho is not set up completely, we add here all needed 
     * information to the maven project manually.
     *  
     * @see ch.ivyteam.maven.public_api_source.PublicApiSourceMojo#isRelevantProjectImpl(MavenProject, BuildPropertiesParser)
     * @see org.eclipse.tycho.core.osgitools.OsgiBundleProject#getArtifactKey(ReactorProject)
     * 
     * @param project
     */
    private void addEclipseBundleInfoToProject(MavenProject project)
    {
      project.setPackaging(TYPE_ECLIPSE_PLUGIN);
      
      DefaultArtifactKey artifactKey = new DefaultArtifactKey(TYPE_ECLIPSE_PLUGIN, "ch.ivyteam.testProject", "1.0.0.qualifier");
      project.setContextValue("org.eclipse.tycho.core.TychoConstants/osgiBundle/artifactKey", artifactKey);
    }

    
    @Override
    protected void after() 
    {
      try
      {
        FileUtils.deleteDirectory(projectDir);
      }
      catch (IOException ex)
      {
        throw new RuntimeException(ex);
      }  
    }

    public PublicApiSourceMojo getMojo()
    {
      return mojo;
    }
  }
}
