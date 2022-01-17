package ch.ivyteam.ivy.changelog.generator;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
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

import ch.ivyteam.ivy.changelog.generator.jira.JiraQuery;
import ch.ivyteam.ivy.changelog.generator.jira.JiraResponse.Filter;
import ch.ivyteam.ivy.changelog.generator.jira.JiraResponse.Issue;
import ch.ivyteam.ivy.changelog.generator.jira.JiraService;
import ch.ivyteam.ivy.changelog.generator.util.ChangelogIO;
import ch.ivyteam.ivy.changelog.generator.util.TemplateExpander;
import ch.ivyteam.ivy.changelog.generator.util.TokenReplacer;

@Mojo(name = "generate-changelog", requiresProject = false)
public class ChangelogGeneratorMojo extends AbstractMojo {
  /** server id which is configured in settings.xml */
  @Parameter(property = "jiraServerId")
  public String jiraServerId;

  /** jira base url */
  @Parameter(property = "jiraServerUri", defaultValue = "https://axonivy.atlassian.net")
  public String jiraServerUri;

  /*** filter query to run against Jira */
  @Parameter(property = "filterBy", required = true)
  public String filterBy;

  /** comma separated list of issue fields to define ordering */
  @Parameter(property = "jira.issue.order", defaultValue = "project,key")
  public String orderBy;

  /**
   * comma separated list of labels which will be parsed as batches for example:
   * security, performance, usability
   */
  @Parameter(property = "whitelistJiraLabels", defaultValue = "")
  public String whitelistJiraLabels;

  /** compression (supports gz) */
  @Parameter(property = "compression", defaultValue = "")
  public String compression;

  /** word wrap in changelog */
  @Parameter(property = "wordWrap", defaultValue = "-1")
  public int wordWrap;

  /** files which tokens must be replaced */
  @Parameter(property = "fileset", required = true)
  public FileSet fileset;

  @Parameter(property = "markdownTemplate", defaultValue = "* [${key}](${uri}) ${summary} ${labelsWithHtmlBatches}")
  public String markdownTemplate;
  @Parameter(property = "markdownTemplateImprovements", defaultValue = "* ${summary} ${htmlLinkIcon} ${labelsWithHtmlBatches}")
  public String markdownTemplateImprovements;

  @Parameter(property = "asciiTemplate", defaultValue = "${kind} ${key}${spacesKey} ${type}${spacesType} ${summary}")
  public String asciiTemplate;

  @Parameter(property = "project", required = false, readonly = true)
  MavenProject project;
  @Parameter(property = "session", required = true, readonly = true)
  MavenSession session;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    getLog().info("generating changelog for filter query '" + filterBy + "', order by " + orderBy);

    Server server = session.getSettings().getServer(jiraServerId);
    if (server == null) {
      getLog().warn("can not generate changelog because server '" + jiraServerId
              + "' is not definied in setting.xml");
      return;
    }

    List<Issue> issues = loadIssuesFromJira(server);
    for (File sourceFile : getAllFiles()) {
      getLog().info("replace tokens in " + sourceFile.getAbsolutePath());

      ChangelogIO changelogHandler = new ChangelogIO(sourceFile, getOutputFile(sourceFile));
      String changelog = changelogHandler.getTemplateContent();

      TemplateExpander expander = createExpanderForFile(sourceFile);
      expander.setWordWrap(wordWrap);

      Map<String, String> tokens = generateTokens(issues, expander);
      changelog = new TokenReplacer(tokens).replaceTokens(
              changelogHandler.getTemplateContent());

      if (StringUtils.equals(compression, "gz")) {
        changelogHandler.compressMaxGzipFile(changelog);
      } else {
        changelogHandler.writeResult(changelog);
      }
    }
  }

  private List<Issue> loadIssuesFromJira(Server server) throws MojoExecutionException {
    try {
      JiraService jiraService = new JiraService(jiraServerUri, server, getLog());
      JiraQuery query = new JiraQuery(filterBy, orderBy);
      return jiraService.queryIssues(query);
    } catch (RuntimeException ex) {
      throw new MojoExecutionException("could not load issues from jira", ex);
    }
  }

  private List<File> getAllFiles() {
    return Arrays.stream(new FileSetManager().getIncludedFiles(fileset))
            .map(f -> new File(fileset.getDirectory() + File.separatorChar + f))
            .collect(Collectors.toList());
  }

  private File getOutputFile(File sourceFile) {
    String outputDir = fileset.getOutputDirectory();
    if (StringUtils.isNotEmpty(outputDir)) {
      return new File(outputDir + File.separatorChar + sourceFile.getName());
    }
    return sourceFile;
  }

  private TemplateExpander createExpanderForFile(File file) {
    if (file.getName().endsWith(".md")) {
      return new TemplateExpander(markdownTemplate, markdownTemplateImprovements, whitelistJiraLabels);
    }
    return new TemplateExpander(asciiTemplate, asciiTemplate, whitelistJiraLabels);
  }

  private Map<String, String> generateTokens(List<Issue> issues, TemplateExpander expander) {
    Map<String, String> tokens = new HashMap<>();
    tokens.put("changelog#improvements", expander.expandImprovements(Filter.improvements(issues)));

    List<Issue> sortIssues = sortIssues(issues);
    tokens.put("changelog", expander.expand(sortIssues));
    tokens.put("changelog#bugs", expander.expand(Filter.bugs(sortIssues)));
    tokens.put("upgradeRecommendation", generateUpgradeRecommendation(sortIssues));
    return tokens;
  }

  private List<Issue> sortIssues(List<Issue> issues) {
    List<Issue> sortedIssues = new ArrayList<>(issues);
    sortedIssues.sort(Comparator.comparing(Issue::getProjectKey).reversed()
            .thenComparing(Comparator.comparing(Issue::getType).reversed()
                    .thenComparing(Comparator.comparingInt(this::getIssueNumber).reversed())));
    return sortedIssues;
  }

  private int getIssueNumber(Issue issue) {
    String issueNumber = StringUtils.substringAfter(issue.getKey(), issue.getProjectKey());
    return Integer.valueOf(issueNumber);
  }

  private String generateUpgradeRecommendation(List<Issue> sortIssues) {
    if (sortIssues.stream().anyMatch(Issue::isUpgradeCritical)) {
      return "We strongly recommend to install this update release because it fixes security issues!";
    }
    if (sortIssues.stream().anyMatch(Issue::isUpgradeRecommended)) {
      return "We recommend to install this update release because it fixes stability issues!";
    }
    return "We suggest to install this update release if you are suffering from any of these issues.";
  }
}
