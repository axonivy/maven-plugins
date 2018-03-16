package ch.ivyteam.ivy.changelog.generator.format;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import ch.ivyteam.ivy.changelog.generator.jira.JiraResponse.Issue;

public class MarkdownFormatter implements Formatter
{
  @Override
  public String generate(List<Issue> issues)
  {
    return issues.stream()
            .map(issue -> new IssueStringBuilder(issue).build())
            .collect(Collectors.joining("\n"));
  }

  @Override
  public boolean isResponsibleFor(File file)
  {
    return file.getName().endsWith(".md");
  }

  private static class IssueStringBuilder
  {
    private final StringBuilder builder;
    private final Issue issue;

    private IssueStringBuilder(Issue issue)
    {
      this.builder = new StringBuilder();
      this.issue = issue;
    }

    private String build()
    {
      appendMarkdownListElement();
      appendSpace();
      appendSummary();
      appendSpace();
      appendKey();
      return builder.toString();
    }

    private void appendMarkdownListElement()
    {
      builder.append("*");
    }

    private void appendSpace()
    {
      builder.append(" ");
    }

    private void appendKey()
    {
      builder.append("[");
      builder.append(issue.key);
      builder.append("](");
      builder.append(issue.getUri());
      builder.append(")");
    }

    private void appendSummary()
    {
      builder.append(issue.getSummary());
    }
  }

}
