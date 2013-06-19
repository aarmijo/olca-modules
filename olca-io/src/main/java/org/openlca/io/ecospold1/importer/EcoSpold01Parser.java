/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.io.ecospold1.importer;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Actor;
import org.openlca.core.model.AdminInfo;
import org.openlca.core.model.AllocationFactor;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.Category;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.LCIACategory;
import org.openlca.core.model.LCIAFactor;
import org.openlca.core.model.LCIAMethod;
import org.openlca.core.model.Location;
import org.openlca.core.model.ModelingAndValidation;
import org.openlca.core.model.Process;
import org.openlca.core.model.Source;
import org.openlca.core.model.Technology;
import org.openlca.core.model.Time;
import org.openlca.ecospold.IAllocation;
import org.openlca.ecospold.IDataSet;
import org.openlca.ecospold.IEcoSpold;
import org.openlca.ecospold.IExchange;
import org.openlca.ecospold.IPerson;
import org.openlca.ecospold.IReferenceFunction;
import org.openlca.ecospold.ISource;
import org.openlca.ecospold.io.DataSet;
import org.openlca.ecospold.io.EcoSpoldIO;
import org.openlca.ecospold.io.DataSetType;
import org.openlca.io.KeyGen;
import org.openlca.io.UnitMapping;
import org.openlca.io.maps.FlowMap;
import org.openlca.io.maps.MapType;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses EcoSpold01 xml files and creates openLCA objects and inserts them into
 * the database
 */
public class EcoSpold01Parser {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	private Category processCategory;
	private HashMap<Integer, Exchange> localExchangeCache = new HashMap<>();
	private DB db;
	private FlowImport flowImport;

	public EcoSpold01Parser(Category category, IDatabase iDatabase,
			UnitMapping unitMapping) {
		this.db = new DB(iDatabase);
		this.processCategory = category;
		FlowMap flowMap = new FlowMap(MapType.ECOSPOLD_FLOW);
		this.flowImport = new FlowImport(db, unitMapping, flowMap);
	}

	public void parse(File file, boolean process) throws Exception {
		try (InputStream is = new FileInputStream(file)) {
			parse(new FileInputStream(file), process);
		}
	}

	public void parse(ZipFile zipFile, ZipEntry entry, boolean process)
			throws Exception {
		if (!entry.isDirectory()
				&& entry.getName().toLowerCase().endsWith(".xml")) {
			try (InputStream is = zipFile.getInputStream(entry)) {
				parse(is, process);
			}
		}
	}

	public void parse(InputStream is, boolean process) throws Exception {
		DataSetType type = process ? DataSetType.PROCESS
				: DataSetType.IMPACT_METHOD;
		IEcoSpold spold = EcoSpoldIO.readFrom(is, type);
		if (process) {
			for (IDataSet ds : spold.getDataset()) {
				DataSet dataSet = new DataSet(ds, type.getFactory());
				parseProcessDataSet(dataSet);
			}
		} else if (!spold.getDataset().isEmpty()) {
			insertImpactMethod(spold);
		}
	}

	private void parseProcessDataSet(DataSet dataSet) throws Exception {
		if (dataSet.getReferenceFunction() == null)
			return;
		insertPersons(dataSet);
		insertSources(dataSet);
		insertLocations(dataSet);
		insertProcess(dataSet);
	}

	private void insertPersons(DataSet dataSet) {
		for (IPerson person : dataSet.getPersons()) {
			String genKey = ES1KeyGen.forPerson(person);
			if (db.findActor(person, genKey) != null)
				continue;
			Actor actor = new Actor();
			actor.setId(genKey);
			Mapper.mapPerson(person, actor);
			db.put(actor, genKey);
		}
	}

	private void insertSources(DataSet dataSet) {
		for (ISource eSource : dataSet.getSources()) {
			String sourceId = ES1KeyGen.forSource(eSource);
			if (db.findSource(eSource, sourceId) != null)
				continue;
			Source oSource = new Source();
			oSource.setId(sourceId);
			Mapper.mapSource(eSource, oSource);
			db.put(oSource, sourceId);
		}
	}

	private void insertLocations(DataSet dataSet) {
		if (dataSet.getGeography() != null)
			insertLocation(dataSet.getGeography().getLocation());
		for (IExchange exchange : dataSet.getExchanges())
			insertLocation(exchange.getLocation());
	}

	private void insertLocation(String locationCode) {
		if (locationCode == null)
			return;
		String genKey = KeyGen.get(locationCode);
		Location location = db.findLocation(locationCode, genKey);
		if (location == null) {
			location = new Location();
			location.setId(genKey);
			location.setName(locationCode);
			location.setCode(locationCode);
			db.put(location, genKey);
		}
	}

	private void insertProcess(DataSet dataSet) throws Exception {
		String processId = ES1KeyGen.forProcess(dataSet);
		Process process = db.get(Process.class, processId);
		if (process != null) {
			log.trace("Process {} already exists, not imported", processId);
			return;
		}
		process = new Process();
		process.setId(processId);
		if (dataSet.getReferenceFunction() != null) {
			process.setDescription(dataSet.getReferenceFunction()
					.getGeneralComment());
			process.setInfrastructureProcess(dataSet.getReferenceFunction()
					.isInfrastructureProcess());
		}
		process.setProcessType(Mapper.getProcessType(dataSet));

		Technology technology = new Technology(process);
		if (dataSet.getTechnology() != null
				&& dataSet.getTechnology().getText() != null)
			technology.setDescription(Strings.cut(
					(dataSet.getTechnology().getText()), 65500));

		Time time = mapTimeAndGeo(dataSet, process);

		mapExchanges(dataSet.getExchanges(), process);
		if (process.getQuantitativeReference() == null)
			createProductFromRefFun(dataSet, process);

		if (dataSet.getReferenceFunction() != null)
			mapReferenceFunction(dataSet, process);

		if (dataSet.getAllocations() != null
				&& dataSet.getAllocations().size() > 0) {
			mapAllocations(dataSet.getAllocations());
			process.setAllocationMethod(AllocationMethod.Causal);
		}

		ModelingAndValidation modelingAndValidation = new ModelingAndValidation(
				process);
		Mapper.mapModellingAndValidation(dataSet, modelingAndValidation);
		AdminInfo adminInfo = new AdminInfo(process);
		Mapper.mapAdminInfo(dataSet, adminInfo);
		mapActors(modelingAndValidation, adminInfo, dataSet);
		mapSources(modelingAndValidation, adminInfo, dataSet);

		db.put(process, processId);
		db.put(modelingAndValidation, processId);
		db.put(adminInfo, processId);
		db.put(time, processId);
		db.put(technology, processId);

		localExchangeCache.clear();
	}

	private Time mapTimeAndGeo(DataSet dataSet, Process process) {
		ProcessTime processTime = new ProcessTime(process,
				dataSet.getTimePeriod());
		Time time = processTime.map();
		if (dataSet.getGeography() != null) {
			String locationCode = dataSet.getGeography().getLocation();
			if (locationCode != null) {
				String genKey = KeyGen.get(locationCode);
				process.setLocation(db.findLocation(locationCode, genKey));
			}
			process.setGeographyComment(dataSet.getGeography().getText());
		}
		return time;
	}

	private void mapActors(ModelingAndValidation modelingAndValidation,
			AdminInfo adminInfo, DataSet dataSet) {
		Map<Integer, Actor> actors = new HashMap<>();
		for (IPerson person : dataSet.getPersons()) {
			Actor actor = db.findActor(person, ES1KeyGen.forPerson(person));
			if (actor != null)
				actors.put(person.getNumber(), actor);
		}
		if (dataSet.getDataGeneratorAndPublication() != null)
			adminInfo.setDataGenerator(actors.get(dataSet
					.getDataGeneratorAndPublication().getPerson()));
		if (dataSet.getValidation() != null)
			modelingAndValidation.setReviewer(actors.get(dataSet
					.getValidation().getProofReadingValidator()));
		if (dataSet.getDataEntryBy() != null)
			adminInfo.setDataDocumentor(actors.get(dataSet.getDataEntryBy()
					.getPerson()));
	}

	private void mapAllocations(List<IAllocation> allocations) {
		for (IAllocation allocation : allocations) {
			double factor = Math.round(allocation.getFraction() * 10000d) / 1000000d;
			Exchange product = localExchangeCache.get(allocation
					.getReferenceToCoProduct());
			for (Integer i : allocation.getReferenceToInputOutput()) {
				Exchange exchange = localExchangeCache.get(i);
				AllocationFactor allocationFactor = new AllocationFactor(UUID
						.randomUUID().toString(), product.getId(), factor);
				exchange.add(allocationFactor);
			}
		}
	}

	private void mapExchanges(List<IExchange> inExchanges, Process ioProcess)
			throws Exception {
		for (IExchange inExchange : inExchanges) {
			FlowBucket flow = flowImport.handleProcessExchange(inExchange);
			if (flow == null || !flow.isValid()) {
				log.error("Could not import flow {}", inExchange);
				continue;
			}
			Exchange outExchange = new Exchange(ioProcess.getId());
			outExchange.setFlow(flow.flow);
			outExchange.setId(UUID.randomUUID().toString());
			outExchange.setUnit(flow.unit);
			outExchange.setFlowPropertyFactor(flow.flowProperty);
			outExchange.setInput(inExchange.getInputGroup() != null);
			ExchangeAmount exchangeAmount = new ExchangeAmount(outExchange,
					inExchange);
			exchangeAmount.map(flow.conversionFactor);
			ioProcess.add(outExchange);
			localExchangeCache.put(inExchange.getNumber(), outExchange);
			if (ioProcess.getQuantitativeReference() == null
					&& inExchange.getOutputGroup() != null
					&& (inExchange.getOutputGroup() == 0 || inExchange
							.getOutputGroup() == 2)) {
				ioProcess.setQuantitativeReference(outExchange);
			}
		}
	}

	private void mapFactors(List<IExchange> inFactors, LCIACategory ioCategory)
			throws Exception {
		for (IExchange inFactor : inFactors) {
			FlowBucket flow = flowImport.handleImpactFactor(inFactor);
			if (flow == null || !flow.isValid()) {
				log.error("Could not import flow {}", inFactor);
				continue;
			}
			LCIAFactor factor = new LCIAFactor();
			factor.setId(UUID.randomUUID().toString());
			factor.setFlow(flow.flow);
			factor.setFlowPropertyFactor(flow.flowProperty);
			factor.setUnit(flow.unit);
			factor.setValue(flow.conversionFactor * inFactor.getMeanValue());
			ioCategory.add(factor);
		}
	}

	private LCIACategory mapReferenceFunction(IReferenceFunction inRefFunction) {
		LCIACategory category = new LCIACategory();
		category.setId(UUID.randomUUID().toString());
		String name = inRefFunction.getSubCategory();
		if (inRefFunction.getName() != null) {
			name = name.concat(" - ").concat(inRefFunction.getName());
		}
		category.setName(name);
		category.setReferenceUnit(inRefFunction.getUnit());
		return category;
	}

	private void mapReferenceFunction(DataSet dataSet, Process ioProcess) {
		if (dataSet.getReferenceFunction() == null)
			return;
		IReferenceFunction inRefFunction = dataSet.getReferenceFunction();
		ioProcess.setName(inRefFunction.getName());
		ioProcess.setDescription(inRefFunction.getGeneralComment());
		ioProcess.setInfrastructureProcess(inRefFunction
				.isInfrastructureProcess());
		String topCategoryName = inRefFunction.getCategory();
		String subCategoryName = inRefFunction.getSubCategory();
		Category category = db.getPutCategory(processCategory, topCategoryName,
				subCategoryName);
		ioProcess.setCategoryId(category.getId());

	}

	private void createProductFromRefFun(DataSet dataSet, Process ioProcess) {
		FlowBucket flow = flowImport.handleProcessProduct(dataSet);
		if (flow == null || !flow.isValid()) {
			log.warn("Could not create reference flow {}", dataSet);
			return;
		}
		Exchange outExchange = new Exchange(ioProcess.getId());
		outExchange.setFlow(flow.flow);
		outExchange.setId(UUID.randomUUID().toString());
		outExchange.setUnit(flow.unit);
		outExchange.setFlowPropertyFactor(flow.flowProperty);
		outExchange.setInput(false);
		double amount = dataSet.getReferenceFunction().getAmount()
				* flow.conversionFactor;
		outExchange.getResultingAmount().setFormula(Double.toString(amount));
		outExchange.getResultingAmount().setValue(amount);
		ioProcess.add(outExchange);
		ioProcess.setQuantitativeReference(outExchange);
	}

	private void mapSources(ModelingAndValidation modelingAndValidation,
			AdminInfo adminInfo, DataSet adapter) {
		Map<Integer, Source> sources = new HashMap<>();
		for (ISource source : adapter.getSources()) {
			Source s = db.findSource(source, ES1KeyGen.forSource(source));
			if (s != null) {
				sources.put(source.getNumber(), s);
				modelingAndValidation.add(s);
			}
		}
		if (adapter.getDataGeneratorAndPublication() != null
				&& adapter.getDataGeneratorAndPublication()
						.getReferenceToPublishedSource() != null)
			adminInfo.setPublication(sources.get(adapter
					.getDataGeneratorAndPublication()
					.getReferenceToPublishedSource()));
	}

	private void insertImpactMethod(IEcoSpold es) throws Exception {
		if (es.getDataset().isEmpty())
			return;
		DataSet dataSet = new DataSet(es.getDataset().get(0),
				DataSetType.IMPACT_METHOD.getFactory());
		String methodId = ES1KeyGen.forImpactMethod(dataSet);
		LCIAMethod method = db.get(LCIAMethod.class, methodId);
		if (method != null) {
			log.trace("LCIA method {} already exists, not imported", methodId);
			return;
		}

		method = new LCIAMethod(methodId, dataSet.getReferenceFunction()
				.getCategory());
		Category category = db.getPutCategory(LCIAMethod.class, null, null);
		method.setCategoryId(category.getId());
		method.setDescription(dataSet.getReferenceFunction()
				.getGeneralComment());
		for (IDataSet adapter : es.getDataset()) {
			dataSet = new DataSet(adapter,
					DataSetType.IMPACT_METHOD.getFactory());
			LCIACategory lciaCategory = mapReferenceFunction(dataSet
					.getReferenceFunction());
			mapFactors(dataSet.getExchanges(), lciaCategory);
			method.add(lciaCategory);
		}
		db.put(method, methodId);
	}

}