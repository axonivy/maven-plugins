package ch.ivyteam.ivy.changelog.generator.jira;

import org.apache.commons.lang3.StringUtils;

public class JiraQuery {
  private static final String TRAILING_MINOR_VERSION_ZEROS = "(\\d+\\.\\d+)(?:(\\.0+))";
  private final String filterBy;
  private final String orderBy;

  public JiraQuery(String filterBy, String orderBy) {
    this.filterBy = filterBy;
    this.orderBy = orderBy;
  }

  public String toJql() {
    StringBuilder jql = new StringBuilder();
    jql.append(filterBy.replaceAll(TRAILING_MINOR_VERSION_ZEROS, "$1"));
    if (StringUtils.isNotBlank(orderBy)) {
      jql.append(" order by ").append(orderBy);
    }
    return jql.toString();
  }
}