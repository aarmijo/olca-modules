package org.openlca.core.matrix;

import org.openlca.core.math.IMatrix;

/**
 * The inventory of a product system where the values are mapped to real
 * matrices.
 */
public class InventoryMatrix {

	private IMatrix technologyMatrix;
	private IMatrix interventionMatrix;
	private ProductIndex productIndex;
	private FlowIndex flowIndex;

	/**
	 * Indicates that there are no elementary flows or technology flows in the
	 * matrix and no result can be calculated.
	 */
	public boolean isEmpty() {
		return flowIndex == null || interventionMatrix == null
				|| flowIndex.size() == 0 || productIndex == null
				|| productIndex.size() == 0 || technologyMatrix == null;
	}

	public IMatrix getTechnologyMatrix() {
		return technologyMatrix;
	}

	public void setTechnologyMatrix(IMatrix technologyMatrix) {
		this.technologyMatrix = technologyMatrix;
	}

	public IMatrix getInterventionMatrix() {
		return interventionMatrix;
	}

	public void setInterventionMatrix(IMatrix interventionMatrix) {
		this.interventionMatrix = interventionMatrix;
	}

	public FlowIndex getFlowIndex() {
		return flowIndex;
	}

	public void setFlowIndex(FlowIndex flowIndex) {
		this.flowIndex = flowIndex;
	}

	public ProductIndex getProductIndex() {
		return productIndex;
	}

	public void setProductIndex(ProductIndex productIndex) {
		this.productIndex = productIndex;
	}

}
