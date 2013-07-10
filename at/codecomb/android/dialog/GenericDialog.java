package at.ac.uibk.dialog;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

/**
 * When creating dialogs with a DialogBuilder most of the time the same source code is used and not many things differ.
 * That is the goal of GenericDialog, creating dialogs fast and easy. On the other hand GenericDialogs do have one more
 * point to them which make them more advanced compared to standart DialogFragments, they are listenable by implementing
 * DialogListener.
 * 
 * @author David Riedl (Code-Comb)
 * @version 1.2
 */
public abstract class GenericDialog extends DialogFragment {
	/* the object listening to the dialog */
	private DialogListener mListener;
	/* custom tag used to identify the dialog */
	private String mTAG = "";
	/* the dialog's view */
	private View mView;

	/* used to delegate object between activities, adapters etc if needed */
	private Object mObject;

	/* if a return value is needed and an object is listening to the dialog */
	private boolean mListenable = false;
	/* Dialog Title */
	private String mTitle = "";
	/* text of the positive Button */
	private String mPositiveButtonText = "";
	/* text of the negative Button */
	private String mNegativeButtonText = "";

	public GenericDialog() {
		/*
		 * needed or else the instance is not saved and the application crashes on orientation change
		 */
		setRetainInstance(true);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		/* create a simple DialogBuilder */
		Builder builder = new Builder(getActivity());
		/* generate the View by using the abstract method generateView */
		mView = generateView(getActivity().getLayoutInflater());
		builder.setView(mView);

		if (!mTitle.equals("")) {
			builder.setTitle(mTitle);
		}

		/* if no button text were set, use a standart button */
		setStandardButtonText();
		/* if an object is listening use the DialogListener-methods */
		if (mListenable) {
			builder.setPositiveButton(mPositiveButtonText, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					mListener.onDialogPositiveClick(GenericDialog.this);
				}
			});

			builder.setNegativeButton(mNegativeButtonText, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					mListener.onDialogNegativeClick(GenericDialog.this);
				}
			});
		} else {
			/* if no object is listening use the Button to close the dialog */
			builder.setPositiveButton(mPositiveButtonText, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
		}
		return builder.create();
	}

	/* if forgotten, just set standard texts */
	private void setStandardButtonText() {
		if (mPositiveButtonText.equals("")) {
			mPositiveButtonText = "Ok";
		}
		if (mListenable && mNegativeButtonText.equals("")) {
			mNegativeButtonText = "Cancel";
		}
	}

	/* ------------------ getter / setter ------------------ */

	public View getDialogView() {
		return mView;
	}

	/**
	 * If the dialog is "listenable" the dialog will have two buttons, a negative and positive one. If set to true the
	 * activity calling the dialog needs to implement the interface DialogListener. Once one of the buttons is clicked
	 * it will be delegated to the listening Activity calling onPositive or onNegativeButtonClick
	 * 
	 * @param listenable
	 *            listenable if true, not-listenable otherwise
	 */
	public void setListenable(final boolean listenable) {
		mListenable = listenable;
	}

	/**
	 * gives the dialog a tag, which can be used to identify it in onButtonClicked()
	 * 
	 * @param tag
	 *            the tag to identify the dialog
	 */
	public void setDialogTag(final String tag) {
		mTAG = tag;
	}

	/**
	 * the tag to identify the dialog, previously set with setDialogTag()
	 */
	public String getDialogTag() {
		return mTAG;
	}

	/**
	 * the title of the dialog, shown on the top of the dialog
	 * 
	 * @param title
	 *            title of the dialog
	 */
	public void setDialogTitle(final String title) {
		mTitle = title;
	}

	public void setPositiveButtonText(final String text) {
		mPositiveButtonText = text;
	}

	public void setNegativeButtonText(final String text) {
		mNegativeButtonText = text;
	}

	/**
	 * simple object to transfere data from dialoglogic to activity logic
	 * 
	 * @param object
	 *            the object to be set
	 */
	public void setObject(final Object object) {
		mObject = object;
	}

	public Object getObject() {
		return mObject;
	}

	@Override
	public void onAttach(final Activity activity) {
		super.onAttach(activity);
		/* if an object is listening check if it implements the needed interface */
		if (mListenable) {
			try {
				mListener = (DialogListener) activity;
			} catch (ClassCastException e) {
				throw new ClassCastException(activity.toString() + " must implement DialogListener");
			}
		}
	}

	/**
	 * This method implements how the dialog's View is being generated and returns it. A layout from resources can be
	 * inflated by using the inflater given by the parameters.
	 * 
	 * @param inflater
	 *            used to inflate the layout for the dialog's view
	 * @return the created View
	 */
	abstract public View generateView(final LayoutInflater inflater);
}
