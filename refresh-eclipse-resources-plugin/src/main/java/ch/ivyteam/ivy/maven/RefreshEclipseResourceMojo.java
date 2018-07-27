package ch.ivyteam.ivy.maven;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.sonatype.plexus.build.incremental.BuildContext;

@Mojo(name="refresh-eclipse-resource")
public class RefreshEclipseResourceMojo extends AbstractMojo
{
  @Component
  public BuildContext buildContext;
  
  @Parameter(name = "resource", required = true)
  File resource;
  
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException
  {
    getLog().info("Refresh " + resource.getAbsolutePath());
    buildContext.refresh(resource);
  }

}
