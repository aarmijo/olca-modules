package org.openlca.jsonld.output;

import java.lang.reflect.Type;
import java.util.Objects;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.ModelType;
import org.openlca.jsonld.EntityStore;

class FlowWriter implements Writer<Flow> {

	private EntityStore store;

	public FlowWriter() {
	}

	public FlowWriter(EntityStore store) {
		this.store = store;
	}

	@Override
	public void write(Flow flow) {
		if (flow == null || store == null)
			return;
		if (store.contains(ModelType.FLOW, flow.getRefId()))
			return;
		JsonObject obj = serialize(flow, null, null);
		store.put(ModelType.FLOW, obj);
	}

	@Override
	public JsonObject serialize(Flow flow, Type type,
			JsonSerializationContext context) {
		JsonObject obj = store == null ? new JsonObject() : store.initJson();
		map(flow, obj);
		return obj;
	}

	void map(Flow flow, JsonObject obj) {
		if (flow == null || obj == null)
			return;
		JsonExport.addAttributes(flow, obj, store);
		if (flow.getFlowType() != null)
			obj.addProperty("flowType", flow.getFlowType().name());
		obj.addProperty("cas", flow.getCasNumber());
		obj.addProperty("formula", flow.getFormula());
		JsonObject locationRef = Out.put(flow.getLocation(), store);
		obj.add("location", locationRef);
		addFactors(flow, obj);
	}

	private void addFactors(Flow flow, JsonObject obj) {
		JsonArray factorArray = new JsonArray();
		for (FlowPropertyFactor fac : flow.getFlowPropertyFactors()) {
			JsonObject facObj = new JsonObject();
			facObj.addProperty("@type", "FlowPropertyFactor");
			if(Objects.equals(fac, flow.getReferenceFactor()))
				facObj.addProperty("referenceFlowProperty", true);
			JsonObject propRef = Out.put(fac.getFlowProperty(), store);
			facObj.add("flowProperty", propRef);
			facObj.addProperty("conversionFactor", fac.getConversionFactor());
			factorArray.add(facObj);
		}
		obj.add("flowProperties", factorArray);
	}

}
