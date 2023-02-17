package ch.ivyteam.ivy.jira.release;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.apache.maven.settings.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class TestJiraReleaseService {

  private JiraReleaseService releases;

  @BeforeEach
  void setUp() {
    Server server = new Server();
    server.setUsername(System.getProperty("jira.username"));
    server.setPassword(System.getProperty("jira.password"));
    releases = new JiraReleaseService(server, "https://axonivy.atlassian.net");
  }

  @Test
  void readVersions() {
    List<JiraVersion> versions = releases.ivyVersions();
    assertThat(versions).isNotEmpty();
  }

  @Test
  void readSpecific() {
    JiraVersion version = releases.version("8.0.23");
    assertThat(version).isNotNull();
    assertThat(version.id).isGreaterThan(1);
    assertThat(version.name).isEqualTo("8.0.23");
  }

  @Test
  @Disabled("crud op that changes running instance")
  void createMove() {
    JiraVersion v29 = releases.create("8.0.29");
    releases.move(v29.name, "8.0.24");
  }

  @Test
  @Disabled("crud op that changes running instance")
  void move() {
    releases.move("8.0.29", "8.0.24");
  }
}
