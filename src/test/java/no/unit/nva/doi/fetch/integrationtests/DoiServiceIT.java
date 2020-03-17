package no.unit.nva.doi.fetch.integrationtests;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import org.junit.jupiter.api.Test;

public class DoiServiceIT {

    @Test
    public void connectionToCrossRefShouldReturnOK() {
        Client client = ClientBuilder.newClient();
    }
}
