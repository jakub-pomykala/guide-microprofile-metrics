// tag::copyright[]
/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - Initial implementation
 *******************************************************************************/
// end::copyright[]
// tag::MetricsTest[]
package it.io.openliberty.guides.metrics;

import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.util.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.cxf.jaxrs.provider.jsrjsonp.JsrJsonpProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

// tag::TestMethodOrder[]
@TestMethodOrder(OrderAnnotation.class)
// end::TestMethodOrder[]
public class MetricsIT {
  private static String httpPort;
  private static String httpsPort;
  private static String baseHttpUrl;
  private static String baseHttpsUrl;

  private List<String> metrics;
  private Client client;

  private final String INVENTORY_HOSTS = "inventory/systems";
  private final String INVENTORY_HOSTNAME = "inventory/systems/localhost";
  private final String METRICS_APPLICATION = "metrics/application";

  // tag::BeforeAll[]
  @BeforeAll
  // end::BeforeAll[]
  // tag::oneTimeSetup[]
  public static void oneTimeSetup() {
    httpPort = System.getProperty("http.port");
    httpsPort = System.getProperty("https.port");
    baseHttpUrl = "http://localhost:" + httpPort + "/";
    baseHttpsUrl = "https://localhost:" + httpsPort + "/";
  }
  // end::oneTimeSetup[]

  // tag::BeforeEach[]
  @BeforeEach
  // end::BeforeEach[]
  // tag::setup[]
  public void setup() {
    client = ClientBuilder.newClient();
    // tag::JsrJsonpProvider[]
    client.register(JsrJsonpProvider.class);
    // end::JsrJsonpProvider[]
  }
  // end::setup[]

  // tag::AfterEach[]
  @AfterEach
  // end::AfterEach[]
  // tag::teardown[]
  public void teardown() {
    client.close();
  }
  // end::teardown[]

  // tag::Test1[]
  @Test
  // end::Test1[]
  // tag::Order1[]
  @Order(1)
  // end::Order1[]
  // tag::testPropertiesRequestTimeMetric[]
  public void testPropertiesRequestTimeMetric() {
    connectToEndpoint(baseHttpUrl + INVENTORY_HOSTNAME);
    metrics = getMetrics();
    for (String metric : metrics) {
      if (metric.startsWith(
          "application_inventoryProcessingTime_rate_per_second")) {
        float seconds = Float.parseFloat(metric.split(" ")[1]);
        assertTrue(4 > seconds);
      }
    }
  }
  // end::testPropertiesRequestTimeMetric[]

  // tag::Test2[]
  @Test
  // end::Test2[]
  // tag::Order2[]
  @Order(2)
  // end::Order2[]
  // tag::testInventoryAccessCountMetric[]
  public void testInventoryAccessCountMetric() {
    metrics = getMetrics();
    Map<String, Integer> accessCountsBefore = getIntMetrics(metrics,
            "application_inventoryAccessCount_total");
    connectToEndpoint(baseHttpUrl + INVENTORY_HOSTS);
    metrics = getMetrics();
    Map<String, Integer> accessCountsAfter = getIntMetrics(metrics,
            "application_inventoryAccessCount_total");
    for (String key : accessCountsBefore.keySet()) {
      Integer accessCountBefore = accessCountsBefore.get(key);
      Integer accessCountAfter = accessCountsAfter.get(key);
      assertTrue(accessCountAfter > accessCountBefore);
    }
  }
  // end::testInventoryAccessCountMetric[]

  // tag::Test3[]
  @Test
  // end::Test3[]
  // tag::Order3[]
  @Order(3)
  // end::Order3[]
  // tag::testInventorySizeGaugeMetric[]
  public void testInventorySizeGaugeMetric() {
    metrics = getMetrics();
    Map<String, Integer> inventorySizeGauges = getIntMetrics(metrics, 
            "application_inventorySizeGauge");
    for (Integer value : inventorySizeGauges.values()) {
      assertTrue(1 <= value);
    }
  }
  // end::testInventorySizeGaugeMetric[]

  // tag::Test4[]
  @Test
  // end::Test4[]
  // tag::Order4[]
  @Order(4)
  // end::Order4[]
  // tag::testPropertiesAddSimplyTimeMetric[]
  public void testPropertiesAddSimplyTimeMetric() {
    connectToEndpoint(baseHttpUrl + INVENTORY_HOSTNAME);
    metrics = getMetrics();
    boolean checkMetric = false;
    for (String metric : metrics) {
      if (metric.startsWith(
          "application_inventoryAddingTime_total")) {
            checkMetric = true;
      }
    }
    assertTrue(checkMetric);
  }
  // end::testPropertiesAddSimplyTimeMetric[]

  public void connectToEndpoint(String url) {
    Response response = this.getResponse(url);
    this.assertResponse(url, response);
    response.close();
  }

  private List<String> getMetrics() {
    String usernameAndPassword = "admin" + ":" + "adminpwd";
    String authorizationHeaderValue = "Basic "
        + java.util.Base64.getEncoder()
                          .encodeToString(usernameAndPassword.getBytes());
    Response metricsResponse = client.target(baseHttpsUrl + METRICS_APPLICATION)
                                     .request(MediaType.TEXT_PLAIN)
                                     .header("Authorization",
                                         authorizationHeaderValue)
                                     .get();

    BufferedReader br = new BufferedReader(new InputStreamReader((InputStream) 
    metricsResponse.getEntity()));
    List<String> result = new ArrayList<String>();
    try {
      String input;
      while ((input = br.readLine()) != null) {
        result.add(input);
      }
      br.close();
    } catch (IOException e) {
      e.printStackTrace();
      fail();
    }

    metricsResponse.close();
    return result;
  }

  private Response getResponse(String url) {
    return client.target(url).request().get();
  }

  private void assertResponse(String url, Response response) {
    assertEquals(200, response.getStatus(), "Incorrect response code from " + url);
  }
  
  private Map<String, Integer> getIntMetrics(List<String> metrics, String metricName) {
    Map<String, Integer> output = new HashMap<String, Integer>();
    for (String metric : metrics) {
      if (metric.startsWith(metricName)) {
        String[] mSplit = metric.split(" ");
        String key = mSplit[0];
        Integer value = Integer.parseInt(mSplit[mSplit.length - 1]);
        output.put(key, value);
      }
    }
    return output;
  }
}
// end::MetricsTest[]
