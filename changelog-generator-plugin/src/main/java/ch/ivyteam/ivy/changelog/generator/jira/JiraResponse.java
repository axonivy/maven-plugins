package ch.ivyteam.ivy.changelog.generator.jira;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JiraResponse
{
  public static final String LABEL_IMPROVEMENT = "improvement"; 
  
  public List<Issue> issues = new ArrayList<>();

  public static class Issue
  {
    public String key;
    @JsonProperty("fields")
    public IssueFields fields;
    public String serverUri;
    
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
    
    public boolean isBug()
    {
      return getType().equalsIgnoreCase("bug");
    }
    
    public boolean isImprovement()
    {
      return hasLabel(LABEL_IMPROVEMENT);
    }
    
    public boolean isUpgradeCritical()
    {
      return hasLabel("UpgradeCritial");
    }
    
    public boolean isUpgradeRecommended()
    {
      return hasLabel("UpgradeRecommended");
    }
    
    public List<String> getLabels()
    {
      return fields.labels;
    }
    
    private boolean hasLabel(String label)
    {
      return fields.labels.stream().anyMatch(l -> label.equalsIgnoreCase(l));
    }

    @Override
    public String toString()
    {
      return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
    }
    
  }

  public static class IssueFields
  {
    public String summary;
    public List<String> labels = new ArrayList<>();
    public Type issuetype;
    public Project project;
    
    @Override
    public String toString()
    {
      return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
    }
  }
  
  public static class Type
  {
    public String name;
    
    @Override
    public String toString()
    {
      return name;
    }
  }
  
  public enum IssueType
  {
    EPIC, STORY, IMPROVEMENT, BUG, UNKNOWN;
    
    static IssueType toEnum(String name)
    {
      try
      {
        return valueOf(name.toUpperCase());
      }
      catch (IllegalArgumentException ex)
      {
        return UNKNOWN;
      }
    }
  }
  
  public static class Project
  {
    public String key;
    
    @Override
    public String toString()
    {
      return key;
    }
  }
  
  public static class Filter
  {
    public static List<Issue> bugs(List<Issue> issues)
    {
      return issues.stream().filter(i -> i.isBug()).collect(Collectors.toList());
    }

    public static List<Issue> improvements(List<Issue> issues)
    {
      return issues.stream().filter(i -> i.isImprovement()).collect(Collectors.toList());
    }
  }
}
