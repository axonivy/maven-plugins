package ch.ivyteam.bitbucket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

import ch.ivyteam.bitbucket.model.commit.Commit;
import ch.ivyteam.bitbucket.model.commit.Commits;
import ch.ivyteam.bitbucket.model.repo.Repositories;
import ch.ivyteam.bitbucket.model.repo.Repository;
import ch.ivyteam.bitbucket.model.tag.Tag;

public class BitBucketClient 
{
  private final Client client;
  private final WebTarget target;

  public BitBucketClient(String userName, String password)
  {
    HttpAuthenticationFeature basic = HttpAuthenticationFeature.basic(userName, password);
    client = ClientBuilder.newClient();
    client.register(basic);
    target = client.target("https://api.bitbucket.org/2.0/repositories/axonivy-prod");
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

  /**
   * Options:
   * add userName password tagRelease [branch]
   * remove userName password tagRelease
   * @param args
   */
  public static void main(String[] args)
  {       
    BitBucketClient client = new BitBucketClient(args[1], args[2]);
    if ("add".equals(args[0]))
    {
      String branch = args.length == 5 ? args[4] : "master";
      try(Scanner scanner = new Scanner(System.in))
      {
        for(String repository : client.getRepositories())
        {
          Commit commit = client.getLastCommit(repository, branch);
          if (commit != null)
          {
            System.out.println();
            System.out.println("Repository "+repository+": "+commit.getShortDisplayString());
            System.out.println("Tag?");
            
            String answer = scanner.nextLine();
            if (answer.equalsIgnoreCase("y"))
            {
              client.addTag(repository, new Tag("v"+args[3], "Release "+args[3], commit));
            }
          }
          else
          {
            System.out.println("No commit found on repository "+repository);
          }
        }
      }
    }
    else if ("delete".equalsIgnoreCase(args[0]))
    {
      for (String repos : client.getRepositories())
      {
        try
        {
          System.out.println("Delete tag v"+args[3]+" on repository "+repos);
          client.deleteTag(repos, "v"+args[3]);
        }
        catch(Exception ex)
        {
          System.err.println("Cannot delete tag v"+args[3]+" on repository "+repos);
        }
      }
    }
  }
}
