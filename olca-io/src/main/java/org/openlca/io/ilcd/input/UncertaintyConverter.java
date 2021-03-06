package org.openlca.io.ilcd.input;

import java.math.BigDecimal;

import org.openlca.core.model.Uncertainty;
import org.openlca.ilcd.commons.UncertaintyDistribution;
import org.openlca.ilcd.processes.Exchange;
import org.openlca.ilcd.processes.Parameter;
import org.openlca.ilcd.util.ExchangeExtension;

/**
 * Maps uncertainty information of exchanges and parameters.
 */
class UncertaintyConverter {

	public void map(Exchange iExchange,
			org.openlca.core.model.Exchange oExchange) {
		if (iExchange.getUncertaintyDistribution() == null
				|| iExchange.getUncertaintyDistribution() == UncertaintyDistribution.UNDEFINED)
			return;
		switch (iExchange.getUncertaintyDistribution()) {
		case LOG_NORMAL:
			mapLogNormal(iExchange, oExchange);
			break;
		case NORMAL:
			mapNormal(iExchange, oExchange);
			break;
		case TRIANGULAR:
			mapTriangular(iExchange, oExchange);
			break;
		case UNIFORM:
			mapUniform(iExchange, oExchange);
			break;
		default:
			break;
		}
	}

	public void map(Parameter iParameter,
			org.openlca.core.model.Parameter oParameter) {
		if (iParameter.getUncertaintyDistributionType() == null
				|| iParameter.getUncertaintyDistributionType() == UncertaintyDistribution.UNDEFINED)
			return;
		switch (iParameter.getUncertaintyDistributionType()) {
		case LOG_NORMAL:
			mapLogNormal(iParameter, oParameter);
			break;
		case NORMAL:
			mapNormal(iParameter, oParameter);
			break;
		case TRIANGULAR:
			mapTriangular(iParameter, oParameter);
			break;
		case UNIFORM:
			mapUniform(iParameter, oParameter);
			break;
		default:
			break;
		}
	}

	private void mapLogNormal(Exchange iExchange,
			org.openlca.core.model.Exchange oExchange) {
		double mean = getAmount(iExchange);
		BigDecimal bigDec = iExchange.getRelativeStandardDeviation95In();
		if (bigDec == null)
			return;
		double s = bigDec.doubleValue();
		oExchange.setUncertainty(Uncertainty.logNormal(mean, s));
	}

	private void mapLogNormal(Parameter iParameter,
			org.openlca.core.model.Parameter oParameter) {
		Double mean = iParameter.getMeanValue();
		BigDecimal std = iParameter.getRelativeStandardDeviation95In();
		if (mean == null || std == null)
			return;
		oParameter
				.setUncertainty(Uncertainty.logNormal(mean, std.doubleValue()));
	}

	private void mapNormal(Exchange iExchange,
			org.openlca.core.model.Exchange oExchange) {
		double mean = getAmount(iExchange);
		BigDecimal bigDec = iExchange.getRelativeStandardDeviation95In();
		if (bigDec == null)
			return;
		double s = bigDec.doubleValue();
		oExchange.setUncertainty(Uncertainty.normal(mean, s));
	}

	private void mapNormal(Parameter iParameter,
			org.openlca.core.model.Parameter oParameter) {
		Double mean = iParameter.getMeanValue();
		BigDecimal std = iParameter.getRelativeStandardDeviation95In();
		if (mean == null || std == null)
			return;
		oParameter.setUncertainty(Uncertainty.normal(mean, std.doubleValue()));
	}

	private void mapTriangular(Exchange iExchange,
			org.openlca.core.model.Exchange oExchange) {
		Double min = iExchange.getMinimumAmount();
		Double mode = new ExchangeExtension(iExchange).getMostLikelyValue();
		Double max = iExchange.getMaximumAmount();
		if (min == null || mode == null || max == null)
			return;
		oExchange.setUncertainty(Uncertainty.triangle(min, mode, max));
	}

	private void mapTriangular(Parameter iParameter,
			org.openlca.core.model.Parameter oParameter) {
		Double min = iParameter.getMinimumValue();
		Double mean = iParameter.getMeanValue();
		Double max = iParameter.getMaximumValue();
		if (min == null || mean == null || max == null)
			return;
		oParameter.setUncertainty(Uncertainty.triangle(min, mean, max));
	}

	private void mapUniform(Exchange iExchange,
			org.openlca.core.model.Exchange oExchange) {
		Double min = iExchange.getMinimumAmount();
		Double max = iExchange.getMaximumAmount();
		if (min == null || max == null)
			return;
		oExchange.setUncertainty(Uncertainty.uniform(min, max));
	}

	private void mapUniform(Parameter iParameter,
			org.openlca.core.model.Parameter oParameter) {
		Double min = iParameter.getMinimumValue();
		Double max = iParameter.getMaximumValue();
		if (min == null || max == null)
			return;
		oParameter.setUncertainty(Uncertainty.uniform(min, max));
	}

	private double getAmount(Exchange iExchange) {
		ExchangeExtension ext = new ExchangeExtension(iExchange);
		Double val = ext.getAmount();
		if (val != null)
			return val;
		val = iExchange.getResultingAmount();
		if (val != null)
			return val;
		return iExchange.getMeanAmount();
	}

}
