package at.codecomb.dialog;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

/*
 * Copyright (c) 2013, All Rights Reserved, file = GenericDialog.java
 * 
 * This source is subject to Code Comb. 
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the "Software"), 
 * to deal in the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software 
 * is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
 * OR OTHER DEALINGS IN THE SOFTWARE.
 */

/**
 * When creating dialogs with a DialogBuilder most of the time the same source code is used and not many things differ. That is the goal of
 * GenericDialog, creating dialogs fast and easy. On the other hand GenericDialogs do have one more point to them which make them more advanced
 * compared to standart DialogFragments, they are listenable by implementing DialogListener.
 * 
 * @author David Riedl (Code Comb)
 * @version 1.3
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

	public void setListenable(boolean listenable) {
		mListenable = listenable;
	}

	public void setDialogTag(String tag) {
		mTAG = tag;
	}

	public String getDialogTag() {
		return mTAG;
	}

	public void setDialogTitle(String title) {
		mTitle = title;
	}

	public void setPositiveButtonText(String text) {
		mPositiveButtonText = text;
	}

	public void setNegativeButtonText(String text) {
		mNegativeButtonText = text;
	}

	public void setObject(Object object) {
		mObject = object;
	}

	public Object getObject() {
		return mObject;
	}

	@Override
	public void onAttach(Activity activity) {
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
	 * This method implements how the dialog's View is being generated and returns it. A layout from resources can be inflated by using the inflater
	 * given by the parameters.
	 * 
	 * @param inflater
	 *            used to inflate the layout for the dialog's view
	 * @return the created View
	 */
	abstract public View generateView(LayoutInflater inflater);
}
