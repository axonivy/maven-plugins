package ch.ivyteam.bitbucket.tag.maven;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
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

@Mojo(name="bitbucket-issue")
public class BitBucketIssueMojo extends AbstractMojo
{  
  static final String GOAL = "issue";

  private static final IssueComparator ISSUE_COMPARATOR = new IssueComparator();

  @Parameter(property="outputFile", defaultValue="${project.build.directory}/issues.txt")
  private File outputFile;

  @Parameter(property="version", required = true)
  private String version;
    
  @Parameter(property="branch", defaultValue="master")
  private String branch;
  
  @Parameter(property="repositories")
  private List<String> repositories;
  
  @Parameter(property="issueRegEx", defaultValue="XIVY-[0-9]+")
  private String issueRegEx;
  
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

    Set<String> issues = new HashSet<>();
    for (String repository : repos)
    {
      issues.addAll(getIssuesOn(client, repository));
    }
    writeToOutputFile(issues);
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
  
  private Set<String> getIssuesOn(BitBucketClient client, String repository)
  {
    Pattern pattern = Pattern.compile(issueRegEx);
    String sinceVersion = getSinceVersion();
    getLog().info("Read commits since "+sinceVersion+" from repository "+ repository);
    Tag tag = client.getTag(repository, "v"+sinceVersion);
    if (tag == null)
    {
      getLog().warn("No tag for version "+sinceVersion+" found in repository " + repository);
      return Collections.emptySet();
    }
    List<Commit> commits = client.getCommitsSince(repository, branch, tag.getTarget().getHash(), 10_000);
    Set<String> issues = toIssues(commits, pattern);
    getLog().info("Found "+commits.size()+" commits and "+issues.size()+" issues since "+sinceVersion+" from repository "+ repository);
    return issues;    
  }

  private Set<String> toIssues(List<Commit> commits, Pattern pattern)
  {
    return commits
        .stream()
        .flatMap(commit -> toIssues(commit, pattern))
        .collect(Collectors.toSet());
  }
  
  private static Stream<String> toIssues(Commit commit, Pattern pattern)
  {
    Set<String> issues = new HashSet<>();
    Matcher matcher = pattern.matcher(commit.getSummary().getRaw());
    while (matcher.find())
    {
      issues.add(matcher.group());
    }
    return issues.stream();
  }
  
  private void writeToOutputFile(Set<String> issues) throws MojoExecutionException
  {
    getLog().info("Writing "+issues.size()+" issues to file "+outputFile);
    List<String> orderedIssues = new ArrayList<>(issues);
    Collections.sort(orderedIssues, ISSUE_COMPARATOR);
    try
    {
      Files.createDirectories(outputFile.getParentFile().toPath());
      try(BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath()))
      {
        writer.append("Issues since "+ getSinceVersion()+":\n");
        for (String issue : orderedIssues)
        {
          writer.append(issue);
          writer.append("\n");
        }
      }
    }
    catch(IOException ex)
    {
      throw new MojoExecutionException("Cannot write issues to output file "+outputFile, ex);
    }   
  }
  
  private String getSinceVersion()
  {
    String raw = StringUtils.substringBefore(version, "-");
    int service = Integer.parseInt(StringUtils.substringAfterLast(raw, "."));
    raw = StringUtils.substringBeforeLast(raw, ".");
    int minor = Integer.parseInt(StringUtils.substringAfterLast(raw, "."));
    raw = StringUtils.substringBeforeLast(raw, ".");
    int major = Integer.parseInt(raw);
    
    // 8.0.4 -> 8.0.3
    String since = major+"."+minor+"."+(service-1);
    if (service == 0 && 
        major%2 == 1)
    {
      if (minor == 1)
      {
        // 9.1.0 -> 8.0.0
        since = (major-1)+"."+(minor-1)+"."+service; 
      }
      else
      {
        // 9.2.0 -> 9.1.0
        since = major+"."+(minor-1)+"."+service;
      }
    }
    return since;
  }

  private static final class IssueComparator implements Comparator<String>
  {
    @Override
    public int compare(String o1, String o2)
    {
      int issueNr1 = Integer.parseInt(StringUtils.substringAfter(o1, "-"));
      int issueNr2 = Integer.parseInt(StringUtils.substringAfter(o2, "-"));
      return issueNr1-issueNr2;
    }
  }
}
