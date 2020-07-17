package ch.ivyteam.ivy.maven;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.model.FileSet;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;


@Mojo(name=GenerateImageHtmlMojo.GOAL, requiresProject = true)
public class GenerateImageHtmlMojo extends AbstractMojo
{
  static final String GOAL = "generate-html";
  static final String REPLACE_TAG_IMG = "{generated.img.tag.location}";
  static final String REPLACE_TAG_TARGET_PATH = "{artifact.target.path}";
  static final String REPLACE_TAG_JENKINS_URL = "{jenkins.job.url}";

  /** Custom html template to use around generated images, add {generated.img.tag.location} inside template 
   * to define location for generated <img> tags. If not defined a default template is used */
  @Parameter(property = "html.template")
  File htmlTemplate;
  
  /** The URL path to the artifact target folder which contains the reference images.
   * If you define your own template this parameter is accessible in the template via {artifact.target.path}*/
  @Parameter(property = "artifact.target.path", defaultValue = "artifact/target/")
  String artifactTargetPath;

  /** Images to include in generated html, this directory can have sub-directories */
  @Parameter(property="include.imgs")
  FileSet images;
  
  /** Output file location of html, default value ${project.build.directory}/overview.html */
  @Parameter(property = "outputFile", defaultValue = "${project.build.directory}/overview.html")
  File outputFile;

  @Parameter(property = "rootRelativePath", defaultValue = "")
  String rootRelativePath;
  
  @Parameter(property = "referenceRelativePath", defaultValue = "")
  String referenceRelativePath;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException
  {
    List<File> imageFiles = CompareImagesMojo.toFiles(images);
    Collections.sort(imageFiles);
    
    getLog().info("Generating " + outputFile);
    
    String templateString = readTemplate();
    Path rootDir = Paths.get(images.getDirectory());
    String outputHtml = new HtmlGenerator(templateString, artifactTargetPath, imageFiles, rootDir, getLog(), rootRelativePath, referenceRelativePath).generate();
    writeHtmlFile(outputHtml);
  }

  private String readTemplate() throws MojoExecutionException
  {
    try
    {
      return IOUtils.toString(getTemplateStream(), StandardCharsets.UTF_8);
    }
    catch (IOException ex)
    {
      throw new MojoExecutionException("Failed reading template " + htmlTemplate.toPath() + " " + ex);
    }
  }
  
  private InputStream getTemplateStream() throws IOException
  {
    if (htmlTemplate == null)
    {
      return getClass().getResourceAsStream("template.html");
    }
    return new ByteArrayInputStream(Files.readAllBytes(htmlTemplate.toPath()));
  }
  
  private void writeHtmlFile(String outputHtml) throws MojoExecutionException
  {
    getLog().debug("Writing " + outputHtml + " to file " + outputFile);
    try
    {
      new File(outputFile.getParent()).mkdirs();
      FileUtils.writeStringToFile(outputFile, outputHtml, StandardCharsets.UTF_8);
    }
    catch (IOException ex)
    {
      throw new MojoExecutionException("Could not generate file in " + outputFile + " " + ex);
    }
  }

}
