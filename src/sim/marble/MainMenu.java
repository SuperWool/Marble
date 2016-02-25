package sim.marble;

import java.io.File;
import java.util.Set;

import sim.example.readandroid.R;
import sim.marble.PlotGraph;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainMenu extends Activity {

	private static final boolean D = false;
	private static final String TAG = "Main Menu";
	private static final int REQUEST_ENABLE_BT = 1;
	private static final int REQUEST_OPEN_BT_SETTINGS = 2;

	private final String ACTIVITY_NAME = "MainMenu";
	private final File fileDirectory = new File(Environment.getExternalStorageDirectory(), "Marble");

	private BluetoothAdapter mAdapter = null;
	private Button mReadData, mPlotGraph;
	private Marble mMarble;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (!fileDirectory.isDirectory())
			fileDirectory.mkdir();
		mMarble = (Marble)getApplication();
		mReadData = (Button)findViewById(R.id.read_data);
		mPlotGraph = (Button)findViewById(R.id.plot_graph);

		mReadData.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent readData = new Intent(MainMenu.this, ReadData.class);
				readData.putExtra("FileDir", fileDirectory.getAbsolutePath());
				startActivity(readData);
			}
		});
		mPlotGraph.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent plotGraph = new Intent(MainMenu.this, PlotGraph.class);
				plotGraph.putExtra("CallingActivity", ACTIVITY_NAME);
				startActivity(plotGraph);
			}
		});
		// This check should only be done once in the activity's lifecycle so place this here.
		if (mAdapter==null)
			startBtDialog();
	}

	/**
	 * Starts the AlertDialog that prompts the user to enable their Bluetooth adapter when the
	 * app first starts.
	 */
	private void startBtDialog() {
		new AlertDialog.Builder(this)
		.setIcon(android.R.drawable.ic_dialog_alert)
		.setTitle("Enable BT")
		.setMessage("App needs Bluetooth to be enabled. Choose \"Yes\" to do so, or \"Quit\" to exit.")
		.setCancelable(false)
		.setPositiveButton("Yes", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialogue, int which) {
				mAdapter = BluetoothAdapter.getDefaultAdapter();
				startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
						REQUEST_ENABLE_BT);
			}
		})
		.setNegativeButton("Quit", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialogue, int which) {
				finish();
			}
		})
		.show();		
	}

	/**
	 * BT adapter has already been enabled. Or at least it SHOULD BE
	 * <p>Gets the paired Marble, which will then be assigned to the global BluetoothDevice
	 * variable for ReadData to work on, or if it hasn't been paired yet, bring up a dialogue.
	 * <p>The SerialService and everything is done in the ReadData activity, not here. This is as 
	 * far as we need to go in this MainMenu activity.
	 * <p>Last but not least this is called at the onStart() function.
	 */
	private void getPairedDevice() {
		Set<BluetoothDevice> mmDevices = mAdapter.getBondedDevices();

		// Find BlueBee; once found then break. If it ain't there, bring up the alert dialogue.
		// REMEMBER TO CHANGE BLUEBEE TO WHATEVER THE THING WILL BE CALLED
		for (BluetoothDevice device : mmDevices) {
			if (device.getName().equals("HC-06")) {
				mMarble.setDevice(device);
				break;
			}
		}
		if (mMarble.getDevice()==null) {
			new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle("Marble not found!")
			.setMessage("Marble hasn't been paired yet! Bring up settings to pair it, or quit?")
			.setCancelable(false)
			.setPositiveButton("Open it; make it happen", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
					startActivityForResult(intent, REQUEST_OPEN_BT_SETTINGS);
				}
			})
			.setNegativeButton("Quit", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					finish();
				}
			})
			.show();
		}
	}

	protected void onStart() {
		super.onStart();
		if (D) Log.d(TAG, "+++ On Start +++");
		if (mAdapter!=null) {
			if (mMarble.isAdapterEnabled()) {
				mMarble.ninjaEnableAdapter();
			}
			getPairedDevice();
		}
	}

	protected void onDestroy() {
		if (D) Log.d(TAG, "+++ On Destroy +++");

		// Closes the global and local bluetooth adapters, if they exist and are enabled.
		if (!mMarble.isAdapterNull())
			mMarble.closeAdapter();
		if (mAdapter!=null) {
			if (mAdapter.isEnabled())
				mAdapter.disable();
			mAdapter = null;
			mMarble.setAdapter(null);
		}
		super.onDestroy();
	}

	public void onBackPressed() {
		new AlertDialog.Builder(this)
		.setIcon(android.R.drawable.ic_dialog_alert)
		.setTitle("Quit?")
		.setMessage("Are you sure you want to quit?")
		.setPositiveButton("Yeah", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				finish();
			}
		})
		.setNegativeButton("Nah", null)
		.show();
	}

	public void onActivityResult(int requestCode, int resultCode, Intent action) {
		switch(requestCode) {
		case REQUEST_ENABLE_BT:
			if (resultCode==RESULT_OK) {
				mMarble.setAdapter(mAdapter);
			}
			else {
				Toast.makeText(this, "Bluetooth isn't supported on this device. Exiting",
						Toast.LENGTH_SHORT).show();
				finish();
			}
		case REQUEST_OPEN_BT_SETTINGS:
			getPairedDevice();
			break;
		}
	}
}
