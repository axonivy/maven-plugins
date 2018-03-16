package ch.ivyteam.ivy.changelog.generator;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Server;

import ch.ivyteam.ivy.changelog.generator.format.AsciiFormatter;
import ch.ivyteam.ivy.changelog.generator.format.Formatter;
import ch.ivyteam.ivy.changelog.generator.format.MarkdownFormatter;
import ch.ivyteam.ivy.changelog.generator.jira.JiraResponse.Issue;
import ch.ivyteam.ivy.changelog.generator.jira.JiraService;
import ch.ivyteam.ivy.changelog.generator.util.FileUtil;
import ch.ivyteam.ivy.changelog.generator.util.TokenReplacer;

@Mojo(name = "generate-changelog", requiresProject = false)
public class ChangelogGeneratorMojo extends AbstractMojo
{
  private final static Set<Formatter> formatters = new HashSet<>();
  static
  {
    formatters.add(new AsciiFormatter());
    formatters.add(new MarkdownFormatter());
  }

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

  
  @Parameter(property = "dir", required = true)
  /** lesen schreiben todo */
  public String dir; // todo fileset FileSet / in/out
  
  
  
  @Parameter(property = "session", required = true, readonly = true)
  MavenSession session;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException
  {
    getLog().info("generating changelog for fixVersion " + fixVersion + " and jiraProjects " + jiraProjects);

    List<Issue> issues = loadIssuesFromJira();

    for (Path file : FileUtil.getAllFiles(dir))
    {
      File f = file.toFile();
      Formatter formatter = findFormatter(f);
      if (formatter != null)
      {
        getLog().info("replace tokens in " + f.getAbsolutePath());
        Map<String, String> tokens = generateTokens(issues, formatter);
        new TokenReplacer(f, tokens).replaceTokens();
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

  private static Formatter findFormatter(File file)
  {
    return formatters.stream()
            .filter(formatter -> formatter.isResponsibleFor(file))
            .findFirst()
            .orElse(null);
  }
  
  private static Map<String, String> generateTokens(List<Issue> issues, Formatter formatter)
  {
    Map<String, String> tokens = new HashMap<>();
    tokens.put("changelog", formatter.generate(issues));
    tokens.put("changelog#bugs", formatter.generate(issues.stream().filter(i -> "bug".equalsIgnoreCase(i.getType())).collect(Collectors.toList())));
    tokens.put("changelog#improvements", formatter.generate(issues.stream().filter(i -> "improvement".equalsIgnoreCase(i.getType())).collect(Collectors.toList())));
    return tokens;
  }
  
}
