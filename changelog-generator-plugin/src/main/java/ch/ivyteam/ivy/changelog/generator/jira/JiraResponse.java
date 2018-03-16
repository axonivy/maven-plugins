package ch.ivyteam.ivy.changelog.generator.jira;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JiraResponse
{
  public List<Issue> issues = new ArrayList<>();

  public static class Issue
  {
    public String serverUri;
    public String key;
    @JsonProperty("fields")
    private IssueFields fields;
    
    public String getUri()
    {
      return serverUri + "/browse/" + getKey();
    }
    
    public String getKey()
    {
      return key;
    }

    public String getProjectKey()
    {
      return fields.project.key;
    }
    
    public String getSummary()
    {
      return fields.summary;
    }
    
    public String getType()
    {
      return fields.issuetype.name;
    }
    
    public IssueType getIssueType()
    {
      return IssueType.toEnum(getType());
    }
    
    public boolean isSecurityIssue()
    {
      return hasLabel("security");
    }
    
    public boolean isStabilityIssue()
    {
      return hasLabel("stability");
    }
    
    private boolean hasLabel(String label)
    {
      return fields.labels.stream().anyMatch(l -> label.equalsIgnoreCase(l));
    }
  }

  public static class IssueFields
  {
    public String summary;
    public List<String> labels = new ArrayList<>();
    public Type issuetype;
    public Project project;
  }
  
  public static class Type
  {
    public String name;
  }
  
  public enum IssueType
  {
    EPIC, STORY, IMPROVEMENT, BUG;
    
    static IssueType toEnum(String name)
    {
      try
      {
        return valueOf(name.toUpperCase());
      }
      catch (IllegalArgumentException ex)
      {
        return null;
      }
    }
  }
  
  public static class Project
  {
    public String key;
  }
}
