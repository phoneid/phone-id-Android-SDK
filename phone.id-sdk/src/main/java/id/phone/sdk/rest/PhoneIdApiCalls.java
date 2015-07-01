package id.phone.sdk.rest;

import id.phone.sdk.rest.response.ClientResponse;
import id.phone.sdk.rest.response.DefaultResponse;
import id.phone.sdk.rest.response.ContactsRefreshNeededResponse;
import id.phone.sdk.rest.response.TokenResponse;
import id.phone.sdk.rest.response.UploadContactsResponse;
import id.phone.sdk.rest.response.UserResponse;
import retrofit.Callback;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Query;
import retrofit.http.Path;

/**
 * Created by azubchenko on 4/8/15.
 */
public interface PhoneIdApiCalls {

    @GET("/v2/auth/users/sendcode")
    void getCode(
        @Query("client_id") String client_id
        , @Query("number") String number
        , Callback<DefaultResponse> callback);

    @FormUrlEncoded
    @POST("/v2/auth/users/token")
    void getToken(
        @Field("client_id") String client_id
        , @Field("grant_type") String grant_type
        , @Field("code") String code
        , Callback<TokenResponse> callback);

    @FormUrlEncoded
    @POST("/v2/auth/users/token")
    void getRefreshToken(
        @Field("client_id") String client_id
        , @Field("grant_type") String grant_type
        , @Field("refresh_token") String refresh_token
        , Callback<TokenResponse> callback);

    @GET("/v2/auth/users/me")
    void getUser(@Query("access_token") String access_token, Callback<UserResponse> callback);

    @GET("/v2/clients/{client_id}")
    void getClients(@Path("client_id") String client_id, Callback<ClientResponse> callback);

    @GET("/v2/auth/contacts/refresh")
    void getContactsRefreshNeeded(
        @Query("access_token") String access_token
        , @Query("checksum") String hash
        , Callback<ContactsRefreshNeededResponse> callback);

	@FormUrlEncoded
	@POST("/v2/auth/contacts")
	void uploadContacts(
		@Query("access_token") String access_token
		, @Field("contacts") String contactsJSON
		, Callback<UploadContactsResponse> callback);

}
