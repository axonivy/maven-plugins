package ch.ivyteam.ivy.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import org.apache.maven.model.FileSet;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;


@Mojo(name=GenerateImageHtmlMojo.GOAL, requiresProject = false)
public class GenerateImageHtmlMojo extends AbstractMojo
{
  static final String GOAL = "generate-html";

  /** Images to include in generated html, this directory can have sub-directories */
  @Parameter(property="include.imgs")
  FileSet images;
  
  @Parameter(defaultValue = "${project.build.directory}", property = "outputDirectory", required = true)
  private File outputDirectory;
  
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException
  {
    List<File> imageFiles = CompareImagesMojo.toFiles(images);
    Collections.sort(imageFiles);
    getLog().info("Generating html with " + imageFiles.size()+" images from " + images.getDirectory());
    
    String html = new HtmlGenerator(imageFiles, Paths.get(images.getDirectory()), getLog()).generate();
    getLog().debug(html);
    
    try
    {
      if (!outputDirectory.exists())
      {
        outputDirectory.mkdirs();
      }
      Files.write(new File(outputDirectory.toPath() + File.separator + "overview.html").toPath(), html.getBytes());
    }
    catch (IOException ex)
    {
      getLog().error("Could not generate file in " + outputDirectory + " " + ex);
    }
  }

}
