package ch.ivyteam.bitbucket.tag.maven;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;

import ch.ivyteam.bitbucket.BitBucketClient;
import ch.ivyteam.bitbucket.model.commit.Commit;
import ch.ivyteam.bitbucket.model.tag.Tag;

@Mojo(name="bitbucket-tag")
public class BitBucketTagMojo extends AbstractMojo
{  
  static final String GOAL = "tag";

  @Parameter(property="version")
  private String version;
  
  @Parameter(required = true, property="tagName", defaultValue="v${version}")
  private String tagName;
  
  @Parameter(property="tagMessage", defaultValue="Release ${version}")
  private String tagMessage;
  
  @Parameter(property="branch", defaultValue="master")
  private String branch;
  
  @Parameter(property="repositories")
  private List<String> repositories;
  
  @Parameter( defaultValue = "${settings}", readonly = true, required = true )
  private Settings settings;
  
  @Parameter(required = true, property="serverId")
  private String serverId;
  
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException
  {
    Server server = settings.getServer(serverId);
    if (server == null)
    {
      throw new MojoExecutionException("No server with id "+serverId+" found");
    }
    BitBucketClient client = new BitBucketClient(server.getUsername(), server.getPassword());
    List<String> repos = repositories;
    if (repos.isEmpty())
    {
      repos = getAllRepositoriesThatHaveGivenBranch(client); 
    }

    for (String repository : repos)
    {
      addTag(client, repository);
    }
  }

  private List<String> getAllRepositoriesThatHaveGivenBranch(BitBucketClient client)
  {
    List<String> repos = client.getRepositories();
    return repos
       .stream()
       .filter(repo -> hasBranch(client, repo))
       .collect(Collectors.toList());
  }

  private boolean hasBranch(BitBucketClient client, String repo)
  {
    return client.getBranches(repo).contains(branch);
  }

  private void addTag(BitBucketClient client, String repository) throws MojoExecutionException
  {
    try
    {
      Commit commit = client.getLastCommit(repository, branch);
      if (commit == null)
      {
        throw new MojoExecutionException("Could not add tag "+tagName+"+ to repository "+repository+". No commit found on branch "+branch);
      }
      
      getLog().info("Add tag "+tagName+" to repository "+repository+" on commit "+commit.getShortDisplayString());
      
      client.addTag(repository, new Tag(tagName, tagMessage, commit));
    }
    catch(Throwable ex)
    {
      throw new MojoExecutionException("Could not add tag "+tagName+" to repository "+repository, ex);
    }   
  }
}
