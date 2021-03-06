package org.openlca.eigen;

import org.openlca.core.math.IMatrix;

/**
 * While the interface provides double precision values this matrix
 * implementation stores the data in a single precision array internally. The
 * data are stored in column major order.
 */
public class DenseFloatMatrix implements IMatrix {

	private final float[] data;
	private final int rows;
	private final int columns;

	public DenseFloatMatrix(int rows, int columns) {
		this.rows = rows;
		this.columns = columns;
		data = new float[rows * columns];
	}

	public float[] getData() {
		return data;
	}

	@Override
	public int getRowDimension() {
		return rows;
	}

	@Override
	public int getColumnDimension() {
		return columns;
	}

	@Override
	public void setEntry(int row, int col, double val) {
		data[index(row, col)] = (float) val;
	}

	public void setFloatEntry(int row, int col, float val) {
		data[index(row, col)] = val;
	}

	@Override
	public double getEntry(int row, int col) {
		return data[index(row, col)];
	}

	public float getFloatEntry(int row, int col) {
		return data[index(row, col)];
	}

	@Override
	public double[] getColumn(int i) {
		double[] col = new double[rows];
		for (int r = 0; r < rows; r++) {
			col[r] = getEntry(r, i);
		}
		return col;
	}

	@Override
	public double[] getRow(int i) {
		double[] row = new double[columns];
		for (int c = 0; c < columns; c++) {
			row[c] = getEntry(i, c);
		}
		return row;
	}

	private int index(int row, int column) {
		return row + rows * column;
	}

	@Override
	public DenseFloatMatrix copy() {
		DenseFloatMatrix copy = new DenseFloatMatrix(rows, columns);
		System.arraycopy(data, 0, copy.data, 0, data.length);
		return copy;
	}

}
