package ch.ivyteam.ivy.jira.release;

import java.net.URI;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * https://docs.atlassian.com/software/jira/docs/api/REST/1000.824.0/#api/2/version-getVersion
 */
public class JiraVersion {

  public URI self;
  public Integer id;
  public String name;
  public String description;
  public boolean archived;
  public boolean released;
  public int projectId;
  public String project;

  public static JiraVersion newXivy(String version) {
    JiraVersion v = new JiraVersion();
    v.project = "XIVY";
    v.projectId = 10048;
    v.name = version;
    return v;
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this, ToStringStyle.JSON_STYLE);
  }

}
