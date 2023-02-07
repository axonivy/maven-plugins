package ch.ivyteam.ivy.changelog.generator;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJsonProvider;
import org.junit.Test;

import com.fasterxml.jackson.databind.DeserializationFeature;

import ch.ivyteam.ivy.changelog.generator.jira.JiraResponse;
import ch.ivyteam.ivy.changelog.generator.jira.Paging;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;

public class TestJiraJson {

  @Test
  public void testImprovement() throws IOException {
    Path sampleJson = new File("src/test/resources/samples/searchImprovements93.json").toPath();
    try(InputStream json = Files.newInputStream(sampleJson, StandardOpenOption.READ)) {
      JiraResponse response = deserialize(json);
      assertThat(response.issues).hasSize(2);
    }
  }

  @Test
  public void testPaging() throws IOException {
    Path sampleJson = new File("src/test/resources/samples/page1.json").toPath();
    try(InputStream json = Files.newInputStream(sampleJson, StandardOpenOption.READ)) {
      JiraResponse response = deserialize(json);
      Paging paging = response.page();
      assertThat(paging.startAt).isEqualTo(0);
      assertThat(paging.maxResults).isEqualTo(100);
      assertThat(paging.total).isEqualTo(150);

      assertThat(paging.hasNext()).isTrue();
      Paging next = paging.next();
      assertThat(next.startAt).isEqualTo(100);
    }
  }

  private static JiraResponse deserialize(InputStream json) throws IOException {
    JacksonJsonProvider provider = new JacksonJaxbJsonProvider()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return (JiraResponse) provider.readFrom(Object.class, JiraResponse.class, new Annotation[0]
            , MediaType.APPLICATION_JSON_TYPE, new MultivaluedHashMap<String, String>(), json);
  }
}
