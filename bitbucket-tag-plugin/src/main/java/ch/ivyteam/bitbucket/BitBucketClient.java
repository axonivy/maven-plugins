package ch.ivyteam.bitbucket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.client.oauth2.ClientIdentifier;
import org.glassfish.jersey.client.oauth2.OAuth2ClientSupport;
import org.glassfish.jersey.client.oauth2.TokenResult;

import ch.ivyteam.bitbucket.model.commit.Commit;
import ch.ivyteam.bitbucket.model.commit.Commits;
import ch.ivyteam.bitbucket.model.repo.Repositories;
import ch.ivyteam.bitbucket.model.repo.Repository;
import ch.ivyteam.bitbucket.model.tag.Tag;

public class BitBucketClient 
{
  private static final String BITBUCKET_ACCESS_TOKEN_URL = "https://bitbucket.org/site/oauth2/access_token";
  private final Client client;
  private final WebTarget target;

  public BitBucketClient(String clientId, String clientSecret)
  {
    String accessToken = authenticateWithOAuth2(new ClientIdentifier(clientId, clientSecret));

    client = ClientBuilder.newClient();
    client.register(OAuth2ClientSupport.feature(accessToken));
    target = client.target("https://api.bitbucket.org/2.0/repositories/axonivy-prod");    
  }

  /**
   * See https://bitbucket.org/atlassian/bb-cloud-client-creds-grant-sample-app/src/master/
   *
   * Get Access Token:
   * <code>curl -X POST -u "4ZNbXJBbDQffWEFq2a:ntEgXZGSJ8JmZSZWdFXg8usGduMsb4Rg" https://bitbucket.org/site/oauth2/access_token -d grant_type=client_credentials</code>
   * -u = BasicAuthentication
   * 
   * Use Access Token:
   * <code>curl -H "Authorization: Bearer {4qpVtAjc1CG96D88Z8rGBBVBgZ1mLMMgNXxBQU6RSFB-OIrRu17B3zF5sldo5N1UfFxijvZpyx5uSObPJ4xjfRYVNk0IhmEiYu_EyBp3pUKcLvbX6dz_bw4n}" https://api.bitbucket.org/2.0/repositories</code>
   * @param clientIdentifier 
   * 
   * @return OAuth access token
   */
  private String authenticateWithOAuth2(ClientIdentifier clientIdentifier)
  {
    Client oAuthClient = ClientBuilder.newClient();

    // registers a response content handler for TokenResult
    OAuth2ClientSupport.authorizationCodeGrantFlowBuilder(clientIdentifier, null, BITBUCKET_ACCESS_TOKEN_URL)
            .client(oAuthClient)
            .build();

    // use client identifier client id and secret to authenticate against Bitbucket (Basic Authentication) 
    HttpAuthenticationFeature basic = HttpAuthenticationFeature.basic(clientIdentifier.getClientId(), clientIdentifier.getClientSecret());
    oAuthClient.register(basic);

    // requests to authenticate with client id and secret
    Form form = new Form();
    form.param("grant_type", "client_credentials");
    Entity<Form> entity = Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE);
    
    TokenResult result = oAuthClient.target(BITBUCKET_ACCESS_TOKEN_URL)
            .request(MediaType.APPLICATION_JSON_TYPE)            
            .post(entity, TokenResult.class);
    
    return result.getAccessToken();
  }
  
  public List<String> getRepositories()
  {
    List<String> repositories = new ArrayList<>();
    Repositories repos = null;
    do
    {
      if (repos == null)
      {
        repos = target
                .queryParam("q", "project.key=\"IVY\"")
                .request()
                .get(Repositories.class);
      }
      else
      {
        repos = client
                .target(repos.getNext())
                .queryParam("q", "project.key=\"IVY\"")
                .request()
                .get(Repositories.class);
      }
      repos.getValues()
          .stream()
          .map(Repository::getName)
          .forEach(repositories::add);
    } while (repos.getNext() != null);
    return repositories;
  }

  public List<Commit> getLastCommits(String repository, String branch)
  {
    try
    {
      return target
          .path(repository)
          .path("commits")
          .path(branch)
          .request()
          .get(Commits.class)
          .getValues();
    }
    catch(NotFoundException ex)
    {
      return Collections.emptyList();
    }
  }

  public Commit getLastCommit(String repository, String branch)
  {
    return getLastCommits(repository, branch)
        .stream()
        .findFirst()
        .orElse(null);
  }
  
  public void addTag(String repository, Tag tag)
  {
    Entity<Tag> entity = Entity.entity(tag, MediaType.APPLICATION_JSON);
    Builder request = target
        .path(repository)
        .path("refs")
        .path("tags")
        .request();
    try(Response response = request.post(entity))
    {
      checkResponse(response, "Failed to add tag "+tag+" to repository "+repository);
    }
  }

  public void deleteTag(String repository, String tagName)
  {
    Builder request = target
            .path(repository)
            .path("refs")
            .path("tags")
            .path(tagName)
            .request();
    try(Response response = request.delete())
    {
      checkResponse(response, "Failed to delete tag "+tagName+" from repository "+repository);
    }
  }

  private void checkResponse(Response response, String message)
  {
    if (response.getStatusInfo().getFamily() != Family.SUCCESSFUL)
    {
      throw new RuntimeException(message+". Status Code "+response.getStatusInfo().getStatusCode() +"  "+response.getStatusInfo().getReasonPhrase());
    }
  }
}
