package ch.ivyteam.ivy.changelog.generator.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import ch.ivyteam.ivy.changelog.generator.jira.JiraResponse.Issue;
import ch.ivyteam.ivy.changelog.generator.jira.JiraResponse.IssueFields;
import ch.ivyteam.ivy.changelog.generator.jira.JiraResponse.Type;

public class TestTemplateExpander
{

  @Test
  public void expand_key()
  {
    TemplateExpander testee = new TemplateExpander("[${key}] - ${summary}", "", "");
    String expand = testee.expand(createOneIssue());
    assertThat(expand).isEqualTo("[XIVY-500] - JSF Bug");
  }

  @Test
  public void expand_labelsWithHtmlBatches()
  {
    TemplateExpander testee = new TemplateExpander("${labelsWithHtmlBatches}", "", "seCurity , performance");
    String expand = testee.expand(createOneIssue());
    assertThat(expand).isEqualTo("<span class=\"feature-badge\">security</span>");
  }

  @Test
  public void expand_ascii_keepLeadingWhitespace()
  {
    TemplateExpander testee = new TemplateExpander("   [${key}] - ${summary}", "", "");
    String expand = testee.expand(createOneIssue());
    assertThat(expand).isEqualTo("   [XIVY-500] - JSF Bug");
  }

  private static List<Issue> createOneIssue() {
    List<Issue> issues = new ArrayList<>();
    Issue i = new Issue();

    i.key = "XIVY-500";
    i.fields = new IssueFields();

    i.fields.summary = "JSF Bug";

    i.fields.issuetype = new Type();
    i.fields.issuetype.name = "Bug";

    i.fields.labels = new ArrayList<>();
    i.fields.labels.add("Security");
    i.fields.labels.add("jira_escalated");

    issues.add(i);
    return issues;
  }

}
