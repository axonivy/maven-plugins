package ch.ivyteam.ivy.maven;

import java.io.File;
import java.net.URLClassLoader;
import java.util.Collections;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import ch.ivyteam.ivy.maven.engine.EngineClassLoaderFactory;
import ch.ivyteam.ivy.maven.engine.MavenProjectBuilderProxy;

@Mojo(name="compileProject", defaultPhase = LifecyclePhase.COMPILE)
@Execute(lifecycle="iarcycle", phase=LifecyclePhase.COMPILE)
public class CompileProjectMojo extends AbstractEngineMojo
{
  
  // read IAR dependencies!!!
  
  /**
   * Home application where the project to build and its dependencies will be temporary deployed. 
   */
  @Parameter(defaultValue = "${project.build.directory}/ivyBuildApp")
  File buildApplicationDirectory;
  
  /** 
   * The ivy project to build by this mojo.
   */
  @Parameter(defaultValue = "${basedir}")
  File projectToBuild;

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
      builder.execute(projectToBuild, Collections.<File>emptyList(), engineDirectory.getAbsoluteFile());
    }
    catch (Exception ex)
    {
      throw new MojoExecutionException("Failed to compile project '"+projectToBuild+"'.", ex);
    }
  }

}
