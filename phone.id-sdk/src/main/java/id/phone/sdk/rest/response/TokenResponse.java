package id.phone.sdk.rest.response;

import java.text.DateFormat;
import java.util.Date;

/**
 * Access token information
 * @author azubchenko on 4/17/15.
 */
public class TokenResponse
{
    public static final String TOKEN_TYPE_ACCESS_MANAGER = "TOKEN_TYPE_ACCESS_MANAGER";

    private String token_type;
    private String access_token;
    private long expires_in;
    private String refresh_token;

    private long create_time;

	/**
	 * Get Token type
	 * @return Token type
	 */
    public String getTokenType() { return token_type; }

	/**
	 * Get Access token
	 * @return Access token
	 */
    public String getAccessToken() {
        return access_token;
    }

	/**
	 * Get Refresh token
	 * @return Refresh token
	 */
    public String getRefreshToken() {
        return refresh_token;
    }

    public long getCreate_time()
    {
        return create_time;
    }

    public void setCreate_time(long create_time)
    {
        this.create_time = create_time;
    }

    public long getExpires_in()
    {
        return expires_in;
    }

    public TokenResponse(String token_type, String access_token, String refresh_token)
    {
        this.token_type = token_type;
        this.access_token = access_token;
        this.refresh_token = refresh_token;
    }

	/**
	 * Check of token is already expired
	 * @return true if token already expired
	 */
    public boolean isExpired()
    {
        return (System.currentTimeMillis() - create_time) >= (expires_in * 1000);
    }

	/**
	 * Check of token will expire soon
	 * @return true if token will expire soon
	 */
    public boolean isExpireSoon()
    {
        return (System.currentTimeMillis() - create_time) >= ((expires_in * 1000) - (expires_in / 4 * 1000));
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Token type: ").append(token_type);
        sb.append('\n').append("Access Token: ").append(access_token);
        sb.append('\n').append("Refresh Token: ").append(refresh_token);
        if (isExpired())
            sb.append('\n').append("TOKEN EXPIRED");
        else
            sb.append('\n').append("Expires: ").append(DateFormat.getDateTimeInstance()
                .format(new Date(create_time + expires_in * 1000)));

        return sb.toString();
    }
}
