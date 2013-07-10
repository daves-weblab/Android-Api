package at.codecomb.dialog;

/*
 * Copyright (c) 2013, All Rights Reserved, file = GenericDialogListener.java
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
 * Activities which start GenericDialogs using show must implement this interface when the GenericDialog is set to be listenable. The GenericDialog
 * will set the Activity as listener in onAttach and call the methods on the Activity when buttons were pressed
 * 
 * @author David Riedl (Code Comb)
 * @version 1.0
 */
public interface DialogListener {
	/**
	 * this method is called on the listening Activity when the positive button in the GenericDialog is pressed
	 * 
	 * @param dialog
	 *            the dialog sends itself so the activity might retrieve content
	 */
	public void onDialogPositiveClick(GenericDialog dialog);

	/**
	 * this method is called on the listening Activity when the negative button in the GenericDialog is pressed
	 * 
	 * @param dialog
	 *            the dialog sends itself so the activity might retrieve content
	 */
	public void onDialogNegativeClick(GenericDialog dialog);
}
