package ch.ivyteam.ivy.maven;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;

public abstract class AbstractReadmeGeneratorMojo extends AbstractMojo
{
  static final String TOKEN_SEPARATOR = "@";
  
  /** the encoding used to read the {@link #templateFile} and write the {@link #outputFile} */
  @Parameter(defaultValue="UTF-8", property="readme.file.encoding")
  String encoding;
  
  /** the readme.html template which contains tokens to be replaced by with real generated content.*/
  @Parameter(defaultValue = "${basedir}/ReadMe.html", property="readme.template.file")
  protected File templateFile;
  
  @Parameter(defaultValue = "${basedir}/ReadMe.html", property="readme.output.file")
  protected File outputFile;
  
  /**
   * @param htmlTokens key=token, value=html table
   * @throws MojoExecutionException
   */
  protected final void writeReadme(Map<String, String> htmlTokens) throws MojoExecutionException
  {
    String templateHtml = getTemplateContent();
    String outputHtml = fillTokens(templateHtml, htmlTokens);
    writeResultHtml(outputHtml);
  }

  private String getTemplateContent() throws MojoExecutionException
  {
    try
    {
      return FileUtils.readFileToString(templateFile, encoding);
    }
    catch (IOException ex)
    {
      throw new MojoExecutionException("Failed to read template file "+templateFile, ex);
    }
  }

  private String fillTokens(String html, Map<String, String> htmlTokens)
  {
    for(Entry<String, String> entry : htmlTokens.entrySet())
    {
      String token = TOKEN_SEPARATOR+entry.getKey()+TOKEN_SEPARATOR;
      if (!html.contains(token))
      {
        getLog().warn("Replaceable token '"+token+"' does not exist in '"+templateFile+"'");
        continue;
      }
      
      html = StringUtils.replace(html, token, entry.getValue());
    }
    return html;
  }

  private void writeResultHtml(String outputHtml) throws MojoExecutionException
  {
    try
    {
      FileUtils.write(outputFile, outputHtml, encoding);
    }
    catch (IOException ex)
    {
      throw new MojoExecutionException("Failed to write generated html to "+outputHtml);
    }
  }

}