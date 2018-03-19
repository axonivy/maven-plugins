package ch.ivyteam.ivy.changelog.generator.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.text.StrSubstitutor;

import ch.ivyteam.ivy.changelog.generator.jira.JiraResponse;
import ch.ivyteam.ivy.changelog.generator.jira.JiraResponse.Issue;

public class TemplateExpander
{
  private final String template;
  private final String templateImprovements;
  
  public TemplateExpander(String template, String templateImprovements)
  {
    this.template = template;
    this.templateImprovements = templateImprovements;
  }
  
  public String expand(List<Issue> issues)
  {
    return expand(issues, template);
  }
  
  public String expandImprovements(List<Issue> issues)
  {
    return expand(issues, templateImprovements);
  }
  
  private static String expand(List<Issue> issues, String template)
  {
    Integer maxKeyLength = issues.stream().map(i -> i.getKey().length()).reduce(Integer::max).orElse(0);
    Integer maxTypeLength = issues.stream().map(i -> i.getType().length()).reduce(Integer::max).orElse(0);
    
    return issues.stream()
            .map(issue -> createValues(issue, maxKeyLength, maxTypeLength))
            .map(values -> new StrSubstitutor(values).replace(template))
            .collect(Collectors.joining("\n"));
  }
  
  private static Map<String, String> createValues(Issue issue, int maxKeyLength, int maxTypeLength)
  {
    Map<String, String> values = new HashMap<>();
    values.put("kind", createFirstSign(issue));
    values.put("summary", issue.getSummary());
    values.put("key", issue.getKey());
    values.put("spacesKey", generateSpaces(maxKeyLength - issue.getKey().length()));
    values.put("type", issue.getType());
    values.put("spacesType", generateSpaces(maxTypeLength - issue.getType().length()));
    values.put("uri", issue.getUri());
    values.put("labelsWithHtmlBatches", generateLabels(issue));
    values.put("htmlLinkIcon", "<a href=\"" + issue.getUri() + "\"><span class=\"glyphicon glyphicon-new-window\"></span></a>");
    return values;
  }

  private static String generateLabels(Issue issue)
  {
    return issue.getLabels().stream()
            .filter(i -> !i.equalsIgnoreCase(JiraResponse.LABEL_IMPROVEMENT))
            .map(String::toLowerCase)
            .map(l -> "<span class=\"feature-badge\">" + l + "</span>")
            .collect(Collectors.joining(" "));
  }
  
  private static String generateSpaces(int length)
  {
    String spaces = "";
    for (int i = 0; i < length; i++)
    {
      spaces += " ";
    }
    return spaces;
  }
  
  private static String createFirstSign(Issue issue)
  {
    if (issue.isSecurityIssue())
    {
      return "!";
    }
    else if (issue.isStabilityIssue())
    {
      return "*";
    }
    else
    {
      return "+";
    }
  }
}