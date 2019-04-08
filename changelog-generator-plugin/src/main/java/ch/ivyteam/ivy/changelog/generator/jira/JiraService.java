package ch.ivyteam.ivy.changelog.generator.jira;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Server;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJsonProvider;

import com.fasterxml.jackson.databind.DeserializationFeature;

import ch.ivyteam.ivy.changelog.generator.jira.JiraResponse.Issue;
import ch.ivyteam.ivy.changelog.generator.jira.JiraResponse.IssueType;

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
  
  public List<Issue> getIssuesWithFixVersion(String fixVersion, String projectsCommaSeparated)
  {
    String convertedFixVersion = removeEndingZero(fixVersion);
    
    Client client = createClient();
    List<Issue> issues = readIssues(client, convertedFixVersion, projectsCommaSeparated).issues.stream()
            .map(i -> { i.serverUri = serverUri; return i; })
            .sorted(createComparator())
            .collect(Collectors.toList());
    
    return orderIssuesByProjects(projectsCommaSeparated, issues);
  }

  private List<Issue> orderIssuesByProjects(String projectsCommaSeparated, List<Issue> unsortedIssues)
  {
    List<String> projects = Arrays.stream(projectsCommaSeparated.split(","))
            .map(StringUtils::trimToEmpty)
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.toList());
    
    List<Issue> issues = new ArrayList<>();
    for (String project : projects)
    {
      issues.addAll(unsortedIssues.stream().filter(i -> i.getProjectKey().equalsIgnoreCase((project))).collect(Collectors.toList()));
    }
    return issues;
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

  private static Comparator<Issue> createComparator()
  {
    Comparator<Issue> byType = (e1, e2) -> Objects.compare(e1.getIssueType(), e2.getIssueType(), IssueType::compareTo);
    Comparator<Issue> byKey = (e1, e2) -> e1.getType().compareTo(e2.getType());
    return byType.thenComparing(byKey);
  }

}
