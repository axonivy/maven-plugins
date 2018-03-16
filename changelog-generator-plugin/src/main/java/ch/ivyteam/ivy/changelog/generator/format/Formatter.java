package ch.ivyteam.ivy.changelog.generator.format;

import java.io.File;
import java.util.List;

import ch.ivyteam.ivy.changelog.generator.jira.JiraResponse.Issue;

public interface Formatter
{
  String generate(List<Issue> issues);
  
  boolean isResponsibleFor(File file);
}
