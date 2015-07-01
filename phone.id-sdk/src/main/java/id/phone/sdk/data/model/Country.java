package id.phone.sdk.data.model;

import android.content.ContentValues;

import java.util.Hashtable;

import id.phone.sdk.data.DatabaseContract;

/**
 * Created by azubchenko on 4/6/15.
 * Entity that represents country
 */
public class Country {
    private long tel;
    private String name;
    private String iso;

    /**
     * Method that transforms hashtable with fields values to the entity
     *
     * @param hashtable
     * @return
     * @throws NumberFormatException
     * @throws IllegalArgumentException
     */
    static public Country newInstance(Hashtable<String, String> hashtable) throws IllegalArgumentException {
        Country country = new Country();

        country.setTel(Long.valueOf(hashtable.get(DatabaseContract.CountryEntry._ID)));
        country.setName(hashtable.get(DatabaseContract.CountryEntry.COLUMN_NAME));
        country.setIso(hashtable.get(DatabaseContract.CountryEntry.COLUMN_ISO));

        return country;
    }

    /**
     * Yet another method for getting new instance
     *
     * @param id
     * @param name
     * @param iso
     * @return
     */
    static public Country newInstance(long id, String name, String iso) {
        Country country = new Country();

        country.setTel(id);
        country.setName(name);
        country.setIso(iso);

        return country;
    }


    public long getTel() {
        return tel;
    }

    public void setTel(long tel) {
        this.tel = tel;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setIso(String iso) { this.iso = iso; }

    public String getIso() { return iso; }

    /**
     * Helper method for communicating with DataBase
     *
     * @return ContentValues, that is representing country object
     */
    public ContentValues getContentValues() {
        ContentValues result = new ContentValues();

        result.put(DatabaseContract.CountryEntry._ID, this.getTel());
        result.put(DatabaseContract.CountryEntry.COLUMN_NAME, this.getName());
        result.put(DatabaseContract.CountryEntry.COLUMN_ISO, this.getIso());

        return result;
    }

}
