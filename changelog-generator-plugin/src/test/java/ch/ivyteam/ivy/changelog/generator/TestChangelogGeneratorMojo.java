package ch.ivyteam.ivy.changelog.generator;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.shared.model.fileset.FileSet;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TestChangelogGeneratorMojo {
  @Rule
  public MojoRule rule = new MojoRule();

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  private String outputPath;

  private ChangelogGeneratorMojo mojo;

  private File basedir;

  @Before
  public void setup() throws Exception {
    tempFolder.create();

    basedir = new File("src/test/resources");
    outputPath = tempFolder.getRoot().getAbsolutePath() + "/target";
    MavenProject project = rule.readMavenProject(basedir);
    MavenSession session = rule.newMavenSession(project);
    MojoExecution execution = rule.newMojoExecution("generate-changelog");
    Server server = new Server();
    server.setUsername(System.getProperty("jira.username"));
    server.setPassword(System.getProperty("jira.password"));
    server.setId("axonivy.jira");
    session.getSettings().addServer(server);
    mojo = (ChangelogGeneratorMojo) rule.lookupConfiguredMojo(session, execution);

    mojo.jiraServerId = "axonivy.jira";
    mojo.whitelistJiraLabels = "security,performance";
    mojo.fileset = new FileSet();
    mojo.fileset.setDirectory(basedir.getAbsolutePath());
    mojo.fileset.setOutputDirectory(outputPath);
    mojo.wordWrap = 80;
  }

  @Test
  public void createChangelog() throws Exception {
    mojo.filterBy = "project IN (XIVY,IVYPORTAL) AND fixVersion = 7.2.0";
    mojo.asciiTemplate = "  * ${key} ${summary}";
    mojo.fileset.addInclude("changelog");
    mojo.compression = "gz";
    mojo.execute();

    assertThat(new File(outputPath + "/changelog.gz")).exists();
  }

  @Test
  public void createProjectSortedReleaseNotes() throws Exception {
    mojo.filterBy = "project IN (XIVY,IVYPORTAL) AND fixVersion = 7.4.0";
    mojo.asciiTemplate = "${key} ${type}";
    mojo.fileset.addInclude("ReleaseNotes.txt");
    mojo.execute();

    File releaseNotes = new File(outputPath + "/ReleaseNotes.txt");
    assertThat(releaseNotes).exists();

    String issues = StringUtils.substringAfter(readFileContent(releaseNotes),
            "This is a Leading Edge version.");
    String portalIssues = StringUtils.substringBeforeLast(issues, "IVYPORTAL");
    portalIssues = StringUtils.substringAfter(issues, "IVYPORTAL");
    assertThat(portalIssues)
            .as("No XIVY and IVYPORTAL issues mixed")
            .doesNotContain("XIVY-");
  }

  @Test
  public void createPortalReleaseNotes() throws Exception {
    mojo.filterBy = "project = IVYPORTAL AND fixVersion = 7.4.0 AND issuetype in (Story, Improvement, Bug)";
    mojo.asciiTemplate = "${key} ${type}";
    mojo.fileset.addInclude("ReleaseNotes.txt");
    mojo.execute();

    File releaseNotes = new File(outputPath + "/ReleaseNotes.txt");
    assertThat(releaseNotes).exists();

    assertThat(readFileContent(releaseNotes))
            .as("No Technical tasks displayed")
            .doesNotContain("Technical task");
  }

  @Test
  public void createOnlyBugReleaseNotes() throws Exception {
    mojo.filterBy = "project = XIVY AND fixVersion = 7.4.0 AND issuetype = Bug";
    mojo.asciiTemplate = "${key} ${type}";
    mojo.fileset.addInclude("ReleaseNotes.txt");
    mojo.execute();

    File releaseNotes = new File(outputPath + "/ReleaseNotes.txt");
    assertThat(releaseNotes).exists();

    assertThat(readFileContent(releaseNotes))
            .as("Only bugs contained")
            .doesNotContain("Technical task")
            .doesNotContain("Story")
            .doesNotContain("Improvement")
            .contains("Bug");
  }

  @Test
  public void createSortedReleaseNotes() throws Exception {
    mojo.filterBy = "project = XIVY AND fixVersion = 7.4.0";
    mojo.asciiTemplate = "${key}:${type};";
    mojo.fileset.addInclude("ReleaseNotes.txt");
    mojo.execute();

    File releaseNotes = new File(outputPath + "/ReleaseNotes.txt");
    assertThat(releaseNotes).exists();

    String issues = StringUtils.substringAfter(readFileContent(releaseNotes),
            "This is a Leading Edge version.");
    String[] splitIssues = StringUtils.split(issues, ";");
    assertCorrectlyOrdered(filterIssuesForType(splitIssues, "Story"));
    assertCorrectlyOrdered(filterIssuesForType(splitIssues, "Improvement"));
    assertCorrectlyOrdered(filterIssuesForType(splitIssues, "Bug"));

    assertThat(StringUtils.substringBeforeLast(issues, "Story")).doesNotContain("Bug", "Improvement");
    assertThat(StringUtils.substringBeforeLast(issues, "Improvement")).doesNotContain("Bug");
  }

  @Test
  public void createReleaseNotesWithUpgradRecommended() throws Exception {
    String recommendation = createReleaseNotesAndReadRecommendation("7.4.0");
    assertThat(recommendation)
            .contains("We recommend to install this update release because it fixes stability issues!");
  }

  @Test
  public void createReleaseNotesWithUpgradeCritical() throws Exception {
    String recommendation = createReleaseNotesAndReadRecommendation("8.0.4");
    assertThat(recommendation).contains(
            "We strongly recommend to install this update release because it fixes security issues!");
  }

  @Test
  public void createReleaseNotesWithUpgradeSuggested() throws Exception {
    String recommendation = createReleaseNotesAndReadRecommendation("8.0.3");
    assertThat(recommendation).contains(
            "We suggest to install this update release if you are suffering from any of these issues.");
  }

  private String createReleaseNotesAndReadRecommendation(String version)
          throws MojoExecutionException, MojoFailureException, FileNotFoundException, IOException {
    mojo.filterBy = "project = XIVY AND fixVersion = " + version;
    mojo.asciiTemplate = "${key}:${type};";
    mojo.fileset.addInclude("ReleaseNotes.txt");
    mojo.execute();

    File releaseNotes = new File(outputPath + "/ReleaseNotes.txt");
    assertThat(releaseNotes).exists();

    String recommendation = StringUtils.substringAfter(readFileContent(releaseNotes),
            "This is a Leading Edge version.");
    return recommendation;
  }

  private String[] filterIssuesForType(String[] splitIssues, String type) {
    return Arrays.stream(splitIssues)
            .filter(issue -> StringUtils.substringAfter(issue, ":").equals(type))
            .toArray(String[]::new);
  }

  private void assertCorrectlyOrdered(String[] splitIssues) {
    for (int i = 0; i < splitIssues.length - 1; i++) {
      Integer issueNumber = getIssueNumber(splitIssues[i]);
      Integer nextIssueNumber = getIssueNumber(splitIssues[i + 1]);
      assertThat(issueNumber)
              .as("Next issue number must be higher")
              .isLessThan(nextIssueNumber);
    }
  }

  private Integer getIssueNumber(String issue) {
    return Integer.valueOf(StringUtils.substringBetween(issue, "XIVY-", ":"));
  }

  private String readFileContent(File releaseNotes) throws FileNotFoundException, IOException {
    return IOUtils.toString(new FileReader(releaseNotes));
  }
}
