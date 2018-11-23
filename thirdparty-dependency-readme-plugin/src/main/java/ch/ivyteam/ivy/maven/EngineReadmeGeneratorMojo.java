package ch.ivyteam.ivy.maven;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import ch.ivyteam.ivy.generator.Eclipse3rdPartyJarReadmeGenerator;
import ch.ivyteam.ivy.generator.Server3rdPartyJarReadmeGenerator;


@Mojo(name=EngineReadmeGeneratorMojo.GOAL, requiresProject=false)
public class EngineReadmeGeneratorMojo extends AbstractReadmeGeneratorMojo
{
  public static final String GOAL = "generate-engine-readme";
  
  @Parameter(defaultValue="${basedir}", property="readme.engine.dir")
  File engineDir;
  
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException
  {
    getLog().info("generating readme html artifacts for engine @ "+engineDir.getAbsolutePath());
    Map<String, String> htmlTokens = new HashMap<>();
    htmlTokens.put("thirdPartyLibs", generatePlugins());
    htmlTokens.put("htmlDialogLibs", generate("webapps/ivy/WEB-INF/lib"));
    writeReadme(htmlTokens);
  }

  private String generate(String enginePath) throws MojoExecutionException
  {
    File libDirectory = new File(engineDir, enginePath);
    try
    {
      return new Server3rdPartyJarReadmeGenerator().printLibraryTable(libDirectory);
    }
    catch (Exception ex)
    {
      throw new MojoExecutionException(
              "Failed to generate libraries '"+libDirectory.getAbsolutePath()+
              "' as html", ex);
    }
  }

  private String generatePlugins() throws MojoExecutionException
  {
    File pluginsDir = new File(engineDir, "system" + File.separator + "plugins");
    try
    {
      return new Eclipse3rdPartyJarReadmeGenerator(getLog()).generate(pluginsDir);
    }
    catch (Exception ex)
    {
      throw new MojoExecutionException(
              "Failed to generate libraries '"+pluginsDir.getAbsolutePath()+
              "' as html", ex);
    }
  }

}
