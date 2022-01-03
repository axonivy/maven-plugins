package ch.ivyteam.ivy.jira.release;

import java.net.URI;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.maven.settings.Server;

import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ivyteam.ivy.jira.JiraClientFactory;

public class JiraReleaseService {

  private final Client client;
  private final WebTarget jiraApi;

  public JiraReleaseService(Server server, String serverUri) {
    this.client = JiraClientFactory.createClient(server);
    this.jiraApi = client.target(serverUri).path("rest/api/2");
  }

  JiraVersion version(String name) {
    List<JiraVersion> versions = ivyVersions();
    return select(name, versions);
  }

  public List<JiraVersion> ivyVersions() {
    return projectVersions("XIVY");
  }

  private JiraVersion select(String name, List<JiraVersion> versions) {
    return versions.stream()
      .filter(version -> name.equalsIgnoreCase(version.name))
      .findFirst()
      .orElseThrow(()-> new RuntimeException("version "+name+" not found in "+versions));
  }

  List<JiraVersion> projectVersions(String projectName) {
    WebTarget versionsResource = jiraApi.path("project/{idOrName}/versions").resolveTemplate("idOrName", projectName);
    var resultType = new GenericType<List<JiraVersion>>() {};
    return versionsResource.request(MediaType.APPLICATION_JSON).get(resultType);
  }

  public JiraVersion create(String newVersion) {
    var create = JiraVersion.newXivy(newVersion);
    return jiraApi.path("version").request()
      .accept(MediaType.APPLICATION_JSON)
      .post(Entity.json(create), JiraVersion.class);
  }

  public void move(String newVersionName, String afterVersionName) {
    List<JiraVersion> versions = ivyVersions();
    JiraVersion newV = select(newVersionName, versions);
    JiraVersion afterV = select(afterVersionName, versions);
    move(newV.id, afterV.self);
  }

  private void move(int versionId, URI after) {
    WebTarget moveRes = jiraApi.path("version/{versionId}/move").resolveTemplate("versionId", versionId);
    Response response = moveRes.request().post(Entity.json(new MoveAction(after)));
    response.getStatusInfo().getFamily();
  }

  private static class MoveAction {

    @JsonProperty
    private final URI after;

    public MoveAction(URI after) {
      this.after = after;
    }
  }

}
