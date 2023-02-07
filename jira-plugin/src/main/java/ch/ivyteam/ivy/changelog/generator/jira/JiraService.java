package ch.ivyteam.ivy.changelog.generator.jira;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Server;

import ch.ivyteam.ivy.changelog.generator.jira.JiraResponse.Issue;
import ch.ivyteam.ivy.jira.JiraClientFactory;

public class JiraService {
  private final String serverUri;
  private final Server server;
  private final Log log;

  public JiraService(String serverUri, Server server, Log log) {
    this.serverUri = serverUri;
    this.server = server;
    this.log = log;
  }

  public List<Issue> queryIssues(JiraQuery query) {
    Client client = JiraClientFactory.createClient(server);
    List<Issue> issues = readIssues(jqlTarget(client, query)).stream()
            .map(i -> {
              i.serverUri = serverUri;
              return i;
            })
            .collect(Collectors.toList());
    return issues;
  }

  private WebTarget jqlTarget(Client client, JiraQuery query) {
    return client
            .target(serverUri)
            .path("rest/api/2/search")
            .queryParam("jql", query.toJql());
  }

  private List<Issue> readIssues(WebTarget target) {
    List<Issue> issues = new ArrayList<>();

    Paging read = new Paging(0);
    do {
      JiraResponse response = readPaged(target, read);
      issues.addAll(response.issues);
      read = response.page();
    } while (read.hasNext() && (read = read.next()) != null);

    return issues;
  }

  private JiraResponse readPaged(WebTarget target, Paging jiraWindow) {
    WebTarget pagedTarget = target.queryParam("startAt", jiraWindow.startAt)
                   .queryParam("maxResults", jiraWindow.maxResults);
    log.info("GET: " + pagedTarget.getUri());
    Response response = pagedTarget.request().get();
    if (!response.getStatusInfo().getFamily().equals(Family.SUCCESSFUL)) {
      throw new RuntimeException(response.getStatusInfo().getStatusCode() + " "
              + response.getStatusInfo().getReasonPhrase() + " " + response.readEntity(String.class));
    }
    return response.readEntity(JiraResponse.class);
  }

}
