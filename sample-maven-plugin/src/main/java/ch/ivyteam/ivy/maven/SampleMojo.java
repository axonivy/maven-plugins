package ch.ivyteam.ivy.maven;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;


@Mojo(name="sample")
public class SampleMojo extends AbstractMojo
{
  @Parameter(property = "project", required = true, readonly = true)
  MavenProject project;
  
  @Parameter(defaultValue = "${project.build.directory}/ivyBuildApp")
  File buildApplicationDirectory;
  
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException
  {
    getLog().info("running "+SampleMojo.class.getSimpleName()+" with parameter 'buildApplicationDirectory'="+buildApplicationDirectory);
    getLog().error("not yet implemented");
  }

}
