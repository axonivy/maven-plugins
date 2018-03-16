package ch.ivyteam.ivy.changelog.generator.format;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import ch.ivyteam.ivy.changelog.generator.jira.JiraResponse.Issue;

public class AsciiFormatter implements Formatter
{
  @Override
  public String generate(List<Issue> issues)
  {
    Integer maxKeyLength = issues.stream().map(i -> i.key.length()).reduce(Integer::max).orElse(0);
    Integer maxTypeLength = issues.stream().map(i -> i.getType().length()).reduce(Integer::max).orElse(0);

    return issues.stream()
            .map(issue -> new IssueStringBuilder(issue, maxKeyLength, maxTypeLength).build())
            .collect(Collectors.joining("\n"));
  }
  
  @Override
  public boolean isResponsibleFor(File file)
  {
    return file.getName().endsWith(".txt");
  }
  
  private static class IssueStringBuilder
  {
    private final int maxKeyLength;
    private final int maxTypeLength;

    private final StringBuilder builder;
    private final Issue issue;

    private IssueStringBuilder(Issue issue, int maxKeyLength, int maxTypeLength)
    {
      this.builder = new StringBuilder();
      this.issue = issue;
      this.maxKeyLength = maxKeyLength;
      this.maxTypeLength = maxTypeLength;
    }

    private String build()
    {
      appendFirstSign();
      appendSpace();
      appendKey();
      appendSpace();
      appendType();
      appendSpace();
      appendSummary();
      return builder.toString();
    }

    private void appendFirstSign()
    {
      if (issue.isSecurityIssue())
      {
        builder.append("!");
      }
      else if (issue.isStabilityIssue())
      {
        builder.append("*");
      }
      else
      {
        builder.append("+");
      }
    }

    private void appendSpace()
    {
      builder.append(" ");
    }

    private void appendKey()
    {
      String key = issue.key;
      builder.append(key);
      fillUpWithSpaces(key.length(), maxKeyLength);
    }

    private void appendType()
    {
      String type = issue.getType();
      builder.append(type);
      fillUpWithSpaces(type.length(), maxTypeLength);
    }

    private void appendSummary()
    {
      builder.append(issue.getSummary());
    }

    private void fillUpWithSpaces(int used, int fillUpTo)
    {
      for (int i = used; i < fillUpTo; i++)
      {
        appendSpace();
      }
    }
  }
}
