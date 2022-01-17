package ch.ivyteam.ivy.jira.release;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
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
    if (StringUtils.isBlank(newVersion)) {
      getLog().error("aborting: property 'newVersion' is mandatory, but was "+newVersion);
      return;
    }

    JiraReleaseService releases = new JiraReleaseService(server, jiraServerUri);

    List<JiraVersion> versions = releases.ivyVersions();
    Optional<JiraVersion> existing = versions.stream()
      .filter(version -> newVersion.equalsIgnoreCase(version.name))
      .findFirst();
    if (existing.isPresent()) {
      getLog().info("skipping: XIVY version "+newVersion+" exists already "+existing.get().self);
      return;
    }

    JiraVersion created = releases.create(newVersion);
    getLog().info("created new XIVY version "+created.self);

    if (StringUtils.isBlank(afterVersion) && project != null) {
      String version = project.getVersion();
      afterVersion = StringUtils.substringBefore(version, "-");
    }

    if (StringUtils.isNotBlank(afterVersion)) {
      releases.move(newVersion, afterVersion);
      getLog().info("moved "+newVersion+" to occur after "+afterVersion);
    }
  }
}
