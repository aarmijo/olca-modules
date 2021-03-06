package org.openlca.jsonld;

import org.openlca.core.model.ModelType;

public final class ModelPath {

	private ModelPath() {
	}

	public static String get(ModelType type) {
		if (type == null)
			return "";
		switch (type) {
		case CATEGORY:
			return "categories";
		case PROCESS:
			return "processes";
		case FLOW:
			return "flows";
		case FLOW_PROPERTY:
			return "flow_properties";
		case ACTOR:
			return "actors";
		case IMPACT_CATEGORY:
			return "lcia_categories";
		case IMPACT_METHOD:
			return "lcia_methods";
		case LOCATION:
			return "locations";
		case NW_SET:
			return "nw_sets";
		case PRODUCT_SYSTEM:
			return "product_systems";
		case PROJECT:
			return "projects";
		case SOURCE:
			return "sources";
		case UNIT:
			return "units";
		case UNIT_GROUP:
			return "unit_groups";
		default:
			return "unknown";
		}
	}

}
