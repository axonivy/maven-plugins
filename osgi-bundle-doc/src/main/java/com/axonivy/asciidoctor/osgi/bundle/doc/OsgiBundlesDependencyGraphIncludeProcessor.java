package com.axonivy.asciidoctor.osgi.bundle.doc;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.asciidoctor.ast.DocumentRuby;
import org.asciidoctor.extension.IncludeProcessor;
import org.asciidoctor.extension.PreprocessorReader;

public class OsgiBundlesDependencyGraphIncludeProcessor extends IncludeProcessor
{
  public OsgiBundlesDependencyGraphIncludeProcessor(Map<String, Object> config)
  {
    super(config);
  }

  @Override
  public void process(DocumentRuby document, PreprocessorReader reader, String target,
          Map<String, Object> attributes)
  {
    StringBuilder builder = new StringBuilder();
    builder.append("[graphviz, Bundles dependency graph, svg]\n");
    builder.append("----\n");
    builder.append("digraph context {\n");
    BundleInfoScanner scanner = BundleInfoScanner.scanDirectory(new File("target/generated-docs/bundles"));
    Map<String, OsgiBundleManifest> manifests = new HashMap<>();
    for (BundleInfo bundle : scanner.getNonTestBundles())
    {
      OsgiBundleManifest manifest = bundle.getManifest();
      builder.append("  \"");
      builder.append(manifest.getName());
      builder.append("\" [shape=box]\n");
      manifests.put(manifest.getSymbolicName(), manifest);
    }
    
    for (BundleInfo bundle : scanner.getNonTestBundles())
    {
      OsgiBundleManifest manifest = bundle.getManifest();
      for (String requiredBundleName : manifest.getRequireBundles())
      {
        BundleInfo requiredBundle = scanner.getBundleOf(requiredBundleName);
        if (requiredBundle != null)
        {
          builder.append("  \"");
          builder.append(manifest.getName());
          builder.append("\" -> \"");
          builder.append(requiredBundle.getManifest().getName());
          builder.append("\"\n");
        }
      }
    }
    builder.append("}\n");
    builder.append("----\n");
    
    reader.push_include(builder.toString(), target, target, 1, attributes);
  }



  @Override
  public boolean handles(String target)
  {
    return target.toUpperCase().endsWith("BUNDLES_GRAPH");
  }
}
