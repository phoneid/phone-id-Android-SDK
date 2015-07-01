package id.phone.sdk.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

/**
 * Created by azubchenko on 4/18/15.
 */
public class BaseListFragment extends ListFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //
        if (savedInstanceState != null) {
            restoreState(savedInstanceState);
        } else if (getArguments() != null) {
            populateArguments(getArguments());
        }

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        saveState(outState);
    }

    /**
     * @param outState
     */
    protected void saveState(Bundle outState) {};

    /**
     * @param savedInstanceState
     */
    protected void restoreState(Bundle savedInstanceState) {};

    /**
     * @param savedInstanceState
     */
    protected void populateArguments(Bundle savedInstanceState) {};


    /**
     * Helper method to hide keyboard if shown
     *
     */
    protected void hideSoftwareKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getActivity().getWindow().getCurrentFocus().getWindowToken(), 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper method to hide keyboard if shown
     *
     */
    protected void showSoftwareKeyboard(EditText control)
    {
        try
        {
            InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(
                Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(control, InputMethodManager.SHOW_IMPLICIT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}