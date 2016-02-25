package sim.marble;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

public class Marble extends Application {
	private BluetoothAdapter mAdapter;
	private BluetoothDevice mDevice;
	private boolean mIsAdapterNull = true;
	private boolean mIsAdapterEnabled = false;

	/**
	 * Gets the global BT adapter.
	 * Gotta check if it's enabled on its own later.
	 * @return <tt>null</tt> if it hasn't been initiated, or the adapter itself if it has.
	 */
	public BluetoothAdapter getAdapter() {
		if (mIsAdapterNull)
			return null;
		return mAdapter;
	}
	
	/**
	 * Sets and enables the global BT adapter.
	 */
	public void setAdapter(BluetoothAdapter adapter) {
		mAdapter = adapter;
		mIsAdapterNull = false;
		mIsAdapterEnabled = true;
	}

	/**
	 * Get the device set in MainMenu. To be used in ReadData
	 * @return mDevice The device (BlueBee)
	 */
	public BluetoothDevice getDevice() {
		return mDevice;
	}
	
	/**
	 * Sets the device to be used by ReadData later.
	 * Make sure this is done in MainMenu.
	 * @param d The device to be put into the class (BlueBee).
	 */
	public void setDevice(BluetoothDevice d) {
		mDevice = d;
	}
	
	/**
	 * Enables adapter discreetly.<br>Not sure if I should use this tho; gotta think about
	 * if home button's pressed while data's bein read.</br>
	 * <br>ninjaDisableAdapter() exists too.
	 */
	public void ninjaEnableAdapter() {
		if (mAdapter!=null && !mAdapter.isEnabled())
			mAdapter.enable();
		mIsAdapterEnabled = true;
	}
	
	/**
	 * Disables adapter discreetly.<br>Not sure if I should use this tho.
	 */
	public void ninjaDisableAdapter() {
		if (mAdapter!=null && mAdapter.isEnabled())
			mAdapter.disable();
		mIsAdapterEnabled = false;
	}

	/**
	 * Closes the adapter. More specifically disables it if it's enabled, and then sets the
	 * adapter to null.
	 */
	public void closeAdapter() {
		if (mAdapter!=null) {
			if (mAdapter.isEnabled())
				mAdapter.disable();
			mAdapter=null;
		}
	}
	
	/**
	 * Checks if the global BT adapter hasn't been initiated.
	 * <p>Use before isAdapterEnabled() which checks if it has been enabled. 
	 */
	public boolean isAdapterNull() {
		return mIsAdapterNull;
	}
	
	/**
	 * Checks if the global BT adapter has been enabled.
	 * <p>Use after isAdapterSet() which checks if it has been initiated. 
	 * @return <tt>true</tt> if the adapter is enabled, <tt>false</tt> if it isn't.
	 */
	public boolean isAdapterEnabled() {
		return mIsAdapterEnabled;
	}
}
