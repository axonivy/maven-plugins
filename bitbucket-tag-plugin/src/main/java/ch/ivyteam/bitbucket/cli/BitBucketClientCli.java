package ch.ivyteam.bitbucket.cli;

import java.util.Collections;
import java.util.List;

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
   * {client id} {client secret} add tag {tagName} {repository} [branch]
   * {client id} {client secret} remove tag {tagName} [repository]
   * {client id} {client secret} list repos
   * {client id} {client secret} list branches {repository}
   * {client id} {client secret} list tags {repository}
   * {client id} {client secret} list commits {repository} [branch] [since commit hash]
   * {client id} {client secret} info tag {repository} {tagName}
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
      case "info":
        info();
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

    if (args.length < 3)
    {
      printRemoveHelp();
    }
    
    if ("tag".equals(command) && 
        StringUtils.isNotBlank(tag))
    {
      List<String> repositories;
      if (StringUtils.isBlank(repo))
      { 
        repositories = client.getRepositories();
      }
      else
      {
        repositories = Collections.singletonList(repo);
      }
      for (String repository : repositories)
      {
        System.out.println("Delete tag "+tag+" in repository "+repository);
        if (client.getTag(repository, tag) != null)
        {
          client.deleteTag(repository, tag);
        }
        else
        {
          System.err.println("Tag "+tag +" not found in repository "+repository);
        }
      }
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
      case "commits":
        listCommits();
        break;
      case "tags":
        listTags();
        break;
      default:
        printListHelp();
        break;
    }
  }
  
  private void info()
  {
    String command = getNextArgument("help");
    switch(command)
    {
      case "tag":
        infoTag();
        break;
      default:
        printInfoHelp();
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

  private void listTags()
  {
    String repo = getNextArgument(null);
    if (StringUtils.isNotBlank(repo))
    {
      System.out.println("Tags of repository "+repo+":");
      client
          .getTags(repo)
          .stream()
          .map(tag -> " "+tag)
          .forEach(System.out::println);
    }
    else
    {
      printListTagsHelp();
    }    
  }

  private void listCommits()
  {
    String repo = getNextArgument(null);
    String branch = getNextArgument("master");
    String since = getNextArgument(null);
    if (StringUtils.isNotBlank(repo))
    {
      if (StringUtils.isNotBlank(since))
      {
        System.out.println("Commits of repository "+repo+" and branch "+branch+" since "+since+":");
        client
        .getCommitsSince(repo, branch, since, 1000)
        .stream()
        .map(commit -> commit.getShortDisplayString())
        .map(commit -> " "+commit)
        .forEach(System.out::println);
      }
      else
      {
        System.out.println("Commits of repository "+repo+" and branch "+branch+":");
        client
            .getLastCommits(repo, branch)
            .stream()
            .map(commit -> commit.getShortDisplayString())
            .map(commit -> " "+commit)
            .forEach(System.out::println);
      }
    }
    else
    {
      printListCommitsHelp();
    }    
  }
  
  private void infoTag()
  {
    String repo = getNextArgument(null);
    String tagName = getNextArgument(null);
    if (StringUtils.isNotBlank(repo) && 
        StringUtils.isNotBlank(tagName))
    {
      System.out.println("Tag "+tagName+" of repository "+repo+":");
      Tag tag = client.getTag(repo, tagName);
      System.out.print(" Name: ");
      System.out.println(tag.getName());
      System.out.print(" Message: ");
      System.out.println(tag.getMessage());
      System.out.print(" Commit: ");
      System.out.println(tag.getTarget().getHash());
    }
    else
    {
      printInfoTagHelp();
    }    
  }

  private void printHelp()
  {
    System.out.println("Options:");
    printAddHelp();
    printRemoveHelp();
    printListHelp();
    printInfoHelp();
    System.out.println(" help");
  }

  private void printAddHelp()
  {
    System.out.println(" <client id> <client secret> add tag <tagName> <repo> [<branch>]");
  }
  
  private void printRemoveHelp()
  {
    System.out.println(" <client id> <client secret> remove tag <tagName> [<repo>]");
  }

  private void printListHelp()
  {
    printListReposHelp();
    printListBranchesHelp();
    printListTagsHelp();
    printListCommitsHelp();
  }

  private void printListReposHelp()
  {
    System.out.println(" <client id> <client secret> list repos");
  }

  private void printListBranchesHelp()
  {
    System.out.println(" <client id> <client secret> list branches <repo>");
  }
  
  private void printListTagsHelp()
  {
    System.out.println(" <client id> <client secret> list tags <repo>");
  }

  private void printListCommitsHelp()
  {
    System.out.println(" <client id> <client secret> list commits <repo> [<branch>] [<since commit hash>]");
  }
  
  private void printInfoHelp()
  {
    printInfoTagHelp();
  }

  private void printInfoTagHelp()
  {
    System.out.println(" <client id> <client secret> info tag <repo> <tagName>");
  }

}
