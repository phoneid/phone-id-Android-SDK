package id.phone.demo.ui;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;

import id.phone.demo.R;

import static id.phone.sdk.utils.LogUtils.LOGD;

public class InfoDialog extends DialogFragment
{
    public static final String TAG = InfoDialog.class.getSimpleName();
	public static final String MESSAGE = "message";
    public static final String TITLE = "title";

	private DialogInterface.OnClickListener mOnClickListener;

	public static InfoDialog newInstance(int title, Exception ex)
	{
		InfoDialog frag = new InfoDialog();
		Bundle args = new Bundle();
		args.putString(MESSAGE,  TextUtils.isEmpty(ex.getLocalizedMessage()) ?
                ex.toString() : ex.getLocalizedMessage());
		if (title > 0)
			args.putInt(TITLE, title);
		frag.setArguments(args);
		return frag;
	}

    public static InfoDialog newInstance(int title, String message)
    {
        InfoDialog frag = new InfoDialog();
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
			.setIcon(android.R.drawable.ic_dialog_info)
			.setTitle(getArguments().containsKey(TITLE) ? getArguments().getInt(TITLE) :
				R.string.Info)
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

	public InfoDialog setClickListener(DialogInterface.OnClickListener mListener) {
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
