package id.phone.sdk.rest;

import com.squareup.okhttp.OkHttpClient;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import id.phone.sdk.Commons;
import retrofit.RestAdapter;
import retrofit.client.OkClient;

/**
 * Created by azubchenko on 4/8/15.
 */
public class PhoneIdRestClient {
    static private final String ROOT_SERVER = "https://api.phone.id";

    static {
        setupRestClient();
    }

    private static PhoneIdApiCalls REST_CLIENT;

    private PhoneIdRestClient() {
    }

    private static void setupRestClient() {

        Executor executorHttp = Executors.newSingleThreadExecutor();
        Executor executorCallback = Executors.newSingleThreadExecutor();

        RestAdapter.Builder builder = new RestAdapter.Builder().setEndpoint(ROOT_SERVER).
                setClient(setupOkClient()).setExecutors(executorHttp, executorCallback).
                setLogLevel(RestAdapter.LogLevel.BASIC);

        RestAdapter restAdapter = builder.build();
        REST_CLIENT = restAdapter.create(PhoneIdApiCalls.class);
    }

    /**
     * Setup OkClient with timeout
     *
     * @return
     */
    private static OkClient setupOkClient() {
        OkHttpClient client = new OkHttpClient();
        client.setConnectTimeout(Commons.DEFAULT_CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS);
        client.setReadTimeout(Commons.DEFAULT_READ_TIMEOUT, TimeUnit.MILLISECONDS);

        return new OkClient(client);
    }

    public static PhoneIdApiCalls get() {
        return REST_CLIENT;
    }
}
