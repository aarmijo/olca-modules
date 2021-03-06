package org.openlca.eigen;

import org.junit.Assert;
import org.junit.Test;

public class EigenTest {

	static {
		TestSession.loadLib();
	}

	@Test
	public void testSuperLu() {
		HashMatrix a = new HashMatrix(2, 2);
		a.setEntry(0, 0, 1);
		a.setEntry(1, 0, -2);
		a.setEntry(1, 1, 1);
		SparseMatrixData data = new SparseMatrixData(a);
		double[] b = new double[2];
		b[0] = 1;
		double[] x = new double[2];
		Eigen.sparseLu(2, data.numberOfEntries, data.rowIndices,
				data.columnIndices, data.values, b, x);
		Assert.assertArrayEquals(new double[] { 1, 2 }, x, 1e-14);
	}

	@Test
	public void testBicgstab() {
		HashMatrix a = new HashMatrix(2, 2);
		a.setEntry(0, 0, 1);
		a.setEntry(1, 0, -2);
		a.setEntry(1, 1, 1);
		SparseMatrixData data = new SparseMatrixData(a);
		double[] b = new double[2];
		b[0] = 1;
		double[] x = new double[2];
		Eigen.bicgstab(2, data.numberOfEntries, data.rowIndices,
				data.columnIndices, data.values, b, x);
		Assert.assertArrayEquals(new double[] { 1, 2 }, x, 1e-14);
	}

	@Test
	public void testBicgstabInvert() {
		HashMatrix a = new HashMatrix(2, 2);
		a.setEntry(0, 0, 1);
		a.setEntry(1, 0, -2);
		a.setEntry(1, 1, 1);
		CompressedRowMatrix m = a.compress();
		double[] inverse = new double[4];
		Eigen.bicgstabInvert(m, inverse);
		Assert.assertArrayEquals(new double[] { 1, 2, 0, 1 }, inverse, 1e-14);
	}

	@Test
	public void testSparseLuInvert() {
		HashMatrix a = new HashMatrix(2, 2);
		a.setEntry(0, 0, 1);
		a.setEntry(1, 0, -2);
		a.setEntry(1, 1, 1);
		SparseMatrixData data = new SparseMatrixData(a);
		double[] inverse = new double[4];
		Eigen.sparseLuInvert(2, data.numberOfEntries, data.rowIndices,
				data.columnIndices, data.values, inverse);
		Assert.assertArrayEquals(new double[] { 1, 2, 0, 1 }, inverse, 1e-14);
	}

}
