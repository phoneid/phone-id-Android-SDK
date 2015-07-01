package id.phone.sdk.ui.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;

import id.phone.sdk.R;
import id.phone.sdk.utils.LogUtils;

import static id.phone.sdk.utils.LogUtils.LOGD;

public class ErrorDialog extends DialogFragment
{
    public static final String TAG = LogUtils.makeLogTag(ErrorDialog.class);
	public static final String MESSAGE = "message";
    public static final String TITLE = "title";

	private DialogInterface.OnClickListener mOnClickListener;

	public static ErrorDialog newInstance(int title, Exception ex)
	{
		ErrorDialog frag = new ErrorDialog();
		Bundle args = new Bundle();
		args.putString(MESSAGE,  TextUtils.isEmpty(ex.getLocalizedMessage()) ?
                ex.toString() : ex.getLocalizedMessage());
		if (title > 0)
			args.putInt(TITLE, title);
		frag.setArguments(args);
		return frag;
	}

    public static ErrorDialog newInstance(int title, String message)
    {
        ErrorDialog frag = new ErrorDialog();
        Bundle args = new Bundle();
		args.putString(MESSAGE, message);
		if (title > 0)
        	args.putInt(TITLE, title);
        frag.setArguments(args);
        return frag;
    }

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		return new AlertDialog.Builder(getActivity())
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle(getArguments().containsKey(TITLE) ? getArguments().getInt(TITLE) :
				R.string.phid_error)
			.setMessage(getArguments().getString(MESSAGE))
			.setPositiveButton(android.R.string.ok,
				new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int whichButton)
					{
						doPositiveClick();
						dismiss();
					}
				}
			)
			.create();
	}

	public void doPositiveClick()
	{
		if (mOnClickListener != null)
		{
			mOnClickListener.onClick(getDialog(), DialogInterface.BUTTON_POSITIVE);
		}
	}

	public ErrorDialog setClickListener(DialogInterface.OnClickListener mListener) {
		this.mOnClickListener = mListener;
		return this;
	}

    @Override
    public void show(FragmentManager manager, String tag)
    {
		try
		{
			Fragment frag;
			if ((frag = manager.findFragmentByTag(tag)) != null)
			{
				manager.beginTransaction().remove(frag).commit();
			}
			super.show(manager, tag);
		}
		catch (Exception ex)
		{
			LOGD(TAG, "show", ex);
		}
    }

}
