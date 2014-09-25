package ch.ivyteam.ivy.maven;

import java.io.File;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import ch.ivyteam.ivy.maven.engine.EngineClassLoaderFactory;
import ch.ivyteam.ivy.maven.engine.MavenProjectBuilderProxy;

@Mojo(name="compileProject", requiresDependencyResolution=ResolutionScope.COMPILE)
public class CompileProjectMojo extends AbstractEngineMojo
{
  @Parameter(property = "project", required = true, readonly = true)
  private MavenProject project;
  
  @Parameter(property = "basedir")
  File projectToBuild;
  
  /**
   * Home application where the project to build and its dependencies will be temporary deployed. 
   */
  @Parameter(defaultValue = "${project.build.directory}/ivyBuildApp")
  File buildApplicationDirectory;
  
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException
  {
    getLog().info("Compiling ivy Project...");
    compileProject();
  }

  private void compileProject() throws MojoExecutionException
  {
    try(URLClassLoader classLoader = EngineClassLoaderFactory.createEngineClassLoader(engineDirectory))
    {
      MavenProjectBuilderProxy builder = new MavenProjectBuilderProxy(classLoader, buildApplicationDirectory);
      builder.execute(projectToBuild, resolveIarDependencies(), engineDirectory.getAbsoluteFile());
    }
    catch (Exception ex)
    {
      throw new MojoExecutionException("Failed to compile project '"+projectToBuild+"'.", ex);
    }
  }
  
  private List<File> resolveIarDependencies()
  {
    List<File> dependentIars = new ArrayList<>();
    for(org.apache.maven.artifact.Artifact artifact : project.getDependencyArtifacts())
    {
      if (artifact.getType().equals("iar"))
      {
        dependentIars.add(artifact.getFile());
      }
    }
    return dependentIars;
  }

}
