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
  private static final Comparator<Issue> BY_PRODUCT_THEN_BY_KEY = Comparator.comparing(Issue::getProjectKey).reversed()
                  .thenComparing(Comparator.comparingInt(Issue::getIssueNumber));

  /** server id which is configured in settings.xml */
  @Parameter(property = "jiraServerId")
  public String jiraServerId;

  /** jira base url */
  @Parameter(property = "jiraServerUri", defaultValue = "https://axonivy.atlassian.net")
  public String jiraServerUri;

  /*** filter query to run against Jira */
  @Parameter(property = "filterBy", required = true)
  public String filterBy;

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
  @Parameter(property = "fileset")
  public FileSet fileset;

  /** Prints the changelog to the console */
  @Parameter(property = "console")
  public boolean console = false;

  @Parameter(property = "markdownTemplate", defaultValue = "* [${key}](${uri}) ${summary} ${labelsWithHtmlBatches}")
  public String markdownTemplate;

  @Parameter(property = "asciiTemplate", defaultValue = "${kind} ${key}${spacesKey} ${type}${spacesType} ${summary}")
  public String asciiTemplate;

  /**
   * Prints a markdown header title if set.
   * E.g if you set it to "###", the changelog#improvements tag will also get a "### Improvements" header.
   *
   */
  @Parameter(property = "markdownHeaderIndent", defaultValue = "")
  public String markdownHeaderIndent;

  @Parameter(property = "project", required = false, readonly = true)
  MavenProject project;
  @Parameter(property = "session", required = true, readonly = true)
  MavenSession session;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    getLog().info("generating changelog for filter query '" + filterBy + "'");

    Server server = session.getSettings().getServer(jiraServerId);
    if (server == null) {
      getLog().warn("can not generate changelog because server '" + jiraServerId
              + "' is not definied in setting.xml");
      return;
    }
    exec(server);
  }

  void exec(Server server) throws MojoExecutionException {
    List<Issue> issues = loadIssuesFromJira(server);
    if (console) {
      printToConsole(issues);
    } else {
      replaceInTemplateFiles(issues);
    }
  }

  private void printToConsole(List<Issue> issues) {
    TemplateExpander expander = createMarkdownTemplateExpander();
    expander.setWordWrap(wordWrap);
    Map<String, String> tokens = generateTokens(issues, expander);

    for (Map.Entry<String, String> entry : tokens.entrySet()) {
      System.out.println();
      System.out.println(entry.getKey());
      System.out.println(entry.getValue());
    }
  }

  private void replaceInTemplateFiles(List<Issue> issues) throws MojoExecutionException {
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
      JiraQuery query = new JiraQuery(filterBy);
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
      return createMarkdownTemplateExpander();
    }
    return new TemplateExpander(asciiTemplate, whitelistJiraLabels, "");
  }

  private TemplateExpander createMarkdownTemplateExpander() {
    return new TemplateExpander(markdownTemplate, whitelistJiraLabels, markdownHeaderIndent);
  }

  private Map<String, String> generateTokens(List<Issue> issues, TemplateExpander expander) {
    Map<String, String> tokens = new HashMap<>();
    tokens.put("changelog#improvements", expander.expand(Filter.improvements(issues), "Improvements"));

    List<Issue> sortIssues = sortIssues(issues);
    tokens.put("changelog", expander.expand(sortIssues, "All Changes"));
    tokens.put("changelog#features", expander.expand(Filter.others(sortIssues), "New Features"));
    tokens.put("changelog#bugs", expander.expand(Filter.bugs(sortIssues), "Bugs"));
    tokens.put("upgradeRecommendation", generateUpgradeRecommendation(sortIssues));
    return tokens;
  }

  private List<Issue> sortIssues(List<Issue> issues) {
    List<Issue> sortedIssues = new ArrayList<>(issues);
    sortedIssues.sort(BY_PRODUCT_THEN_BY_KEY);
    return sortedIssues;
  }

  private String generateUpgradeRecommendation(List<Issue> sortIssues)
  {
    if (sortIssues.stream().anyMatch(Issue::isUpgradeCritical))
    {
      return "We strongly recommend to install this update release because it fixes security issues!";
    }
    if (sortIssues.stream().anyMatch(Issue::isUpgradeRecommended))
    {
      return "We recommend to install this update release because it fixes stability issues!";
    }
    return "We suggest to install this update release if you are suffering from any of these issues.";
  }
}
