package ch.ivyteam.ivy.maven;

import java.io.File;
import java.io.IOException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.codehaus.plexus.archiver.zip.ZipArchiver;

@Mojo(name = "attach-artifact")
public class CustomPackagingMojo extends AbstractMojo
{
  @Parameter(property = "project", required = true, readonly = true)
  private MavenProject project;

  @Component
  private MavenProjectHelper projectHelper;

  @Parameter(property = "basedir")
  public String basedir;

  @Override
  public void execute()
          throws MojoExecutionException, MojoFailureException
  {
    File file = new File(basedir + "/target/",
              project.getArtifactId() + "-" +
                      project.getVersion() + "." +
                      project.getPackaging());
    createIvyArchive(file);

    Artifact artifact = project.getArtifact();
    artifact.setFile(file);
    project.setArtifact(artifact);
    getLog().info("Attached " + artifact.toString() + ".");
  }

  private void createIvyArchive(File file) throws MojoExecutionException
  {
    ZipArchiver archiver = new ZipArchiver();
    archiver.setDestFile(file);
    DefaultFileSet fileSet = new DefaultFileSet();
    fileSet.setDirectory(new File(basedir));
    fileSet.setIncludingEmptyDirectories(false);
    fileSet.setIncludes(new String[] {"**/*"});
    fileSet.setExcludes(new String[] {"target", "pom.xml"});
    archiver.addFileSet(fileSet);
    try
    {
      archiver.createArchive();
    }
    catch (ArchiverException | IOException ex)
    {
      throw new MojoExecutionException("failed to create IAR: " + file.getAbsolutePath(), ex);
    }
  }

}
