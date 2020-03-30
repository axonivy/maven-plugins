package ch.ivyteam.bitbucket.cli;

import org.apache.commons.lang3.StringUtils;

import ch.ivyteam.bitbucket.BitBucketClient;
import ch.ivyteam.bitbucket.model.commit.Commit;
import ch.ivyteam.bitbucket.model.tag.Tag;

public class BitBucketClientCli
{
  private final BitBucketClient client;
  private String[] args;
  private int currentArg = 0;

  public BitBucketClientCli(String[] args)
  {
    this.args = args;
    String clientId = getNextArgument(null);
    String secret = getNextArgument(null);
    if (StringUtils.isNotBlank(clientId) &&
        StringUtils.isNotBlank(secret))
    {
      client = new BitBucketClient(clientId, secret);
    }
    else
    {
      client = null;
    }
  }

  /**
   * Options:
   * {client id} {client secret} add tag {tagRelease} {repository} [branch]
   * {client id} {client secret} remove tag {tagRelease} {repository}
   * {client id} {client secret} list repos
   * {client id} {client secret} list branches repo
   * help
   * @param args
   */
  public static void main(String[] args)
  {       
    new BitBucketClientCli(args).main();
  }
  
  private void main()
  {
    String command = getNextArgument("help");
    switch(command)
    {
      case "add":
        add();
        break;
      case "remove":
        remove();
        break;
      case "list":
        list();
        break;
      default:
        printHelp();
        break;
    }
  }

  private String getNextArgument(String defaultValue)
  {
    if (currentArg >= args.length)
    {
      return defaultValue;
    }
      
    currentArg++;
    return args[currentArg-1];
  }

  private void add()
  {
    String command = getNextArgument(null);
    String tag = getNextArgument(null);
    String repo = getNextArgument(null);
    String branch = getNextArgument("master");
            
    if ("tag".equals(command) && 
        StringUtils.isNotBlank(tag) && 
        StringUtils.isNotBlank(repo))
    {
      Commit commit = client.getLastCommit(repo, branch);
      if (commit != null)
      {
        System.out.println("Add tag "+tag+" to repository "+repo+" and commit "+commit.getShortDisplayString());
        client.addTag(repo, new Tag(tag, "Tag "+tag, commit));
      }
      else
      {
        System.err.println("No commit found on repository that can be tagged"+repo);
      }
    }
    else
    {
      printAddHelp();
    }
  }
  
  private void remove()
  {
    String command = getNextArgument(null);
    String tag = getNextArgument(null);
    String repo = getNextArgument(null);

    if (args.length < 4)
    {
      printRemoveHelp();
    }
    
    if ("tag".equals(command) && 
        StringUtils.isNotBlank(tag) && 
        StringUtils.isNotBlank(repo))
    {
      System.out.println("Delete tag "+tag+" on repository "+repo);
      client.deleteTag(repo, tag);
    }
    else
    {
      printRemoveHelp();
    }
  }
  
  private void list()
  {
    String command = getNextArgument("help");
    switch(command)
    {
      case "repos":
      case "repositories":
        listRepos();
        break;
      case "branches":
        listBranches();
        break;
      default:
        printListHelp();
        break;
    }
  }
 
  private void listRepos()
  {
    System.out.println("Repositories:");
    client
        .getRepositories()
        .stream()
        .map(repo -> " "+repo)
        .forEach(System.out::println);
  }

  private void listBranches()
  {
    String repo = getNextArgument(null);
    if (StringUtils.isNotBlank(repo))
    {
      System.out.println("Branches of repository "+repo+":");
      client
          .getBranches(repo)
          .stream()
          .map(branch -> " "+branch)
          .forEach(System.out::println);
    }
    else
    {
      printListBranchesHelp();
    }    
  }

  private void printHelp()
  {
    System.out.println("Options:");
    printAddHelp();
    printRemoveHelp();
    printListHelp();
    System.out.println(" help");
  }

  private void printAddHelp()
  {
    System.out.println(" <client id> <client secret> add tag <tagName> <repo> [<branch>]");
  }
  
  private void printRemoveHelp()
  {
    System.out.println(" <client id> <client secret> remove tag <tagName> <repo>");
  }

  private void printListHelp()
  {
    printListReposHelp();
    printListBranchesHelp();
  }

  private void printListReposHelp()
  {
    System.out.println(" <client id> <client secret> list repos");
  }

  private void printListBranchesHelp()
  {
    System.out.println(" <client id> <client secret> list branches <repo>");
  }
}
