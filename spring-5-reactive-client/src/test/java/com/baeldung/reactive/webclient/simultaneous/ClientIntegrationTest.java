package com.baeldung.reactive.webclient.simultaneous;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import com.github.tomakehurst.wiremock.WireMockServer;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ClientIntegrationTest {

    private WireMockServer wireMockServer;

    @Before
    public void setup() {
        wireMockServer = new WireMockServer(wireMockConfig().port(8089));
        wireMockServer.start();
        configureFor("localhost", wireMockServer.port());
    }

    @After
    public void tearDown() {
        wireMockServer.stop();
    }

    @Test
    public void givenClient_whenFetchingUsers_thenExecutionTimeIsLessThanDouble() {
        // Arrange
        int requestsNumber = 5;
        int singleRequestTime = 1000;

        for (int i = 1; i <= requestsNumber; i++) {
            stubFor(get(urlEqualTo("/user/" + i)).willReturn(aResponse().withFixedDelay(singleRequestTime)
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(String.format("{ \"id\": %d }", i))));
        }

        List<Integer> userIds = IntStream.rangeClosed(1, requestsNumber)
            .boxed()
            .collect(Collectors.toList());

        Client client = new Client("http://localhost:8089");

        // Act
        long start = System.currentTimeMillis();
        List<User> users = client.fetchUsers(userIds);
        long end = System.currentTimeMillis();

        // Assert
        long totalExecutionTime = end - start;

        assertEquals("Unexpected number of users", requestsNumber, users.size());
        assertTrue("Execution time is too big", 2 * singleRequestTime > totalExecutionTime);
    }
}
