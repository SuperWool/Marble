/*
 * This is the ReadData activity which reads data from the microcontroller. All work is mine,
 * save for the BluetoothSerialService class. That's adapted from the BlueTerm app by Pymasde (pymasde.es,
 * source code: https://code.google.com/p/bluetooth-remote-control/source/browse/trunk/src/pro/apus/
 * blueremote/BluetoothSerialService.java?r=3).
 * That source code, in turn, was adapted from the BluetoothChatService class by Android's BluetoothChat
 * app. 
 */

package sim.marble;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import sim.example.readandroid.R;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class ReadData extends Activity {

	private static final boolean D = false;
	private static final String TAG = "Read Data";

	// Message types sent from the BluetoothReadService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;	

	// Key names received from the BluetoothChatService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";

	private boolean isDone = false, hasStarted = true;
	private int mBytesRead = 0;
	private final String ACTIVITY_NAME = "ReadData";

	private final File FILE_DIR = new File(Environment.getExternalStorageDirectory(), "Marble");

	private Button plotGraph;
	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothDevice mDevice = null;
	private BluetoothSerialService mBluetoothSerialService;
	private File currentFile;
	private Marble mMarble;
	private PrintWriter mPrintWriter;
	private SimpleDateFormat mDateFormat = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
	private TextView mTitle, mProgress;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_read_data);

		mMarble = (Marble)getApplication();

		// Creates the file for the data to be written to
		if (!FILE_DIR.isDirectory())
			FILE_DIR.mkdir();
		currentFile = new File(FILE_DIR, mDateFormat.format(new Date()) + ".txt");
		try {
			if (!currentFile.isFile())
				currentFile.createNewFile();
			mPrintWriter = new PrintWriter(currentFile);
			if (D) Log.i(TAG, "mPrintWriter created");
			if (D) Log.i(TAG, currentFile.getAbsolutePath());
		} catch (IOException e) {
			Log.e(TAG, "IOException at file creation/mPrintWriter: " + e.getMessage());
		}
		mProgress = (TextView)findViewById(R.id.progress);
		mTitle = (TextView)findViewById(R.id.title_text_right);
		plotGraph = (Button)findViewById(R.id.b1);

		plotGraph.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				if (isDone)
					startPlotGraphActivity(currentFile.getAbsolutePath());
				else butter("Not done receiving yet!");
			}
		});

		// Set up AND enable BT in the main menu screen
		mBluetoothAdapter = mMarble.getAdapter();
		mDevice = mMarble.getDevice();

		// BT adapter should already be enabled in the main menu
		if (mBluetoothAdapter.isEnabled()) {
			mBluetoothSerialService = new BluetoothSerialService(this, mBtHandler);
			mBluetoothSerialService.start();
		} else {
			butter("Adapter wasn't enabled!");
			finish();
		}
		mBluetoothSerialService.connect(mDevice);
	}

	private void butter(String message) {
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
	}

	/**
	 * Starts the PlotGraph activity to plot the results.
	 * <p>PlotGraph reads data from a file and plots data from there, and ReadData immediately
	 * writes to a file as data is being read. Hence, the filename where the data is stored
	 * is passed into this function to tell which file PlotGraph should read from.
	 * @param fileName The name of the file which PlotGraph will read from
	 */
	private void startPlotGraphActivity(String fileName) {
		Intent intent = new Intent(this, PlotGraph.class);
		intent.putExtra("File", fileName);
		intent.putExtra("CallingActivity", ACTIVITY_NAME);
		startActivity(intent);
	}

	// The Handler that gets information back from the BluetoothService
	@SuppressLint("HandlerLeak")
	private Handler mBtHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {        	
			switch (msg.what) {
			case MESSAGE_STATE_CHANGE:
				switch (msg.arg1) {
				case BluetoothSerialService.STATE_CONNECTED:
					if(D) Log.i(TAG, "Connected to device");
					mTitle.setText("Connected to device");
					hasStarted = true;
					break;

				case BluetoothSerialService.STATE_CONNECTING:
					if(D) Log.i(TAG, "Connecting to device");
					mTitle.setText("Connecting to device...");
					break;

				case BluetoothSerialService.STATE_LISTEN:
				case BluetoothSerialService.STATE_NONE:
					if (hasStarted) {
						hasStarted = false;
						isDone = true;
						if (currentFile.length()==0) {
							if(D) Log.i(TAG, "No data, deleting");
							mTitle.setText("No data received!");
							butter("No data received!!");
						}
						if(D) Log.i(TAG, "Data receiving is DONE WOWOWO");
						mTitle.setText("Done!");
						butter("Data received! Saved at: " + currentFile.getPath());
					} else {
						if(D) Log.i(TAG, "Not connected to device");
						mTitle.setText("Not connected yet");
					}
					break;
				}
				break;

			case MESSAGE_READ:
				String message = (String)msg.obj;
				if (D) Log.i(TAG, message);
				mPrintWriter.write((String)message);
				mProgress.setText(String.valueOf(mBytesRead+=msg.arg1) + " bytes received");
				break;

			case MESSAGE_DEVICE_NAME:
				String mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
				Toast.makeText(ReadData.this, "Connected to: "
						+ mConnectedDeviceName, Toast.LENGTH_SHORT).show();
				break;

			case MESSAGE_TOAST:
				Toast.makeText(ReadData.this, msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
				break;
			}
		}
	};    

	//////////////////////////
	//*   LOGS 'N' STUFF   *//
	//////////////////////////
	protected void onStart() {
		if (!mBluetoothAdapter.isEnabled())
			mBluetoothAdapter.enable();
		super.onStart();
		if (D) Log.i(TAG, "+++ On Start +++");
	}

	protected void onResume() {
		super.onResume();
		if (D) Log.i(TAG, "+++ On Resume +++");
	}

	protected void onPause() {
		super.onPause();
		if (D) Log.i(TAG, "+++ On Pause+++");
	}

	protected void onStop() {
		super.onStop();
		if (D) Log.i(TAG, "+++ On Stop+++");
	}

	protected void onDestroy() {
		// Close all the things: SerialService, and the PrintWriter. And maybe other things.
		if (mBluetoothSerialService!=null) {
			mBluetoothSerialService.stop();
			mBluetoothSerialService = null;
		}
		if (mPrintWriter!=null)
			mPrintWriter.close();
		if (currentFile.length()==0)
			currentFile.delete();
		super.onDestroy();
		if (D) Log.i(TAG, "+++ On Destroy +++");
	}
}
