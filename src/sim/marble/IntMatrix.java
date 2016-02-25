/*
 * This is the IntMatrix class. It is a one-dimensional integer matrix acting as a two-dimensional one,
 * with functions to work on this matrix.
 */

package sim.marble;

import java.util.Arrays;

public class IntMatrix {
	private int rows;
	private int cols;
	private int[] data;

	/**
	 * Allocate a matrix with the indicated initial dimensions.
	 * @param rows The row (vertical or y) dimension for the matrix
	 * @param cols The column (horizontal or x) dimension for the matrix
	 */
	public IntMatrix(int rows, int cols) {
		this.rows = rows;
		this.cols = cols;
		data = new int[cols * rows];
	}

	/**
	 * Wraps an existing int[] array into an IntMatrix object with the specified
	 * dimensions.
	 * @param data The primitive int[] array to wrap
	 * @param rows The row (vertical or y) dimension for the matrix 
	 * @param cols The column (horizontal or x) dimension for the matrix
	 */
	public IntMatrix(int[] data, int rows, int cols) {
		this.rows = rows;
		this.cols = cols;
		this.data = data;
	}

	/**
	 * Calculates the index of the indicated row and column for
	 * a matrix with the indicated width. This uses row-major ordering
	 * of the matrix elements.
	 * <p>
	 * Note that this is a static method so that it can be used independent
	 * of any particular data instance.
	 * @param col The column index of the desired element
	 * @param row The row index of the desired element
	 * @param width The width of the matrix aka the number of columns aka this.cols
	 */
	private static int getIndex(int row, int col, int width) {
		return (row-1) * width + (col-1);
	}

	/**
	 * Adds a new row to the matrix
	 */
	public void addRow() {
		data = Arrays.copyOf(data, ++rows*cols);
	}

	/**
	 * Returns the value at the specified index
	 * @return The data at the wanted index
	 */
	public int get(int row, int col) {
		return data[getIndex(row, col, cols)];
	}

	/**
	 * Returns the number of columns of the matrix
	 * @return
	 */
	public int getCols() {
		return cols;
	}

	/**
	 * Returns the number of rows of the matrix
	 * @return
	 */
	public int getRows() {
		return rows;
	}

	/**
	 * Returns the size of the matrix (the product of the rows and the columns of the matrix)
	 * @return A cookie for you if you guess it right
	 */
	public int getSize() {
		return rows*cols;
	}

	/**
	 * Resizes the matrix. The values in the current matrix are placed
	 * at the top-left corner of the new matrix. In each dimension, if
	 * the new size is smaller than the current size, the data are
	 * truncated; if the new size is larger, the remainder of the values
	 * are set to 0.
	 * @param rows The new row (vertical) dimension for the matrix
	 * @param cols The new column (horizontal) dimension for the matrix
	 */
	public void resize(int rows, int cols) {
		int [] newData = new int[cols * rows];
		int colsToCopy = Math.min(cols, this.cols);
		int rowsToCopy = Math.min(rows, this.rows);
		for (int i = 1; i <= rowsToCopy; ++i) {
			int oldRowStart = getIndex(1, i, this.cols);
			int newRowStart = getIndex(1, i, cols);
			System.arraycopy(data, oldRowStart, newData, newRowStart,
					colsToCopy
					);
		}
		this.rows = rows;
		this.cols = cols;
		data = newData;
	}

	/**
	 * Sets the value at the specified index
	 * @param row Row of the matrix where the data will be set
	 * @param col Column of the matrix where the data will be set
	 * @param value The value to be set
	 */
	public void set(int row, int col, int value) {
		data[getIndex(row, col, cols)] = value;
	}

	/**
	 * Returns the data in the primitive int[] form.
	 */
	public int[] toArray() {
		return data;
	}
}