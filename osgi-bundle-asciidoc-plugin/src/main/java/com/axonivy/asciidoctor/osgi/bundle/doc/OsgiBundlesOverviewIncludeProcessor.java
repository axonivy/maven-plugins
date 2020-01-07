package com.axonivy.asciidoctor.osgi.bundle.doc;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.asciidoctor.ast.DocumentRuby;
import org.asciidoctor.extension.IncludeProcessor;
import org.asciidoctor.extension.PreprocessorReader;

public class OsgiBundlesOverviewIncludeProcessor extends IncludeProcessor
{
  public OsgiBundlesOverviewIncludeProcessor(Map<String, Object> config)
  {
    super(config);
  }

  @Override
  public void process(DocumentRuby document, PreprocessorReader reader, String target,
          Map<String, Object> attributes)
  {
    StringBuilder builder = new StringBuilder();
    builder.append(".Bundles\n");
    builder.append("|===\n");
    builder.append("|Name|SymbolicName|Description|Version|Vendor|Requires\n");
    builder.append("\n");
    File workspaceDirectory = new File("target/generated-docs/bundles");
    BundleInfoScanner scanner = BundleInfoScanner.scanDirectory(workspaceDirectory);
    for (BundleInfo bundle : scanner.getAllBundles())
    {
      String techDocRelativePath = bundle.getTechnicalDocumentationWorkspaceRelativPath();
      OsgiBundleManifest manifest = bundle.getManifest();
      String name;
      if (techDocRelativePath != null)
      {
        name = "[["+manifest.getSymbolicName()+"]] link:bundles/"+techDocRelativePath+"["+manifest.getName()+"]";
      }
      else
      {
        name = "[["+manifest.getSymbolicName()+"]]"+manifest.getName();
      }
      appendCell(builder, name);
      appendCell(builder, manifest.getSymbolicName());
      appendCell(builder, bundle.getPom().getDescription());
      appendCell(builder, manifest.getVersion());
      appendCell(builder, manifest.getVendor());
      appendRequires(builder, manifest.getRequireBundles());
      builder.append("\n");
    }
    builder.append("|===\n");
    reader.push_include(builder.toString(), target, target, 1, attributes);
  }

  private void appendRequires(StringBuilder builder, List<String> requireBundles)
  {
    builder.append("|");
    for (String bundle : requireBundles)
    {
      builder.append("<<"+bundle+","+bundle+">>");
      builder.append(", ");
    }
    builder.append("\n");
  }

  private void appendCell(StringBuilder builder, String value)
  {
    builder.append("|");
    builder.append(value);
    builder.append("\n");
  }

  @Override
  public boolean handles(String target)
  {
    return target.toUpperCase().endsWith("BUNDLES_TABLE");
  }
}
