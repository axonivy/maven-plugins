package ch.ivyteam.ivy.changelog.generator;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;

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
  
  @Before
  public void setup() throws IOException
  {
    tempFolder.create();
  }
  
  @Test
  public void createChangelog() throws Exception
  {
    File basedir = new File("src/test/resources");
    String outputPath = tempFolder.getRoot().getAbsolutePath() + "/target";
    MavenProject project = rule.readMavenProject(basedir);
    MavenSession session = rule.newMavenSession(project);
    MojoExecution execution = rule.newMojoExecution("generate-changelog");
    Server server = new Server();
    server.setUsername(System.getProperty("jira.username"));
    server.setPassword(System.getProperty("jira.password"));
    server.setId("axonivy.jira");
    session.getSettings().addServer(server);
    ChangelogGeneratorMojo mojo = (ChangelogGeneratorMojo) rule.lookupConfiguredMojo( session, execution );
    
    mojo.jiraServerId = "axonivy.jira";
    mojo.fixVersion = "7.2.0";
    mojo.jiraProjects = "XIVY,IVYPORTAL";
    mojo.asciiTemplate = "  * ${key} ${summary}";
    mojo.whitelistJiraLabels = "security,performance";
    mojo.fileset = new FileSet();
    mojo.fileset.setDirectory(basedir.getAbsolutePath());
    mojo.fileset.addInclude("changelog");
    mojo.fileset.setOutputDirectory(outputPath);
    mojo.compression = "gz";
    mojo.wordWrap = 80;
    mojo.execute();
    
    assertThat(new File(outputPath + "/changelog.gz")).exists();
  }
}
