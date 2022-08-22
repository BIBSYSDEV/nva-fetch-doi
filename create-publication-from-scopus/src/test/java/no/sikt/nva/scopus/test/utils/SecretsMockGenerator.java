package no.sikt.nva.scopus.test.utils;

import static no.sikt.nva.scopus.conversion.PiaConnection.PIA_PASSWORD_KEY_NAME;
import static no.sikt.nva.scopus.conversion.PiaConnection.PIA_SECRETS_NAME;
import static no.sikt.nva.scopus.conversion.PiaConnection.PIA_USERNAME_KEY_NAME;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Map;
import nva.commons.secrets.SecretsReader;
import org.mockito.invocation.InvocationOnMock;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

public class SecretsMockGenerator {
    public static final String ERROR_MESSAGE_FROM_AWS_SECRET_MANAGER = "Secret not found";

    public static SecretsReader createSecretsReaderMock() {
        var secretsManager = mock(SecretsManagerClient.class);
        when(secretsManager.getSecretValue(any(GetSecretValueRequest.class)))
            .thenAnswer(SecretsMockGenerator::provideGetSecretValueResult);
        return new SecretsReader(secretsManager);
    }

    private static GetSecretValueResponse provideGetSecretValueResult(InvocationOnMock invocation)
        throws JsonProcessingException {
        String providedSecretName = getSecretNameFromRequest(invocation);
        if (providedSecretName.equals(PIA_SECRETS_NAME)) {
            String secretString = createSecretJsonObject();
            return createGetSecretValueResult(secretString);
        } else {
            throw new RuntimeException(ERROR_MESSAGE_FROM_AWS_SECRET_MANAGER);
        }
    }

    private static GetSecretValueResponse createGetSecretValueResult(String secretString) {
        return GetSecretValueResponse.builder()
                   .secretString(secretString)
                   .name(PIA_SECRETS_NAME)
                   .build();
    }

    private static String createSecretJsonObject() throws JsonProcessingException {
        Map<String, String> secret = Map.of(PIA_PASSWORD_KEY_NAME, randomString(), PIA_USERNAME_KEY_NAME,
                                            randomString());
        return dtoObjectMapper.writeValueAsString(secret);
    }

    private static String getSecretNameFromRequest(InvocationOnMock invocation) {
        GetSecretValueRequest request = invocation.getArgument(0);
        return request.secretId();
    }

}
