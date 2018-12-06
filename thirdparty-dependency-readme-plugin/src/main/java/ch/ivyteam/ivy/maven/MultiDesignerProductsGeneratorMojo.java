package ch.ivyteam.ivy.maven;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.FileSet;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;

/**
 * Writes the ReadMe.html for multiple products of different environments (win, linux, mac).
 * 
 * @author rew
 * @since 07.03.2016
 */
@Mojo(name=MultiDesignerProductsGeneratorMojo.GOAL, requiresProject=false)
public class MultiDesignerProductsGeneratorMojo extends AbstractMojo
{
  public static final String GOAL = "generate-multi-designer-readme";
  
  @Parameter(property="readme.templates")
  FileSet readMeTemplates;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException
  {
    for(File readme : toFiles(readMeTemplates))
    {
      generateReadme(readme);
    }
  }

  private static List<File> toFiles(FileSet fs) throws MojoExecutionException
  {
    try
    {
      return FileUtils.getFiles(
              new File(fs.getDirectory()), 
              StringUtils.join(fs.getIncludes(),","), 
              StringUtils.join(fs.getExcludes(),","));
    }
    catch (IOException ex)
    {
      throw new MojoExecutionException("Failed to resolve readme templates from '"+fs+"'", ex);
    }
  }

  private static void generateReadme(File readMe) throws MojoExecutionException, MojoFailureException
  {
    OSGiReadmeGeneratorMojo generator = new OSGiReadmeGeneratorMojo();
    File rootDesignerDir = readMe.getParentFile().getParentFile().getParentFile();
    generator.featuresDir = new File(rootDesignerDir, "features");
    generator.pluginsDir = new File(rootDesignerDir, "plugins");
    generator.webappLibsDir = new File(rootDesignerDir, "webapps/ivy/WEB-INF/lib");
    
    generator.templateFile = readMe;
    generator.outputFile = readMe;
    generator.execute();
  }
  
}
