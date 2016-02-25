/*
 * This is the PlotGraph activity which, as the name would suggest, plots a graph based on data
 * from a text file, which are represented as string literals.
 * The data is first read from the file and fed into an IntMatrix object, which is a one-dimensional
 * integer matrix acting as a two-dimensional one.  
 * From there, the graph can now be plotted. Graph is coded with the GraphView APIs
 * (android-graphview.org).
 */

package sim.marble;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import sim.example.readandroid.R;

import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphView.LegendAlign;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewStyle.GridStyle;
import com.jjoe64.graphview.LineGraphView;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;

public class PlotGraph extends Activity {

	// For debugging purposes; set D to "true" if debugging is needed
	private static final boolean D = false;
	private static final String TAG = "Plot Graph";

	// Static constants 
	private static final String MAIN_MENU_ACTIVITY = "MainMenu";
	private static final String READ_DATA_ACTIVITY = "ReadData";
	private static final String SAVE_DATA = "Chosen_file";
	private static final String COLS = "cols";
	private static final String ROWS = "rows";

	private final File FILE_DIR = new File(Environment.getExternalStorageDirectory(), "Marble");

	private AlertDialog mStartDialog;
	private BufferedReader mBufferedReader;
	private GraphViewData[] mDataPosX, mDataPosY, mDataPosZ, mDataLum;
	private GraphViewSeries mSeriesPosX, mSeriesPosY, mSeriesPosZ, mSeriesLum;
	//	private int mMaxValue = 255;
	private IntMatrix mDataValues;
	private LinearLayout mGraph;
	private LineGraphView mGraphView;
	private List<String> mFileNames;
	private String mCallingActivity, mGraphTitle;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.plot_graph);

		mFileNames = new ArrayList<String>();
		mGraph = (LinearLayout)findViewById(R.id.graph);

		mCallingActivity = getIntent().getStringExtra("CallingActivity");
		if (savedInstanceState==null) {
			if (mCallingActivity.equals(READ_DATA_ACTIVITY)) {
				readFile(getIntent().getStringExtra("File"));
			} else if (mCallingActivity.equals(MAIN_MENU_ACTIVITY)) {
				inflateFileList();
			}
		}
	}

	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		int[] selected = savedInstanceState.getIntArray(SAVE_DATA);
		if (selected!=null) {
			mDataValues = new IntMatrix(selected, savedInstanceState.getInt(ROWS),
					savedInstanceState.getInt(COLS));
			plotGraph();
		} else {
			if (mCallingActivity.equals(MAIN_MENU_ACTIVITY)) {
				inflateFileList();
			}
		}
	}

	protected void onSaveInstanceState(Bundle outState) {
		if (mDataValues!=null) {
			if (D) Log.i(TAG, "mDataValues being fed into the outState");
			outState.putIntArray(SAVE_DATA, mDataValues.toArray());
			outState.putInt(COLS, mDataValues.getCols());
			outState.putInt(ROWS, mDataValues.getRows());
		}
		super.onSaveInstanceState(outState);
	}

	/**
	 * <p>Inflates an AlertDialog containing all the files that are present in the directory.</p>
	 * <p>The selected file's name will be passed into readFile() where the data in that file
	 * will be plotted.
	 */
	private void inflateFileList() {
		// Passes the filenames into the List, i.e. just "Bla.txt" instead of "/storage/..."
		final File[] mmFiles = FILE_DIR.listFiles();
		DecimalFormat mmFormat = new DecimalFormat("0.00");
		String item, fileName;
		for (File file : mmFiles) {
			fileName = file.getPath().substring(FILE_DIR.getPath().length()+1);
			item = fileName +
					" (" + mmFormat.format(file.length()/1024) + "kB)";
			mFileNames.add(item);
		}
		String[] mmFileNames = new String[mFileNames.size()];
		mmFileNames = mFileNames.toArray(mmFileNames);
		sortFileNames(mmFileNames);
		mStartDialog = new AlertDialog.Builder(this)
		.setTitle("Choose the file to use:")
		.setOnCancelListener(new DialogInterface.OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialogue) {
				dialogue.cancel();
				mStartDialog = null;
				finish();
			}
		})
		.setItems(mmFileNames, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialogue, int which) {
				readFile(mmFiles[which].getPath());
			}
		})
		.show();
	}

	private static void sortFileNames(String[] files) {
		int count = 0, i=0, length = files.length-1;
		String tmp;
		while (count != length) {
			if (files[i].compareTo(files[i+1])>0) {
				count = 0;
				tmp = files[i];
				files[i] = files[i+1];
				files[i+1] = tmp;
			}
			count++;
			if (i==length-1)
				i=0;
			else i++;
		}
		if (D) {
			for (String a : files)
				Log.i(TAG, a);
		}
	}

	/**
	 * <p>Read file character by character, and feed values into dataValues.</p>
	 * Once a whitespace is read, the value is parsed into the appropriate column in the
	 * int double array.</br>
	 * Once a newline is read, the array dynamically gets a new row, except when a new row
	 * which doesn't have a full set of values has been hit. When that happens, the loop
	 * breaks, because that's when we're done.
	 * <p>By default the max y value, or mMaxValue is 255, which is the max value the
	 * accelerometer will spit out, but the light sensor can go up to 50k. mMaxValue will
	 * be reset to the highest value in the data if needed.
	 * @param fileName The name of the file to be read from
	 */
	private void readFile(String fileName) {
		File file = new File(fileName);
		mGraphTitle = fileName.substring(FILE_DIR.getPath().length()+1, fileName.indexOf('.'));
		try {
			if (file==null || !file.isFile()) {
				Toast.makeText(this, "File can't be found", Toast.LENGTH_SHORT).show();
				return;
			}
			if (!file.canRead()) {
				Toast.makeText(this, "File can't be read", Toast.LENGTH_SHORT).show();
			}
			if (D) Log.d(TAG, "File found, feeding values into dataValues");

			mBufferedReader = new BufferedReader(new FileReader(file));
			String[] parts;
			String line;
			int rowPos = 1, colPos = 1, data;
			mDataValues = new IntMatrix(1,10);
			while ((line = mBufferedReader.readLine())!=null) {
				parts = line.split(" ");
				if (parts.length!=10)
					continue;
				for (String part : parts) {
					// parse the string literal into an integer and feed it to data
					data = Integer.parseInt(part);
					// if the lux value aka the data at the ninth colPos is greater
					// than mMaxValue, feed data into mMaxValue
					//					if ((data>mMaxValue) && (colPos==10))
					//						mMaxValue = data;
					mDataValues.set(rowPos, colPos, data);
					colPos++;
				}
				colPos = 1;
				rowPos++;
				mDataValues.addRow();
			}
			// If everything's OK, close the things and start to plot the graph;
			mBufferedReader.close();
			if (D) {
				Log.d(TAG, "Read successful, plotting graph");
				Log.d(TAG, "Size of dataValues: " + mDataValues.getSize());
			}
			plotGraph();
		} catch(FileNotFoundException e) {
			Log.e(TAG, "Error opening file: " + e.getMessage());
		} catch (IOException e) {
			Log.e(TAG, "Error reading(IOException): " + e.getMessage());
		}
	}

	/**
	 * <p>FIX THE DATALENGTH VARIABLE IF THE WAY DATA'S RECORDED IN THE ARDUINO HAS CHANGED
	 * <br>FOR EXAMPLE IF THERE AREN'T ANY SIX-COLUMN ROWS ANYMORE
	 * <p>As the name would suggest, this plots a graph from the data that's been read.</p>
	 * The data is dynamically appended value by value and the graph is redrawn to fit
	 * all the data that's currently been read to give a visual effect.
	 * @param dataValues The values to be plotted
	 */
	private void plotGraph() {

		// Get the number of rows dataValues has sans the last row which doesn't have ten columns
		int dataLength = mDataValues.getRows()-1;

		// Initialise four different GraphViewData[] for the four different sets o' data
		// Feed the appropriate values into them
		mDataPosX = new GraphViewData[dataLength];
		mDataPosY = new GraphViewData[dataLength];
		mDataPosZ = new GraphViewData[dataLength];
		mDataLum = new GraphViewData[dataLength];

		// Feed the values in, create a new line graph, and feed the data into it
		for (int i=0; i<dataLength; i++) {
			mDataPosX[i] = new GraphViewData(i,mDataValues.get(i+1, 7));
			mDataPosY[i] = new GraphViewData(i,mDataValues.get(i+1, 8));
			mDataPosZ[i] = new GraphViewData(i,mDataValues.get(i+1, 9));
			mDataLum[i]  = new GraphViewData(i,mDataValues.get(i+1, 10));
		}

		// Create a new line graph and add each dataset as an individual series
		mGraphView = new LineGraphView(this, mGraphTitle);

		mSeriesPosX = new GraphViewSeries("XPos/mG", null, mDataPosX);
		mSeriesPosY = new GraphViewSeries("YPos/mG", null, mDataPosY);
		mSeriesPosZ = new GraphViewSeries("ZPos/mG", null, mDataPosZ);
		mSeriesLum = new GraphViewSeries("Luminance/lux", null, mDataLum);
		mSeriesPosX.getStyle().color = Color.rgb(108, 98, 13);
		mSeriesPosY.getStyle().color = Color.rgb(100, 150, 25);
		mSeriesPosZ.getStyle().color = Color.rgb(30,180,20);
		mSeriesLum.getStyle().color = Color.rgb(30,10,200);

		mGraphView.addSeries(mSeriesPosX);
		mGraphView.addSeries(mSeriesPosY);
		mGraphView.addSeries(mSeriesPosZ);
		mGraphView.addSeries(mSeriesLum);

		// Set the graph's style up, add a legend and finally plot the graph on screen
		mGraphView.getGraphViewStyle().setGridStyle(GridStyle.VERTICAL);
		mGraphView.getGraphViewStyle().setHorizontalLabelsColor(0xFFA81414);
		mGraphView.getGraphViewStyle().setVerticalLabelsColor(0xFFA81414);
		mGraphView.setDrawDataPoints(true);
		mGraphView.setDataPointsRadius((float)5);
		mGraphView.setScalable(true);

		mGraphView.setShowLegend(true);
		mGraphView.setLegendAlign(LegendAlign.TOP);
		mGraphView.getGraphViewStyle().setLegendSpacing(30);
		mGraphView.getGraphViewStyle().setLegendWidth(300);
		mGraph.addView(mGraphView);
	}

	//////////////////////////
	//*   LOGS 'N' STUFF   *//
	//////////////////////////
	protected void onStart() {
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
		if (mStartDialog!=null && mStartDialog.isShowing())
			mStartDialog.dismiss();
		super.onDestroy();
	}
}
