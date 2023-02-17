package ch.ivyteam.ivy.changelog.generator;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileReader;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.settings.Server;
import org.apache.maven.shared.model.fileset.FileSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TestChangelogGeneratorMojo {

  @TempDir
  public File tempFolder;

  private String outputPath;

  private ChangelogGeneratorMojo mojo;
  private Server server;

  private File basedir;

  @BeforeEach
  void setup() throws Exception {
    mojo = new ChangelogGeneratorMojo();

    basedir = new File("src/test/resources");
    outputPath = tempFolder.getAbsolutePath() + "/target";
    server = new Server();
    server.setUsername(System.getProperty("jira.username"));
    server.setPassword(System.getProperty("jira.password"));
    server.setId("axonivy.jira");

    mojo.jiraServerUri = "https://axonivy.atlassian.net";
    mojo.jiraServerId = "axonivy.jira";
    mojo.whitelistJiraLabels = "security,performance";
    mojo.fileset = new FileSet();
    mojo.fileset.setDirectory(basedir.getAbsolutePath());
    mojo.fileset.setOutputDirectory(outputPath);
    mojo.wordWrap = 80;
  }

  @Test
  void createChangelog() throws Exception {
    mojo.filterBy = "project IN (XIVY,IVYPORTAL) AND fixVersion = 7.2.0";
    mojo.asciiTemplate = "  * ${key} ${summary}";
    mojo.fileset.addInclude("changelog");
    mojo.compression = "gz";
    mojo.exec(server);

    assertThat(new File(outputPath + "/changelog.gz")).exists();
  }

  @Test
  void createProjectSortedReleaseNotes() throws Exception {
    mojo.filterBy = "project IN (XIVY,IVYPORTAL) AND fixVersion = 7.4.0";
    mojo.asciiTemplate = "${key} ${type}";
    mojo.fileset.addInclude("ReleaseNotes.txt");
    mojo.exec(server);

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
  void createPortalReleaseNotes() throws Exception {
    mojo.filterBy = "project = IVYPORTAL AND fixVersion = 7.4.0 AND issuetype in (Story, Improvement, Bug)";
    mojo.asciiTemplate = "${key} ${type}";
    mojo.fileset.addInclude("ReleaseNotes.txt");
    mojo.exec(server);

    File releaseNotes = new File(outputPath + "/ReleaseNotes.txt");
    assertThat(releaseNotes).exists();

    assertThat(readFileContent(releaseNotes))
            .as("No Technical tasks displayed")
            .doesNotContain("Technical task");
  }

  @Test
  void createOnlyBugReleaseNotes() throws Exception {
    mojo.filterBy = "project = XIVY AND fixVersion = 7.4.0 AND issuetype = Bug";
    mojo.asciiTemplate = "${key} ${type}";
    mojo.fileset.addInclude("ReleaseNotes.txt");
    mojo.exec(server);

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
  void createSortedReleaseNotes() throws Exception {
    mojo.filterBy = "project = XIVY AND fixVersion = 7.4.0";
    mojo.asciiTemplate = "${key}:${type};";
    mojo.fileset.addInclude("ReleaseNotes.txt");
    mojo.exec(server);

    File releaseNotes = new File(outputPath + "/ReleaseNotes.txt");
    assertThat(releaseNotes).exists();

    String issues = StringUtils.substringAfter(readFileContent(releaseNotes),
            "This is a Leading Edge version.");
    assertCorrectlyOrdered(issues.lines().filter(issue -> issue.contains("XIVY-")).toArray(String[]::new));
  }

  @Test
  void createReleaseNotesWithUpgradRecommended() throws Exception {
    String recommendation = createReleaseNotesAndReadRecommendation("7.4.0");
    assertThat(recommendation).contains("We recommend to install this update release because it fixes stability issues!");
  }

  @Test
  void createReleaseNotesWithUpgradeCritical() throws Exception {
    String recommendation = createReleaseNotesAndReadRecommendation("8.0.4");
    assertThat(recommendation).contains("We strongly recommend to install this update release because it fixes security issues!");
  }

  @Test
  void createReleaseNotesWithUpgradeSuggested() throws Exception {
    String recommendation = createReleaseNotesAndReadRecommendation("8.0.3");
    assertThat(recommendation).contains("We suggest to install this update release if you are suffering from any of these issues.");
  }

  private String createReleaseNotesAndReadRecommendation(String version) throws Exception {
    mojo.filterBy = "project = XIVY AND fixVersion = "+version;
    mojo.asciiTemplate = "${key}:${type};";
    mojo.fileset.addInclude("ReleaseNotes.txt");
    mojo.exec(server);

    File releaseNotes = new File(outputPath + "/ReleaseNotes.txt");
    assertThat(releaseNotes).exists();

    String recommendation = StringUtils.substringAfter(readFileContent(releaseNotes), "This is a Leading Edge version.");
    return recommendation;
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

  private String readFileContent(File releaseNotes) throws Exception {
    return IOUtils.toString(new FileReader(releaseNotes));
  }
}
