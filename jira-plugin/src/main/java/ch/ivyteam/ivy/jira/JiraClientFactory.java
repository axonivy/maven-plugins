package ch.ivyteam.ivy.jira;

import org.apache.maven.settings.Server;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJsonProvider;

import com.fasterxml.jackson.databind.DeserializationFeature;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;

public class JiraClientFactory {

  public static Client createClient(Server server) {
    final JacksonJsonProvider jacksonJsonProvider = new JacksonJaxbJsonProvider()
     .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    Client client = ClientBuilder.newClient(new ClientConfig(jacksonJsonProvider));
    if (server != null) {
      client.register(HttpAuthenticationFeature.basic(server.getUsername(), server.getPassword()));
    }
    return client;
  }
}
