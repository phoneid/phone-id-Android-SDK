package id.phone.sdk.ui.fragment;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;

import id.phone.sdk.Commons;
import id.phone.sdk.R;
import id.phone.sdk.data.DatabaseContract;
import id.phone.sdk.data.DatabaseHelper;
import id.phone.sdk.data.model.Country;

/**
 * Created by azubchenko on 4/8/15.
 */
public class CountryListFragment extends BaseListFragment implements LoaderManager.LoaderCallbacks<Cursor>
{

    private static final String[] COUNTRIES_PROJECTION = new String[]{DatabaseContract.CountryEntry._ID, "('+' || " + DatabaseContract.CountryEntry._ID + ") mcc", DatabaseContract.CountryEntry.COLUMN_ISO, DatabaseContract.CountryEntry.COLUMN_NAME};
    private EditText searchStr;
    private SimpleCursorAdapter mAdapter;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mAdapter = new SimpleCursorAdapter(getActivity(), R.layout.phid_item_country, null, new String[]{"mcc", DatabaseContract.CountryEntry.COLUMN_NAME}, new int[]{android.R.id.text1, android.R.id.text2}, android.support.v4.widget.CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        setListAdapter(mAdapter);
        getListView().setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = (Cursor) getListAdapter().getItem(position);

                //sending local broadcast to nearby fragment
                LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getActivity());
                Intent intent = new Intent();
                intent.setAction(Commons.COUNTRY_PICKED_ACTION);
                intent.putExtra(Intent.EXTRA_TEXT, Country.newInstance(DatabaseHelper.getHashtable(cursor)).getIso());
                localBroadcastManager.sendBroadcast(intent);
                //closing fragment
                getActivity().onBackPressed();
            }
        });

        getLoaderManager().initLoader(Commons.COUNTRIES_LOADER, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.phid_fragment_countries, null);

        searchStr = (EditText) view.findViewById(R.id.search);
        searchStr.requestFocus();
        searchStr.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                getLoaderManager().restartLoader(Commons.COUNTRIES_LOADER, null, CountryListFragment.this);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        return view;

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (Commons.COUNTRIES_LOADER == id) {
            String searchStr = ((EditText) getView().findViewById(R.id.search)).getText().toString();
            String select = "((" + DatabaseContract.CountryEntry.COLUMN_NAME + " NOTNULL) AND (" + DatabaseContract.CountryEntry.COLUMN_NAME + " != '' )" +
                    "AND (" + DatabaseContract.CountryEntry.COLUMN_NAME + " LIKE ?  OR " + DatabaseContract.CountryEntry._ID + " LIKE ? OR " + DatabaseContract.CountryEntry.COLUMN_NAME + " LIKE ?) )";
            return new CursorLoader(getActivity(), DatabaseContract.CountryEntry.CONTENT_URI, COUNTRIES_PROJECTION, select, new String[]{"%" + searchStr + "%", "%" + searchStr + "%", "%" + (searchStr.length() > 0 ? Character.toUpperCase(searchStr.charAt(0)) + searchStr.substring(1) : searchStr) + "%"}, DatabaseContract.CountryEntry.COLUMN_NAME + " COLLATE LOCALIZED ASC");
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    // private methods


    @Override
    public void onResume()
    {
        super.onResume();
        showSoftwareKeyboard(searchStr);
    }
}
