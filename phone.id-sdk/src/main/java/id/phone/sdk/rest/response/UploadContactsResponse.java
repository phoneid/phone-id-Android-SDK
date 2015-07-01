package id.phone.sdk.rest.response;

/**
 * Created by Dennis on 6/5/15.
 */
public class UploadContactsResponse
{
    public int getReceived()
    {
        return received;
    }

    private int received;

    @Override
    public String toString()
    {
        return "UploadContactsResponse{" +
            "received=" + received +
            '}';
    }
}
