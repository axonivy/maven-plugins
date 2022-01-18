package ch.ivyteam.ivy.jira.release;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;

@Mojo(name = "jira-newVersion", requiresProject = false)
public class NewReleaseVersionMojo extends AbstractMojo {

  /** server id which is configured in settings.xml */
  @Parameter(property = "jiraServerId")
  public String jiraServerId;

  /** jira base url */
  @Parameter(property = "jiraServerUri", defaultValue = "https://axonivy.atlassian.net")
  public String jiraServerUri;

  /** the new version to introduce in jira*/
  @Parameter(property = "newVersion", required = true)
  public String newVersion;

  /** where to move the 'newVersion' onto. */
  @Parameter(property = "afterVersion", required = false)
  public String afterVersion;

  @Parameter(property = "project", required = false, readonly = true)
  MavenProject project;
  @Parameter(property = "session", required = true, readonly = true)
  MavenSession session;

  @Parameter(property = "skip.jira", required = false)
  public boolean skipJira;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (skipJira) {
      getLog().info("skipping: jira release version creation");
      return;
    }
    Server server = session.getSettings().getServer(jiraServerId);
    if (server == null) {
      getLog().warn("skipping: serverId '" + jiraServerId + "' is not definied in setting.xml");
      return;
    }

    Optional<String> next = ReleaseVersion.parse(newVersion);
    if (next.isEmpty()) {
      getLog().error("aborting: property 'newVersion' is mandatory, but was "+newVersion);
      return;
    }

    if (StringUtils.isBlank(afterVersion) && project != null) {
      afterVersion = project.getVersion();
    }
    Optional<String> after = ReleaseVersion.parse(afterVersion);

    JiraReleaseService releases = new JiraReleaseService(server, jiraServerUri);
    createVersion(releases, newVersion, after, getLog());
  }

  private static void createVersion(JiraReleaseService releases, String newVersion, Optional<String> afterVersion, Log log) {
    List<JiraVersion> versions = releases.ivyVersions();
    Optional<JiraVersion> existing = versions.stream()
      .filter(version -> newVersion.equalsIgnoreCase(version.name))
      .findFirst();
    if (existing.isPresent()) {
      log.info("skipping: XIVY version "+newVersion+" exists already "+existing.get().self);
      return;
    }

    JiraVersion created = releases.create(newVersion);
    log.info("created new XIVY version "+created.self);


    if (afterVersion.isPresent()) {
      releases.move(newVersion, afterVersion.get());
      log.info("moved "+newVersion+" to occur after "+afterVersion.get());
    }
  }

}
