package ch.ivyteam.ivy.changelog.generator.jira;


public class JiraQuery
{
  private final String fixVersion;
  private final String projects;
  private final String issueTypes;
  private final String order;

  public JiraQuery(String fixVersion, String projects, String issueTypes, String order)
  {
    this.fixVersion = removeEndingZero(fixVersion);
    this.projects = projects;
    this.issueTypes = issueTypes;
    this.order = order;
  }

  /**
   * The version string must be converted, because the we tag issues differently
   * if it is part of a minor release or maintenance release.
   * 
   * Example Minor Releases: 7.0, 7.1, 7.2
   * 
   * Example Maintenance Releases: 7.0.1, 7.0.2, 7.1.1
   * 
   * Converts 7.1.0 to 7.1
   * Converts 7.0.0 to 7.0
   * 
   * @param fixVersion
   * @return converted version
   */
  private static String removeEndingZero(String fixVersion)
  {
    if (fixVersion == null)
    {
      return null;
    }
    if (fixVersion.length() > 3 && fixVersion.endsWith(".0"))
    {
      return fixVersion.substring(0, fixVersion.length() - 2);
    }
    return fixVersion;
  }
  
  public String toJql()
  {
    StringBuilder jql = new StringBuilder();
    if (fixVersion != null)
    {
      jql.append("fixVersion = ").append(fixVersion);
    }
    if (issueTypes != null)
    {
      appendParameter(jql);
      jql.append("issuetype in (").append(issueTypes).append(")");
    }
    if (projects != null)
    {
      appendParameter(jql);
      jql.append("project in (").append(projects).append(")");
    }
    if (order != null)
    {
      jql.append(" order by ").append(order);
    }
    return jql.toString();
  }

  private void appendParameter(StringBuilder jql)
  {
    if (jql.length() > 0)
    {
      jql.append(" and ");
    }
  }
}