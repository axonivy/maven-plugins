package ch.ivyteam.ivy.maven;

import java.io.File;
import java.io.IOException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.codehaus.plexus.archiver.zip.ZipArchiver;

/**
 * @author Reguel Wermelinger
 * @since 03.10.2014
 */
@Mojo(name = "pack-iar")
public class IarPackagingMojo extends AbstractMojo
{
  @Parameter(property = "project", required = true, readonly = true)
  MavenProject project;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException
  {
    String iarName = project.getArtifactId() + "-" + project.getVersion() + "." + project.getPackaging();
    File iar = new File(project.getBuild().getDirectory(), iarName);
    createIvyArchive(project.getBasedir(), iar);

    Artifact artifact = project.getArtifact();
    artifact.setFile(iar);
    project.setArtifact(artifact);
    getLog().info("Attached " + artifact.toString() + ".");
  }

  private void createIvyArchive(File sourceDir, File targetIar) throws MojoExecutionException
  {
    ZipArchiver archiver = new ZipArchiver();
    archiver.setDestFile(targetIar);
    DefaultFileSet fileSet = new DefaultFileSet();
    fileSet.setDirectory(sourceDir);
    fileSet.setIncludingEmptyDirectories(false);
    fileSet.setIncludes(new String[] {"**/*"});
    fileSet.setExcludes(new String[] {"target/**/*", "pom.xml"});
    fileSet.setUsingDefaultExcludes(false);
    archiver.addFileSet(fileSet);
    try
    {
      archiver.createArchive();
    }
    catch (ArchiverException | IOException ex)
    {
      throw new MojoExecutionException("Failed to create IAR: " + targetIar.getAbsolutePath(), ex);
    }
  }

}
