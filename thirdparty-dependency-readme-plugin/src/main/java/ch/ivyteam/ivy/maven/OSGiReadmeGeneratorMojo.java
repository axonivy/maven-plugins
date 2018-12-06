package ch.ivyteam.ivy.maven;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import ch.ivyteam.ivy.generator.Eclipse3rdPartyFeatureReadmeGenerator;
import ch.ivyteam.ivy.generator.Eclipse3rdPartyJarReadmeGenerator;
import ch.ivyteam.ivy.generator.Server3rdPartyJarReadmeGenerator;


@Mojo(name=OSGiReadmeGeneratorMojo.GOAL, requiresProject=false)
public class OSGiReadmeGeneratorMojo extends AbstractReadmeGeneratorMojo
{
  public static final String GOAL = "generate-osgi-readme";
  
  @Parameter(defaultValue="${basedir}", property="readme.osgi.features.dir")
  File featuresDir;

  @Parameter(defaultValue="${basedir}", property="readme.osgi.plugins.dir")
  File pluginsDir;
  
  @Parameter(defaultValue="${basedir}", property="readme.osgi.webapplibs.dir")
  File webappLibsDir;
  
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException
  {
    getLog().info("generating readme html artifacts for osgi application");
    
    getLog().info("-- features dir: " + featuresDir);
    getLog().info("-- plugins dir: " + pluginsDir);
    getLog().info("-- webapp libs dir: " + webappLibsDir);
    
    Map<String, String> htmlTokens = new HashMap<>();
    try
    {
      htmlTokens.put("eclipseFeatures", new Eclipse3rdPartyFeatureReadmeGenerator(getLog()).generate(featuresDir));
      htmlTokens.put("eclipsePlugins", new Eclipse3rdPartyJarReadmeGenerator(getLog()).generate(pluginsDir));
      htmlTokens.put("htmlDialogLibs", new Server3rdPartyJarReadmeGenerator().printLibraryTable(webappLibsDir));
    }
    catch (Exception ex)
    {
      throw new MojoExecutionException("Failed to generate readme for osgi application", ex);
    }
    writeReadme(htmlTokens);
  }
}
