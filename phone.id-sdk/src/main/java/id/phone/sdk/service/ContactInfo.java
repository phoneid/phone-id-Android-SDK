package id.phone.sdk.service;

/**
 * Created by dennis on 02.06.15.
 */
public class ContactInfo
{
	private String first_name;
	private String last_name;
	private String number;
	private String kind;
	private String company;

	public ContactInfo(String first_name, String number, String kind)
	{
		this.first_name = first_name;
		this.number = number;
		this.kind = kind;
	}

	public String getFirstName()
	{
		return first_name;
	}

	public void setFirstName(String first_name)
	{
		this.first_name = first_name;
	}

	public String getLastName()
	{
		return last_name;
	}

	public void setLastName(String last_name)
	{
		this.last_name = last_name;
	}

	public String getNumber()
	{
		return number;
	}

	public void setNumber(String number)
	{
		this.number = number;
	}

	public String getKind()
	{
		return kind;
	}

	public void setKind(String kind)
	{
		this.kind = kind;
	}

	public String getCompany()
	{
		return company;
	}

	public void setCompany(String company)
	{
		this.company = company;
	}

}
