package at.ac.uibk.dialog;

/**
 * Activities which start GenericDialogs using show must implement this interface when the GenericDialog is set to be listenable. The GenericDialog will set the Activity as listener in onAttach and
 * call the methods on the Activity when buttons were pressed
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
