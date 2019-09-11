package ch.ivyteam.ivy.changelog.generator;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.shared.model.fileset.FileSet;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TestChangelogGeneratorMojo
{
  @Rule
  public MojoRule rule = new MojoRule();
  
  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  private String outputPath;

  private ChangelogGeneratorMojo mojo;

  private File basedir;
  
  @Before
  public void setup() throws Exception
  {
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
  public void createChangelog() throws Exception
  {
    mojo.fixVersion = "7.2.0";
    mojo.jiraProjects = "XIVY,IVYPORTAL";
    mojo.asciiTemplate = "  * ${key} ${summary}";
    mojo.fileset.addInclude("changelog");
    mojo.compression = "gz";
    mojo.execute();
    
    assertThat(new File(outputPath + "/changelog.gz")).exists();
  }

  @Test
  public void createSortedReleaseNotes() throws Exception
  {
    mojo.fixVersion = "7.4.0";
    mojo.jiraProjects = "XIVY";
    mojo.asciiTemplate = "${key}";
    mojo.fileset.addInclude("ReleaseNotes.txt");
    mojo.execute();

    File releaseNotes = new File(outputPath + "/ReleaseNotes.txt");
    assertThat(releaseNotes).exists();

    String issues = StringUtils.substringAfter(readFileContent(releaseNotes), "This is a Leading Edge version.");
    String[] splitIssues = StringUtils.split(issues, "XIVY-");

    for (int i = 0; i < splitIssues.length - 1; i++)
    {
      Integer issueNumber = Integer.valueOf(splitIssues[i]);
      Integer nextIssueNumber = Integer.valueOf(splitIssues[i + 1]);
      assertThat(issueNumber)
              .as("Next issue number must be higher")
              .isLessThan(nextIssueNumber);
    }
  }

  private String readFileContent(File releaseNotes) throws IOException, FileNotFoundException
  {
    try (BufferedReader br = new BufferedReader(new FileReader(releaseNotes)))
    {
      String fileContent = "";
      String sCurrentLine;
      while ((sCurrentLine = br.readLine()) != null)
      {
        fileContent += sCurrentLine;
      }
      return fileContent;
    }
  }
}
