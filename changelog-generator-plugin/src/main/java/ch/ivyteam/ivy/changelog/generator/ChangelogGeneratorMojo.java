package ch.ivyteam.ivy.changelog.generator;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;

import ch.ivyteam.ivy.changelog.generator.jira.JiraResponse.Issue;
import ch.ivyteam.ivy.changelog.generator.jira.JiraService;
import ch.ivyteam.ivy.changelog.generator.util.TemplateExpander;
import ch.ivyteam.ivy.changelog.generator.util.TokenReplacer;

@Mojo(name = "generate-changelog", requiresProject = false)
public class ChangelogGeneratorMojo extends AbstractMojo
{
  /** server id which is configured in settings.xml */
  @Parameter(property = "jiraServerId")
  public String jiraServerId;

  /** jira base url */
  @Parameter(property = "jiraServerUri", defaultValue = "https://jira.axonivy.com/jira")
  public String jiraServerUri;
  
  /*** version to generate the changelog from. for example 7.1 or 7.0.3 */
  @Parameter(property = "fixVersion", required = true)
  public String fixVersion;

  /** comma separated list of jira projects for example: XIVY, IVYPORTAL */
  @Parameter(property = "jiraProjects", defaultValue = "XIVY")
  public String jiraProjects;
  
  /** files which tokens must be replaced */
  @Parameter(property="fileset", required = true)
  public FileSet fileset;
  
  @Parameter(property = "markdownTemplate", defaultValue = "* [${key}](${uri}) ${summary} ${labelsWithHtmlBatches}")
  public String markdownTemplate;
  @Parameter(property = "asciiTemplate", defaultValue = "${kind} ${key}${spacesKey} ${type}${spacesType} ${summary}")
  public String asciiTemplate;
  
  @Parameter(property = "project", required = false, readonly = true)
  MavenProject project;
  @Parameter(property = "session", required = true, readonly = true)
  MavenSession session;
  
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException
  {
    getLog().info("generating changelog for fixVersion " + fixVersion + " and jiraProjects " + jiraProjects);

    List<Issue> issues = loadIssuesFromJira();

    for (File file : getAllFiles())
    {
      TemplateExpander expander = createExpanderForFile(file);
      if (expander != null)
      {
        getLog().info("replace tokens in " + file.getAbsolutePath());
        Map<String, String> tokens = generateTokens(issues, expander);
        new TokenReplacer(file, tokens).replaceTokens();
      }
    }
  }

  private List<Issue> loadIssuesFromJira() throws MojoExecutionException
  {
    try
    {
      Server server = session.getSettings().getServer(jiraServerId);
      return new JiraService(jiraServerUri, server).getIssuesWithFixVersion(fixVersion,jiraProjects);
    }
    catch (RuntimeException ex)
    {
      throw new MojoExecutionException("could not load issues from jira", ex);
    }
  }
  
  private List<File> getAllFiles()
  {
    return Arrays.stream(new FileSetManager().getIncludedFiles(fileset))
            .map(f -> new File(fileset.getDirectory() + File.separatorChar + f))
            .collect(Collectors.toList());
  }
  
  private TemplateExpander createExpanderForFile(File file)
  {
    TemplateExpander expander = null;
    if (file.getName().endsWith(".md"))
    {
      expander = new TemplateExpander(markdownTemplate);
    }
    if (file.getName().endsWith(".txt"))
    {
      expander = new TemplateExpander(asciiTemplate);
    }
    return expander;
  }

  private static Map<String, String> generateTokens(List<Issue> issues, TemplateExpander expander)
  {
    Map<String, String> tokens = new HashMap<>();
    tokens.put("changelog", expander.expand(issues));
    tokens.put("changelog#bugs", expander.expand(onlyBugs(issues)));
    tokens.put("changelog#improvements", expander.expand(onlyImprovements(issues)));
    return tokens;
  }
 
  private static List<Issue> onlyBugs(List<Issue> issues)
  {
    return issues.stream().filter(i -> i.isBug()).collect(Collectors.toList());
  }
  
  private static List<Issue> onlyImprovements(List<Issue> issues)
  {
    return issues.stream().filter(i -> i.isImprovement()).collect(Collectors.toList());
  }
}
