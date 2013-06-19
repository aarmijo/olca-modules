package org.openlca.core.model.results;

import java.util.List;

import org.openlca.core.model.Indexable;

/** Class for calculating the relative share of a contribution. */
class ContributionShare {

	private ContributionShare() {
	}

	public static void calculate(
			List<? extends Contribution<? extends Indexable>> contributions) {
		if (contributions == null || contributions.isEmpty())
			return;
		double refVal = getRefValue(contributions);
		for (Contribution<? extends Indexable> c : contributions) {
			double share = share(c.getAmount(), refVal);
			c.setShare(share);
		}
	}

	private static double share(double val, double refValue) {
		if (refValue == 0)
			return 0;
		return (val / refValue);
	}

	private static double getRefValue(
			List<? extends Contribution<? extends Indexable>> contributions) {
		Contribution<? extends Indexable> first = contributions.get(0);
		double max = first.getAmount();
		double min = max;
		for (int i = 1; i < contributions.size(); i++) {
			Contribution<? extends Indexable> next = contributions.get(i);
			double nextVal = next.getAmount();
			max = Math.max(max, nextVal);
			min = Math.min(min, nextVal);
		}
		return Math.max(Math.abs(max), Math.abs(min));
	}

}