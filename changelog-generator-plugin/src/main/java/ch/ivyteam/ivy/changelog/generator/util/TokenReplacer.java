package ch.ivyteam.ivy.changelog.generator.util;

import java.util.Map;
import java.util.Map.Entry;

import org.codehaus.plexus.util.StringUtils;

public class TokenReplacer
{
  private static final String TOKEN_SEPARATOR = "@";

  private final Map<String, String> tokens;
  
  public TokenReplacer(Map<String, String> tokens)
  {
    this.tokens = tokens;
  }
  
  public String replaceTokens(String templateContent)
  {
    for (Entry<String, String> entry : tokens.entrySet())
    {
      String token = TOKEN_SEPARATOR + entry.getKey() + TOKEN_SEPARATOR;
      if (templateContent.contains(token))
      {
        templateContent = StringUtils.replace(templateContent, token, entry.getValue());
      }
    }
    return templateContent;
  }
}