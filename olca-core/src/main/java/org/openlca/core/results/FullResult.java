package org.openlca.core.results;

import org.openlca.core.math.IMatrix;
import org.openlca.core.matrix.LongPair;

/**
 * A full result extends the base and contribution result by providing
 * additionally all calculated upstream-results for intervention flows and LCIA
 * categories for each single process-product in the system.
 */
public class FullResult extends ContributionResult {

	protected IMatrix upstreamFlowResults;
	protected IMatrix upstreamImpactResults;

	public void setUpstreamFlowResults(IMatrix upstreamFlowResults) {
		this.upstreamFlowResults = upstreamFlowResults;
	}

	/**
	 * Get the upstream flow results in a matrix where the flows are mapped to
	 * the rows and the process-products to the columns. Inputs have negative
	 * values here.
	 */
	public IMatrix getUpstreamFlowResults() {
		return upstreamFlowResults;
	}

	/**
	 * Get the upstream flow result of the flow with the given ID for the given
	 * process-product. Inputs have negative values here.
	 */
	public double getUpstreamFlowResult(LongPair processProduct, long flowId) {
		int row = flowIndex.getIndex(flowId);
		int col = productIndex.getIndex(processProduct);
		return getValue(upstreamFlowResults, row, col);
	}

	/**
	 * Get the sum of the upstream flow results of the flow with the given ID
	 * for all products of the process with the given ID. Inputs have negative
	 * values here.
	 */
	public double getUpstreamFlowResult(long processId, long flowId) {
		int row = flowIndex.getIndex(flowId);
		return getProcessValue(upstreamFlowResults, row, processId);
	}

	public void setUpstreamImpactResults(IMatrix upstreamImpactResults) {
		this.upstreamImpactResults = upstreamImpactResults;
	}

	/**
	 * Get the upstream LCIA category results in a matrix where the LCIA
	 * categories are mapped to the rows and the process-products to the
	 * columns.
	 */
	public IMatrix getUpstreamImpactResults() {
		return upstreamImpactResults;
	}

	/**
	 * Get the upstream LCIA category result of the LCIA category with the given
	 * ID for the given process-product.
	 */
	public double getUpstreamImpactResult(LongPair processProduct, long impactId) {
		if (!hasImpactResults())
			return 0;
		int row = impactIndex.getIndex(impactId);
		int col = productIndex.getIndex(processProduct);
		return getValue(upstreamImpactResults, row, col);
	}

	/**
	 * Get the sum of the upstream LCIA category results of the LCIA category
	 * with the given ID for all products of the process with the given ID.
	 */
	public double getUpstreamImpactResult(long processId, long impactId) {
		if (!hasImpactResults())
			return 0;
		int row = impactIndex.getIndex(impactId);
		return getProcessValue(upstreamImpactResults, row, processId);
	}

}
