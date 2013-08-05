package org.openlca.core.model.descriptors;

import org.openlca.core.model.FlowType;
import org.openlca.core.model.ModelType;
import org.openlca.util.Strings;

public class FlowDescriptor extends BaseDescriptor {

	private static final long serialVersionUID = 4292185203406513488L;

	private String locationCode;
	private FlowType flowType;

	public FlowDescriptor() {
		setType(ModelType.FLOW);
	}

	public String getLocationCode() {
		return locationCode;
	}

	public void setLocationCode(String locationCode) {
		this.locationCode = locationCode;
	}

	public FlowType getFlowType() {
		return flowType;
	}

	public void setFlowType(FlowType flowType) {
		this.flowType = flowType;
	}

	@Override
	public String getDisplayName() {
		String name = getName();
		String disp = name == null ? "no name" : Strings.cut(name, 75);
		if (locationCode != null)
			disp = disp.concat(" (").concat(locationCode).concat(")");
		return disp;
	}

}