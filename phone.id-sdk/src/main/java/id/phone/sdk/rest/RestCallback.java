package id.phone.sdk.rest;

import id.phone.sdk.rest.response.ErrorResponse;
import retrofit.Callback;
import retrofit.RetrofitError;

/**
 * Created by azubchenko on 4/19/15.
 */
public abstract class RestCallback<T> implements Callback<T> {
    public abstract void failure(ErrorResponse errorResponse, RetrofitError error);

    @Override
    public void failure(RetrofitError error) {
        ErrorResponse errorResponse = (ErrorResponse) error.getBodyAs(ErrorResponse.class);

        if (errorResponse != null) {
            failure(errorResponse, error);
        } else {
            failure(new ErrorResponse(error.getMessage()), error);
        }
    }
}
