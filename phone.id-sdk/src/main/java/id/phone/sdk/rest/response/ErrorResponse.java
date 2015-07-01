package id.phone.sdk.rest.response;

/**
 * Error response container
 * @author azubchenko on 4/19/15.
 */
public class ErrorResponse
{
	/**
	 * Get error code
	 * @return error code
	 */
    public String getCode() { return code; }

	/**
	 * Get error message
	 * @return error message
	 */
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    private String code;
    private String message;

    public ErrorResponse(String message) {
        this.message = message;
    }
}
