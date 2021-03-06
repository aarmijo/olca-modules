package org.openlca.io.olca;

import org.openlca.core.database.ActorDao;
import org.openlca.core.database.CategoryDao;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.FlowPropertyDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ImpactMethodDao;
import org.openlca.core.database.LocationDao;
import org.openlca.core.database.NwSetDao;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.database.ProductSystemDao;
import org.openlca.core.database.ProjectDao;
import org.openlca.core.database.RootEntityDao;
import org.openlca.core.database.SourceDao;
import org.openlca.core.database.UnitGroupDao;
import org.openlca.core.model.Unit;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;

/**
 * Stores the mappings between reference IDs (UUID) and generated IDs (long) of
 * (imported) data sets. An instance of this class is created with the target
 * database in the import and initialized with the IDs of the data sets that are
 * already contained in this database.
 */
class Sequence {

	private Logger log = LoggerFactory.getLogger(getClass());

	int CATEGORY = 0;
	int LOCATION = 1;
	int ACTOR = 2;
	int SOURCE = 3;
	int UNIT = 4;
	int UNIT_GROUP = 5;
	int FLOW_PROPERTY = 6;
	int FLOW = 7;
	int PROCESS = 8;
	int PRODUCT_SYSTEM = 9;
	int IMPACT_METHOD = 10;
	int NW_SET = 11;
	int PROJECT = 12;

	private final HashMap<String, Long>[] sequences;

	@SuppressWarnings("unchecked")
	public Sequence(IDatabase database) {
		sequences = new HashMap[13];
		for (int i = 0; i < sequences.length; i++)
			sequences[i] = new HashMap<>();
		init(database);
	}

	private void init(IDatabase database) {
		index(CATEGORY, new CategoryDao(database));
		index(LOCATION, new LocationDao(database));
		index(ACTOR, new ActorDao(database));
		index(SOURCE, new SourceDao(database));
		index(UNIT, new RootEntityDao<>(Unit.class, BaseDescriptor.class,
				database));
		index(UNIT_GROUP, new UnitGroupDao(database));
		index(FLOW_PROPERTY, new FlowPropertyDao(database));
		index(FLOW, new FlowDao(database));
		index(PROCESS, new ProcessDao(database));
		index(PRODUCT_SYSTEM, new ProductSystemDao(database));
		index(IMPACT_METHOD, new ImpactMethodDao(database));
		index(NW_SET, new NwSetDao(database));
		index(PROJECT, new ProjectDao(database));
	}

	private void index(int type, RootEntityDao<?, ?> dao) {
		List<? extends BaseDescriptor> descriptors = dao.getDescriptors();
		for (BaseDescriptor descriptor : descriptors) {
			if (descriptor.getRefId() == null) {
				log.warn("found root entity without reference ID: {}",
						descriptor);
				continue;
			}
			put(type, descriptor.getRefId(), descriptor.getId());
		}
	}

	public void put(int type, String refId, long genId) {
		if (refId == null)
			return;
		sequences[type].put(refId, genId);
	}

	public long get(int type, String refId) {
		if (refId == null)
			return 0;
		Long val = sequences[type].get(refId);
		return val == null ? 0 : val;
	}

	public boolean contains(int type, String refId) {
		return get(type, refId) != 0;
	}
}
