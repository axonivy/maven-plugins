package ch.ivyteam.ivy.maven;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;
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
  static final String REPLACE_TAG = "{generated.img.tag.location}";

  /** Custom html template to use around generated images, add {generated.img.tag.location} inside template to define location for generated <img> tags*/
  @Parameter(property = "html.template")
  File htmlTemplate;
  
  /** Images to include in generated html, this directory can have sub-directories */
  @Parameter(property="include.imgs")
  FileSet images;
  
  /** Output file location of html, default value ${project.build.directory}/overview.html */
  @Parameter(property = "outputFile", defaultValue = "${project.build.directory}/overview.html")
  File outputFile;
  
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException
  {
    List<File> imageFiles = CompareImagesMojo.toFiles(images);
    Collections.sort(imageFiles);
    
    getLog().info("Generating " + outputFile);
    
    String templateString = readTemplate();
    String outputHtml = new HtmlGenerator(templateString, imageFiles, Paths.get(images.getDirectory()), getLog()).generate();
    writeHtmlFile(outputHtml);
  }

  private String readTemplate()
  {
    try
    {
      if (htmlTemplate == null)
      {
        InputStream resourceAsStream = getClass().getResourceAsStream("template.html");
        return IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8);
      }
      
      return IOUtils.toString(new ByteArrayInputStream(Files.readAllBytes(htmlTemplate.toPath())), StandardCharsets.UTF_8);
    }
    catch (IOException ex)
    {
      getLog().error("Failed reading template " + htmlTemplate.toPath() + " " + ex);
    }
    return null;
  }
  
  private void writeHtmlFile(String outputHtml)
  {
    getLog().debug(outputHtml);
    try
    {
      File parentDir = new File(outputFile.getParent());
      if (!parentDir.exists())
      {
        parentDir.mkdirs();
      }
      Files.write(outputFile.toPath(), outputHtml.getBytes());
    }
    catch (IOException ex)
    {
      getLog().error("Could not generate file in " + outputFile + " " + ex);
    }
  }

}
