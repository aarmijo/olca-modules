package org.openlca.jsonld.input;

import com.google.gson.JsonObject;
import org.openlca.core.model.Actor;
import org.openlca.core.model.ModelType;
import org.openlca.jsonld.EntityStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ActorImport {
	
	private Logger log = LoggerFactory.getLogger(getClass());
	private String refId;
	private EntityStore store;
	private Db db;
	
	private ActorImport(String refId, EntityStore store, Db db) {
		this.refId = refId;
		this.store = store;
		this.db = db;
	}
	
	static Actor run(String refId, EntityStore store, Db db) {
		return new ActorImport(refId, store, db).run();
	}
	
	private Actor run() {
		if (refId == null || store == null || db == null)
			return null;
		try {
			Actor a = db.getActor(refId);
			if (a != null)
				return a;
			JsonObject json = store.get(ModelType.ACTOR, refId);
			return map(json);
		} catch (Exception e) {
			log.error("failed to import actor " + refId, e);
			return null;
		}
	}
	
	private Actor map(JsonObject json) {
		if (json == null)
			return null;
		Actor a = new Actor();
		In.mapAtts(json, a);
		String catId = In.getRefId(json, "category");
		a.setCategory(CategoryImport.run(catId, store, db));		
		mapAtts(json, a);
		return db.put(a);
	}
	
	private void mapAtts(JsonObject json, Actor a) {
		a.setAddress(In.getString(json, "address"));
		a.setCity(In.getString(json, "city"));
		a.setCountry(In.getString(json, "country"));
		a.setEmail(In.getString(json, "email"));
		a.setTelefax(In.getString(json, "telefax"));
		a.setTelephone(In.getString(json, "telephone"));
		a.setWebsite(In.getString(json, "website"));
		a.setZipCode(In.getString(json, "zipCode"));
	}	
}

