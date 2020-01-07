package com.axonivy.asciidoctor.osgi.bundle.doc;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BundleInfoScanner
{
  private Map<String, BundleInfo> bundles = new HashMap<>();
  private final File workspaceDirectory;
  
  private BundleInfoScanner(File workspaceDirectory)
  {
    if (workspaceDirectory.exists())
    {
      this.workspaceDirectory = workspaceDirectory;
    }
    else
    {
      this.workspaceDirectory = new File("techdoc/" + workspaceDirectory.getPath());
    }
    if (!this.workspaceDirectory.exists())
    {
      throw new RuntimeException("workspaceDirectory does not exist " + this.workspaceDirectory.getAbsolutePath());
    }
  }

  public static BundleInfoScanner scanDirectory(File directory)
  {
    BundleInfoScanner scanner = new BundleInfoScanner(directory);
    scanner.scan();
    return scanner;
  }
  
  public List<BundleInfo> getAllBundles()
  {
    ArrayList<BundleInfo> bundleList = new ArrayList<>(bundles.values());
    Collections.sort(bundleList, (left, right) -> left.getManifest().getName().compareTo(right.getManifest().getName()));
    return bundleList;
  }
  
  public List<BundleInfo> getNonTestBundles()
  {
    return getAllBundles()
            .stream()
            .filter(bundle -> !bundle.getManifest().isTest())
            .collect(Collectors.toList());
  }
  
  public BundleInfo getBundleOf(String bundleSymbolicName)
  {
    return bundles.get(bundleSymbolicName);
  }
    
  private void scan()
  {
    for (File bundleDir : workspaceDirectory.listFiles())
    {
      BundleInfo bundle = BundleInfo.read(bundleDir);
      if (bundle != null)
      {
        bundles.put(bundle.getManifest().getSymbolicName(), bundle);
      }
    }
  }
}
