package org.openlca.io.ecospold2.output;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Unit;
import org.openlca.ecospold2.Exchange;
import org.openlca.ecospold2.UserMasterData;
import org.openlca.io.maps.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.supercsv.cellprocessor.ift.CellProcessor;

class UnitMap {

	private Logger log = LoggerFactory.getLogger(getClass());

	private final HashMap<String, ExportRecord> map = new HashMap<>();

	public UnitMap(IDatabase database) {
		initMap(database);
	}

	private void initMap(IDatabase database) {
		try {
			CellProcessor[] processors = { null, null, null, null };
			List<List<Object>> rows = Maps.readAll(Maps.ES2_UNIT_EXPORT,
					database, processors);
			for (List<Object> row : rows) {
				String refId = Maps.getString(row, 0);
				ExportRecord record = new ExportRecord();
				record.id = Maps.getString(row, 2);
				record.name = Maps.getString(row, 3);
				map.put(refId, record);
			}
		} catch (Exception e) {
			log.error("failed to initialize unit export map", e);
		}
	}

	public void apply(Unit unit, Exchange exchange, UserMasterData masterData) {
		if (unit == null || exchange == null) {
			log.warn("could not apply unit mapping: unit or exchange was null");
			return;
		}
		ExportRecord record = map.get(unit.getRefId());
		if (record != null) {
			exchange.setUnitName(record.name);
			exchange.setUnitId(record.id);
		} else {
			log.warn("unit {} is not a known unit in EcoSpold 2", unit);
			exchange.setUnitName(unit.getName());
			exchange.setUnitId(unit.getRefId());
			addMasterEntry(unit, masterData);
		}
	}

	private void addMasterEntry(Unit unit, UserMasterData masterData) {
		for (org.openlca.ecospold2.Unit es2Unit : masterData.getUnits()) {
			if (Objects.equals(unit.getRefId(), es2Unit.getId()))
				return;
		}
		org.openlca.ecospold2.Unit es2Unit = new org.openlca.ecospold2.Unit();
		es2Unit.setComment(unit.getDescription());
		es2Unit.setId(unit.getRefId());
		es2Unit.setName(unit.getName());
		masterData.getUnits().add(es2Unit);
	}

	private class ExportRecord {

		String id;
		String name;

		@Override
		public String toString() {
			return name + " [" + id + "]";
		}
	}

}
