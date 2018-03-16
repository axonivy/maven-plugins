package ch.ivyteam.ivy.changelog.generator.jira;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.apache.maven.settings.Server;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import ch.ivyteam.ivy.changelog.generator.jira.JiraResponse.Issue;
import ch.ivyteam.ivy.changelog.generator.jira.JiraResponse.IssueType;

public class JiraService
{
  private final String serverUri;
  private final Server server;
  
  public JiraService(String serverUri, Server server)
  {
    this.serverUri = serverUri;
    this.server = server;
  }
  
  public List<Issue> getIssuesWithFixVersion(String fixVersion, String projects)
  {
    String convertedFixVersion = removeEndingZero(fixVersion);
    
    Client client = createClient();
    return readIssues(client, convertedFixVersion, projects).issues.stream()
            .map(i -> { i.serverUri = serverUri; return i; })
            .sorted(createComparator())
            .collect(Collectors.toList());
  }
  
  /**
   * The version string must be converted, because the we tag issues differently
   * if it is part of a minor release or maintenance release.
   * 
   * Example Minor Releases: 7.0, 7.1, 7.2
   * 
   * Example Maintenance Releases: 7.0.1, 7.0.2, 7.1.1
   * 
   * Converts 7.1.0 to 7.1
   * Converts 7.0.0 to 7.0
   * 
   * @param fixVersion
   * @return converted version
   */
  private static String removeEndingZero(String fixVersion)
  {
    if (fixVersion.length() > 3 && fixVersion.endsWith(".0"))
    {
      return fixVersion.substring(0, fixVersion.length() - 2);
    }
    return fixVersion;
  }

  private JiraResponse readIssues(Client client, String fixVersion, String projects)
  {
    final int unlimited = -1;
    WebTarget target = client
            .target(serverUri)
            .path("rest/api/2/search")
            .queryParam("maxResults", unlimited) 
            .queryParam("jql", "fixVersion = " + fixVersion + " and project in (" + projects + ") order by key");
     
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

  private static Comparator<Issue> createComparator()
  {
    Comparator<Issue> byProject = (e1, e2) -> e1.getProjectKey().compareTo(e2.getProjectKey());
    Comparator<Issue> byType = (e1, e2) -> Objects.compare(e1.getIssueType(), e2.getIssueType(), IssueType::compareTo);
    Comparator<Issue> byKey = (e1, e2) -> e1.getType().compareTo(e2.getType());
    return byProject.thenComparing(byType).thenComparing(byKey);
  }
}
