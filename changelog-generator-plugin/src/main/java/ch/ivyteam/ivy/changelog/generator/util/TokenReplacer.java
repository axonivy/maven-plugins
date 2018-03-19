package ch.ivyteam.ivy.changelog.generator.util;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.StringUtils;

public class TokenReplacer
{
  private static final String TOKEN_SEPARATOR = "@";
  private static final String encoding = "ISO-8859-1";
  
  private final File file;
  private final Map<String, String> tokens;
  
  public TokenReplacer(File file, Map<String, String> tokens)
  {
    this.file = file;
    this.tokens = tokens;
  }
  
  public void replaceTokens() throws MojoExecutionException
  {
    String template = getTemplateContent();
    String outputContent = fillTokens(template);
    writeResult(outputContent);
  }
  
  private String getTemplateContent() throws MojoExecutionException
  {
    try
    {
      return FileUtils.readFileToString(file, encoding);
    }
    catch (IOException ex)
    {
      throw new MojoExecutionException("Failed to read template file "+ file, ex);
    }
  }

  private String fillTokens(String fileContent)
  {
    for (Entry<String, String> entry : tokens.entrySet())
    {
      String token = TOKEN_SEPARATOR + entry.getKey() + TOKEN_SEPARATOR;
      if (fileContent.contains(token))
      {
        fileContent = StringUtils.replace(fileContent, token, entry.getValue());
      }
    }
    return fileContent;
  }

  private void writeResult(String outputContent) throws MojoExecutionException
  {
    try
    {
      FileUtils.write(file, outputContent, encoding);
    }
    catch (IOException ex)
    {
      throw new MojoExecutionException("Failed to write generated text to " + file, ex);
    }
  }
}