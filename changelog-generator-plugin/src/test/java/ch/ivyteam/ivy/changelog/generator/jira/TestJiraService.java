package ch.ivyteam.ivy.changelog.generator.jira;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.settings.Server;
import org.junit.Before;
import org.junit.Test;

import ch.ivyteam.ivy.changelog.generator.jira.JiraResponse.Filter;
import ch.ivyteam.ivy.changelog.generator.jira.JiraResponse.Issue;

/**
 * jira.username and jira.password must be passed as system property to this
 * test.
 */
public class TestJiraService {
  private JiraService testee;

  @Before
  public void setUp() {
    Server server = new Server();
    server.setUsername(System.getProperty("jira.username"));
    server.setPassword(System.getProperty("jira.password"));
    testee = new JiraService("https://1ivy.atlassian.net", server, new SystemStreamLog());
  }

  @Test
  public void getIssuesWithFixVersion_71() {
    List<Issue> issues = testee.queryIssues(query("7.1", "key"));
    assertThatXIVY2266isContained(issues);
  }

  @Test
  public void getIssuesWithFixVersion_710() {
    List<Issue> issues = testee.queryIssues(query("7.1.0", "key"));
    assertThatXIVY2266isContained(issues);
  }

  private void assertThatXIVY2266isContained(List<Issue> issues) {
    Issue issue = issues.stream().filter(i -> i.key.equals("XIVY-2266")).findFirst().orElse(null);
    assertThat(issue.getSummary()).isEqualTo("Remove AspectJ");
    assertThat(issue.isUpgradeCritical()).isFalse();
    assertThat(issue.isUpgradeRecommended()).isFalse();
    assertThat(issue.getType()).isEqualTo("Story");
  }

  @Test
  public void getIssuesWithFixVersion_703() {
    List<Issue> issues = testee.queryIssues(query("7.0.3", "key"));
    assertThat(issues).hasSize(6);
  }

  @Test
  public void getIssuesWithFixVersion_notValidVersion() {
    assertThatThrownBy(() -> testee.queryIssues(query("nonExistingVersion", "key")))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("400 Bad Request");
  }

  @Test
  public void issuesInOrder() {
    List<Issue> issues = testee.queryIssues(query("7.4", "\"Epic Link\", key"));
    issues = Filter.improvements(issues);
    issues.stream().forEach(System.out::println);
  }

  private static JiraQuery query(String version, String orderBy) {
    StringBuilder builder = new StringBuilder("project = XIVY AND issuetype IN (Story, Improvement, Bug)");
    builder.append(" AND fixVersion = ").append(version);
    return new JiraQuery(builder.toString(), orderBy);
  }

}
