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


@Mojo(name=DesignerReadmeGeneratorMojo.GOAL, requiresProject=false)
public class DesignerReadmeGeneratorMojo extends AbstractReadmeGeneratorMojo
{
  public static final String GOAL = "generate-designer-readme";
  
  @Parameter(defaultValue="${basedir}", property="readme.designer.dir")
  File designerDir;
  
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException
  {
    getLog().info("generating readme html artifacts for designer @ "+designerDir.getAbsolutePath());
    
    Map<String, String> htmlTokens = new HashMap<>();
    try
    {
      htmlTokens.put("eclipsePlugins", new Eclipse3rdPartyJarReadmeGenerator(getLog()).generate(designerDir));
      htmlTokens.put("eclipseFeatures", new Eclipse3rdPartyFeatureReadmeGenerator(getLog()).generate(designerDir));
    }
    catch (Exception ex)
    {
      throw new MojoExecutionException("Failed to generate readme for designer "+designerDir);
    }
    
    writeReadme(htmlTokens);
  }

}
