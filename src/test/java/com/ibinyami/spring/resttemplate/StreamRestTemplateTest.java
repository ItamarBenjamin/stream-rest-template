package com.ibinyami.spring.resttemplate;

import com.google.common.io.CharStreams;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.MockServerRule;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class StreamRestTemplateTest {
    private static final String RESPONSE = "world";
    private static final String ENDPOINT = "/hello";

    @Rule
    public MockServerRule mockServerRule = new MockServerRule(this);

    private StreamRestTemplate target = new StreamRestTemplate();

    private String url = null;
    private MockServerClient mockServerClient;

    @Before
    public void setUp() {
        url = "http://localhost:" + mockServerRule.getPort() + ENDPOINT;

        mockServerClient
                .when(request().withMethod("GET").withPath(ENDPOINT))
                .respond(response().withStatusCode(200).withBody(RESPONSE));

        mockServerClient
                .when(request().withMethod("POST").withPath(ENDPOINT))
                .respond(response().withStatusCode(200).withBody(RESPONSE));
    }

    @Test
    public void getForEntity() {
        ResponseEntity<String> asString = target.getForEntity(url, String.class);
        Assert.assertEquals(asString.getBody(), RESPONSE);
    }

    @Test
    public void getForEntityStream() throws IOException {
        ResponseEntity<InputStreamResource> asResource = target.getForEntity(url, InputStreamResource.class);
        String resourceText = streamToString(asResource.getBody());
        Assert.assertEquals(resourceText, RESPONSE);
    }

    @Test
    public void postForEntity() {
        ResponseEntity<String> asString = target.postForEntity(url, null, String.class);
        Assert.assertEquals(asString.getBody(), RESPONSE);
    }

    @Test
    public void postForEntityStream() throws IOException {
        ResponseEntity<InputStreamResource> asResource = target.postForEntity(url, null, InputStreamResource.class);
        String resourceText = streamToString(asResource.getBody());
        Assert.assertEquals(resourceText, RESPONSE);
    }

    @Test
    public void exchange() {
        ResponseEntity<String> asStringGet = target.exchange(url, HttpMethod.GET, null, String.class);
        Assert.assertEquals(asStringGet .getBody(), RESPONSE);
        ResponseEntity<String> asStringPost = target.exchange(url, HttpMethod.POST, null, String.class);
        Assert.assertEquals(asStringPost.getBody(), RESPONSE);
    }

    @Test
    public void exchangeStream() throws IOException {
        ResponseEntity<InputStreamResource> asResourceGet = target.exchange(url, HttpMethod.GET, null, InputStreamResource.class);
        Assert.assertEquals(streamToString(asResourceGet.getBody()), RESPONSE);
        ResponseEntity<InputStreamResource> asResourcePost = target.exchange(url, HttpMethod.POST, null, InputStreamResource.class);
        Assert.assertEquals(streamToString(asResourcePost.getBody()), RESPONSE);
    }

    private String streamToString(InputStreamResource asResource) throws IOException {
        try (final Reader reader = new InputStreamReader(asResource.getInputStream())) {
              return CharStreams.toString(reader);
        }
    }
}