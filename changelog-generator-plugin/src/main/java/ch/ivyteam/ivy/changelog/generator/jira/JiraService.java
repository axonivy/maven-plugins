package ch.ivyteam.ivy.changelog.generator.jira;

import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Server;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJsonProvider;

import com.fasterxml.jackson.databind.DeserializationFeature;

import ch.ivyteam.ivy.changelog.generator.jira.JiraResponse.Issue;

public class JiraService
{
  private final String serverUri;
  private final Server server;
  private final Log log;
  
  public JiraService(String serverUri, Server server, Log log)
  {
    this.serverUri = serverUri;
    this.server = server;
    this.log = log;
  }
  
  public List<Issue> queryIssues(JiraQuery query)
  {
    Client client = createClient();
    List<Issue> issues = readIssues(client, query).issues.stream()
            .map(i -> { i.serverUri = serverUri; return i; })
            .collect(Collectors.toList());
    return issues;
  }

  private JiraResponse readIssues(Client client, JiraQuery query)
  {
    final int unlimited = -1;
    WebTarget target = client
            .target(serverUri)
            .path("rest/api/2/search")
            .queryParam("maxResults", unlimited) 
            .queryParam("jql", query.toJql());
    log.info("GET: "+target.getUri());
    Response response = target.request().get();
    if (!response.getStatusInfo().getFamily().equals(Family.SUCCESSFUL))
    {
      throw new RuntimeException(response.getStatusInfo().getStatusCode() + " " + response.getStatusInfo().getReasonPhrase() + " " + response.readEntity(String.class));
    }
     return response.readEntity(JiraResponse.class);
  }

  private Client createClient()
  {
    final JacksonJsonProvider jacksonJsonProvider = new JacksonJaxbJsonProvider()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    Client client = ClientBuilder.newClient(new ClientConfig(jacksonJsonProvider));
    
    if (server != null)
    {
      client.register(HttpAuthenticationFeature.basic(server.getUsername(), server.getPassword()));
    }

    return client;
  }

}
