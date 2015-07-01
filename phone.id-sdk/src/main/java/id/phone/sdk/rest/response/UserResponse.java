package id.phone.sdk.rest.response;

import java.text.DateFormat;
import java.util.Date;

/**
 * Phone.id User information storage
 * @author azubchenko on 4/17/15.
 */
public class UserResponse
{
    private String id;
    private String client_id;
    private String phone_number;

	/**
	 * Get user ID on Phone.id server
	 * @return user ID
	 */
    public String getId() {
        return id;
    }

	/**
	 * Get client ID user related to
	 * @return client ID
	 */
    public String getClientId() { return client_id; }

	/**
	 * Get user phone number
	 * @return phone number
	 */
    public String getPhoneNumber() { return phone_number; }

    public UserResponse(String id, String client_id, String phone_number)
    {
        this.id = id;
        this.client_id = client_id;
        this.phone_number = phone_number;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("ID: ").append(id);
        sb.append('\n').append("Client Id: ").append(client_id);
        sb.append('\n').append("Phone number: ").append(phone_number);
        return sb.toString();
    }

}
