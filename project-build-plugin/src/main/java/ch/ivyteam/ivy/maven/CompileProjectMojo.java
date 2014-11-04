package ch.ivyteam.ivy.maven;

import java.io.File;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import ch.ivyteam.ivy.maven.engine.EngineClassLoaderFactory;
import ch.ivyteam.ivy.maven.engine.MavenProjectBuilderProxy;

/**
 * Compiles an ivy Project with an ivyEngine.
 * 
 * @author Reguel Wermelinger
 * @since 04.11.2014
 */
@Mojo(name=CompileProjectMojo.GOAL, requiresDependencyResolution=ResolutionScope.COMPILE)
public class CompileProjectMojo extends AbstractEngineMojo
{
  public static final String GOAL = "compileProject";
  
  @Parameter(property = "project", required = true, readonly = true)
  MavenProject project;
  
  /**
   * Home application where the project to build and its dependencies will be temporary deployed. 
   */
  @Parameter(defaultValue = "${project.build.directory}/ivyBuildApp")
  File buildApplicationDirectory;
  
  private static MavenProjectBuilderProxy builder;
  
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException
  {
    getLog().info("Compiling ivy Project...");
    compileProject();
  }

  private void compileProject() throws MojoExecutionException
  {
    try
    {
      getMavenProjectBuilder().execute(project.getBasedir(), resolveIarDependencies(), getEngineDirectory().getAbsoluteFile());
    }
    catch (Exception ex)
    {
      throw new MojoExecutionException("Failed to compile project '"+project.getBasedir()+"'.", ex);
    }
  }
  
  private MavenProjectBuilderProxy getMavenProjectBuilder() throws Exception
  {
    if (builder == null)
    {
      URLClassLoader classLoader = EngineClassLoaderFactory.createEngineClassLoader(getEngineDirectory());
      builder = new MavenProjectBuilderProxy(classLoader, buildApplicationDirectory);
      return builder;
    }
    return builder;
  }
  
  private List<File> resolveIarDependencies()
  {
    Set<org.apache.maven.artifact.Artifact> dependencies = project.getDependencyArtifacts();
    if (dependencies == null)
    {
      return Collections.emptyList();
    }
    
    List<File> dependentIars = new ArrayList<>();
    for(org.apache.maven.artifact.Artifact artifact : dependencies)
    {
      if (artifact.getType().equals("iar"))
      {
        dependentIars.add(artifact.getFile());
      }
    }
    return dependentIars;
  }

}
