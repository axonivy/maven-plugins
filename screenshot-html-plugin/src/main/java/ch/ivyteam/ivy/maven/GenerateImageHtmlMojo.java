package ch.ivyteam.ivy.maven;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

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

  /** Html template to use around generated images, add {generated.img.tag.location} inside template to define location for generated <img> tags */
  @Parameter(property = "html.template", required = true)
  File htmlTemplate;
  
  /** Filename of output html, default overview.html */
  @Parameter(property = "html.output.name", defaultValue = "overview.html", required = false)
  String outputName;
  
  /** Images to include in generated html, this directory can have sub-directories */
  @Parameter(property="include.imgs")
  FileSet images;
  
  @Parameter(property = "outputDirectory", defaultValue = "${project.build.directory}", required = true)
  File outputDirectory;
  
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException
  {
    List<File> imageFiles = CompareImagesMojo.toFiles(images);
    Collections.sort(imageFiles);
    getLog().info("Generating html with " + imageFiles.size()+" images from " + images.getDirectory());
    
    String templateString = readTemplate();
    String outputHtml = new HtmlGenerator(templateString, imageFiles, Paths.get(images.getDirectory()), getLog()).generate();
    writeHtmlFile(outputHtml);
  }

  private String readTemplate()
  {
    StringBuilder templateBuilder = new StringBuilder();
    try (Stream<String> templateStream = Files.lines(htmlTemplate.toPath(), StandardCharsets.UTF_8))
    {
      templateStream.forEach(line -> templateBuilder.append(line + System.lineSeparator()));
    }
    catch (IOException e)
    {
      getLog().error("Failed reading template " + htmlTemplate.toPath() + " " + e);
    }
    return templateBuilder.toString();
  }
  
  private void writeHtmlFile(String outputHtml)
  {
    getLog().debug(outputHtml);
    try
    {
      if (!outputDirectory.exists())
      {
        outputDirectory.mkdirs();
      }
      Files.write(new File(outputDirectory.toPath() + File.separator + outputName).toPath(), outputHtml.getBytes());
    }
    catch (IOException ex)
    {
      getLog().error("Could not generate file in " + outputDirectory + " " + ex);
    }
  }

}
