package ch.ivyteam.ivy.changelog.generator.jira;

public class JiraQuery {
  private static final String TRAILING_MINOR_VERSION_ZEROS = "(\\d+\\.\\d+)(?:(\\.0+))";
  private final String filterBy;

  public JiraQuery(String filterBy) {
    this.filterBy = filterBy;
  }

  public String toJql() {
    StringBuilder jql = new StringBuilder();
    jql.append(filterBy.replaceAll(TRAILING_MINOR_VERSION_ZEROS, "$1"));
    return jql.toString();
  }
}