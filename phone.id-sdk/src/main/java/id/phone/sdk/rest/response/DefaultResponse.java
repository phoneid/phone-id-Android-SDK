package id.phone.sdk.rest.response;

/**
 * Created by azubchenko on 4/17/15.
 */
public class DefaultResponse
{
    public int getResult()
    {
        return result;
    }

    public String getMessage()
    {
        return message;
    }

    private int result;
    private String message;

    @Override
    public String toString()
    {
        return "DefaultResponse{" +
            "result=" + result +
            ", message='" + message + '\'' +
            '}';
    }
}
