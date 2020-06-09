package no.unit.nva.doi.utils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import no.bibsys.aws.tools.IoUtils;
import no.unit.nva.doi.CrossRefClient;

public abstract class AbstractLambdaTest {

    public static final String DOI_STRING = "10.1007/s00115-004-1822-4";
    public static final String DOI_DX_URL_PREFIX = "https://dx.doi.org";
    public static final String DOI_URL_PREFIX = "https://doi.org";

    public static final Path CrossRefSamplePath = Paths.get("crossRefSample.json");
    public static final String ERROR_MESSAGE = "404 error message";

    protected final Context mockLambdaContext = createMockContext();

    protected static Context createMockContext() {
        Context context = mock(Context.class);
        when(context.getLogger()).thenReturn(mockLambdaLogger());
        return context;
    }

    protected static LambdaLogger mockLambdaLogger() {
        return new LambdaLogger() {
            @Override
            public void log(String message) {
                System.out.print(message);
            }

            @Override
            public void log(byte[] message) {
                log(Arrays.toString(message));
            }
        };
    }

    protected HttpClient mockHttpClientWithNonEmptyResponse() throws IOException {
        String responseBody = IoUtils.resourceAsString(CrossRefSamplePath);
        HttpResponseStatus200<String> response = new HttpResponseStatus200<>(responseBody);
        return new MockHttpClient<>(response);
    }

    protected CrossRefClient crossRefClientReceives404() {
        HttpResponseStatus404<String> errorResponse = new HttpResponseStatus404<>(
            ERROR_MESSAGE);
        MockHttpClient<String> mockHttpClient = new MockHttpClient<>(errorResponse);
        return new CrossRefClient(mockHttpClient);
    }

    protected CrossRefClient crossRefClientReceives500() {
        HttpResponseStatus500<String> errorResponse = new HttpResponseStatus500<>(
            ERROR_MESSAGE);
        MockHttpClient<String> mockHttpClient = new MockHttpClient<>(errorResponse);
        return new CrossRefClient(mockHttpClient);
    }
}
