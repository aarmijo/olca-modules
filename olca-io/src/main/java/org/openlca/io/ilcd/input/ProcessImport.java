package org.openlca.io.ilcd.input;

import java.util.Date;
import java.util.List;

import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.model.Actor;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.Category;
import org.openlca.core.model.Location;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessDocumentation;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.Source;
import org.openlca.core.model.Version;
import org.openlca.ilcd.commons.CommissionerAndGoal;
import org.openlca.ilcd.commons.DataSetReference;
import org.openlca.ilcd.commons.LCIMethodApproach;
import org.openlca.ilcd.io.DataStore;
import org.openlca.ilcd.processes.DataEntry;
import org.openlca.ilcd.processes.Geography;
import org.openlca.ilcd.processes.LCIMethod;
import org.openlca.ilcd.processes.Publication;
import org.openlca.ilcd.processes.Representativeness;
import org.openlca.ilcd.processes.Review;
import org.openlca.ilcd.util.LangString;
import org.openlca.ilcd.util.ProcessBag;
import org.openlca.io.maps.FlowMap;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessImport {

	private Logger log = LoggerFactory.getLogger(getClass());
	private IDatabase database;
	private DataStore dataStore;
	private ProcessBag ilcdProcess;
	private Process process;
	private FlowMap flowMap;

	public ProcessImport(DataStore dataStore, IDatabase database) {
		this.database = database;
		this.dataStore = dataStore;
	}

	public void setFlowMap(FlowMap flowMap) {
		this.flowMap = flowMap;
	}

	public Process run(org.openlca.ilcd.processes.Process process)
			throws ImportException {
		this.ilcdProcess = new ProcessBag(process);
		Process oProcess = findExisting(ilcdProcess.getId());
		if (oProcess != null)
			return oProcess;
		return createNew();
	}

	public Process run(String processId) throws ImportException {
		Process process = findExisting(processId);
		if (process != null)
			return process;
		org.openlca.ilcd.processes.Process iProcess = tryGetProcess(processId);
		ilcdProcess = new ProcessBag(iProcess);
		return createNew();
	}

	private Process findExisting(String processId) throws ImportException {
		try {
			ProcessDao dao = new ProcessDao(database);
			return dao.getForRefId(processId);
		} catch (Exception e) {
			String message = String.format("Search for process %s failed.",
					processId);
			throw new ImportException(message, e);
		}
	}

	private Process createNew() throws ImportException {
		try {
			process = new Process();
			importAndSetCategory();
			createAndMapContent();
			saveInDatabase(process);
			return process;
		} catch (Exception e) {
			throw new ImportException(e);
		}
	}

	private org.openlca.ilcd.processes.Process tryGetProcess(String processId)
			throws ImportException {
		try {
			org.openlca.ilcd.processes.Process iProcess = dataStore.get(
					org.openlca.ilcd.processes.Process.class, processId);
			if (iProcess == null) {
				throw new ImportException("No ILCD process for ID " + processId
						+ " found");
			}
			return iProcess;
		} catch (Exception e) {
			throw new ImportException(e.getMessage(), e);
		}
	}

	private void importAndSetCategory() throws ImportException {
		CategoryImport categoryImport = new CategoryImport(database,
				ModelType.PROCESS);
		Category category = categoryImport.run(ilcdProcess.getSortedClasses());
		process.setCategory(category);
	}

	private void createAndMapContent() throws ImportException {
		process.setRefId(ilcdProcess.getId());
		process.setName(Strings.cut(ilcdProcess.getName(), 254));
		process.setDescription(ilcdProcess.getComment());
		ProcessDocumentation doc = mapDocumentation();
		process.setDocumentation(doc);
		ProcessParameterConversion paramConv = new ProcessParameterConversion(
				process, database);
		paramConv.run(ilcdProcess);
		ProcessExchanges.mapFrom(dataStore, ilcdProcess).withFlowMap(flowMap)
				.to(database, process);
	}

	private ProcessDocumentation mapDocumentation() throws ImportException {
		ProcessDocumentation doc = new ProcessDocumentation();
		ProcessTime processTime = new ProcessTime(ilcdProcess.getTime());
		processTime.map(doc);
		mapGeography(doc);
		mapTechnology(doc);
		mapPublication(doc);
		mapDataEntry(doc);
		mapDataGenerator(doc);
		mapComissionerAndGoal(doc);
		mapLciMethod(doc);
		mapRepresentativeness(doc);
		mapReviews(doc);
		return doc;
	}

	private void mapGeography(ProcessDocumentation doc) throws ImportException {
		Geography iGeography = ilcdProcess.getGeography();
		if (iGeography == null || iGeography.getLocation() == null)
			return;
		doc.setGeography(LangString.get(iGeography.getLocation()
				.getDescription()));
		if (iGeography.getLocation().getLocation() == null)
			return;
		String code = iGeography.getLocation().getLocation();
		Location location = Locations.getOrCreate(code, database);
		process.setLocation(location);
	}

	private void mapTechnology(ProcessDocumentation doc) {
		org.openlca.ilcd.processes.Technology iTechnology = ilcdProcess
				.getTechnology();
		if (iTechnology != null) {
			doc.setTechnology(LangString.get(iTechnology
					.getTechnologyDescriptionAndIncludedProcesses()));
		}
	}

	private void mapPublication(ProcessDocumentation doc) {
		Publication iPublication = ilcdProcess.getPublication();
		if (iPublication != null) {

			// data set owner
			DataSetReference ownerRef = iPublication
					.getReferenceToOwnershipOfDataSet();
			if (ownerRef != null)
				doc.setDataSetOwner(fetchActor(ownerRef));

			// publication
			DataSetReference publicationRef = iPublication
					.getReferenceToUnchangedRepublication();
			if (publicationRef != null)
				doc.setPublication(fetchSource(publicationRef));

			// access and use restrictions
			doc.setRestrictions(LangString.get(iPublication
					.getAccessRestrictions()));

			// version
			process.setVersion(Version.fromString(
					iPublication.getDataSetVersion()).getValue());

			// copyright
			if (iPublication.isCopyright() != null) {
				doc.setCopyright(iPublication.isCopyright());
			}

		}
	}

	private void mapDataEntry(ProcessDocumentation doc) {
		DataEntry iEntry = ilcdProcess.getDataEntry();
		if (iEntry == null)
			return;
		if (iEntry.getTimeStamp() != null) {
			Date tStamp = iEntry.getTimeStamp().toGregorianCalendar().getTime();
			doc.setCreationDate(tStamp);
			if (tStamp != null)
				process.setLastChange(tStamp.getTime());
		}
		if (iEntry.getReferenceToPersonOrEntityEnteringTheData() != null) {
			Actor documentor = fetchActor(iEntry
					.getReferenceToPersonOrEntityEnteringTheData());
			doc.setDataDocumentor(documentor);
		}
	}

	private void mapDataGenerator(ProcessDocumentation doc) {
		if (ilcdProcess.getDataGenerator() != null) {
			List<DataSetReference> refs = ilcdProcess.getDataGenerator()
					.getReferenceToPersonOrEntityGeneratingTheDataSet();
			if (refs != null && !refs.isEmpty()) {
				DataSetReference generatorRef = refs.get(0);
				doc.setDataGenerator(fetchActor(generatorRef));
			}
		}
	}

	private void mapComissionerAndGoal(ProcessDocumentation doc) {
		if (ilcdProcess.getCommissionerAndGoal() != null) {
			CommissionerAndGoal comAndGoal = ilcdProcess
					.getCommissionerAndGoal();
			String intendedApp = LangString.get(comAndGoal
					.getIntendedApplications());
			doc.setIntendedApplication(intendedApp);
			String project = LangString.get(comAndGoal.getProject());
			doc.setProject(project);
		}
	}

	private void mapLciMethod(ProcessDocumentation doc) {

		if (ilcdProcess.getProcessType() != null) {
			switch (ilcdProcess.getProcessType()) {
			case UNIT_PROCESS_BLACK_BOX:
				process.setProcessType(ProcessType.UNIT_PROCESS);
				break;
			case UNIT_PROCESS_SINGLE_OPERATION:
				process.setProcessType(ProcessType.UNIT_PROCESS);
				break;
			default:
				process.setProcessType(ProcessType.LCI_RESULT);
				break;
			}
		}

		LCIMethod iMethod = ilcdProcess.getLciMethod();
		if (iMethod != null) {
			String lciPrinciple = LangString.get(iMethod
					.getDeviationsFromLCIMethodPrinciple());
			doc.setInventoryMethod(lciPrinciple);
			doc.setModelingConstants(LangString.get(iMethod
					.getModellingConstants()));
			process.setDefaultAllocationMethod(getAllocation(iMethod));
		}

	}

	private AllocationMethod getAllocation(LCIMethod iMethod) {
		List<LCIMethodApproach> approaches = iMethod.getLCIMethodApproaches();
		if (approaches == null || approaches.isEmpty())
			return null;
		for (LCIMethodApproach app : approaches) {
			switch (app) {
			case ALLOCATION_OTHER_EXPLICIT_ASSIGNMENT:
				return AllocationMethod.CAUSAL;
			case ALLOCATION_MARKET_VALUE:
				return AllocationMethod.ECONOMIC;
			case ALLOCATION_PHYSICAL_CAUSALITY:
				return AllocationMethod.PHYSICAL;
			default:
				continue;
			}
		}
		return null;
	}

	private void mapRepresentativeness(ProcessDocumentation doc) {
		Representativeness repr = ilcdProcess.getRepresentativeness();
		if (repr != null) {
			doc.setCompleteness(LangString.get(repr
					.getDataCutOffAndCompletenessPrinciples()));
			doc.setDataSelection(LangString.get(repr
					.getDataSelectionAndCombinationPrinciples()));
			doc.setDataTreatment(LangString.get(repr
					.getDataTreatmentAndExtrapolationsPrinciples()));
			doc.setSampling(LangString.get(repr
					.getSamplingProcedure()));
			doc.setDataCollectionPeriod(LangString.get(repr
					.getDataCollectionPeriod()));
			addSources(doc, repr);
		}
	}

	private void addSources(ProcessDocumentation doc, Representativeness repr) {
		for (DataSetReference sourceRef : repr.getReferenceToDataSource()) {
			Source source = fetchSource(sourceRef);
			if (source == null || doc.getSources().contains(source))
				continue;
			doc.getSources().add(source);
		}
	}

	private void mapReviews(ProcessDocumentation doc) {
		if (!ilcdProcess.getReviews().isEmpty()) {
			Review iReview = ilcdProcess.getReviews().get(0);
			if (!iReview.getReferenceToNameOfReviewerAndInstitution().isEmpty()) {
				DataSetReference ref = iReview
						.getReferenceToNameOfReviewerAndInstitution().get(0);
				doc.setReviewer(fetchActor(ref));
			}
			doc.setReviewDetails(LangString.get(iReview.getReviewDetails()));
		}
	}

	private Actor fetchActor(DataSetReference reference) {
		if (reference == null)
			return null;
		try {
			ContactImport contactImport = new ContactImport(dataStore, database);
			return contactImport.run(reference.getUuid());
		} catch (Exception e) {
			log.warn("Failed to get contact {} referenced from process {}",
					reference.getUuid(), process.getRefId());
			return null;
		}
	}

	private Source fetchSource(DataSetReference reference) {
		if (reference == null)
			return null;
		try {
			SourceImport sourceImport = new SourceImport(dataStore, database);
			return sourceImport.run(reference.getUuid());
		} catch (Exception e) {
			log.warn("Failed to get source {} referenced from process {}",
					reference.getUuid(), process.getRefId());
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private <T> void saveInDatabase(T obj) throws ImportException {
		try {
			Class<T> clazz = (Class<T>) obj.getClass();
			database.createDao(clazz).insert(obj);
		} catch (Exception e) {
			String message = String.format(
					"Save operation failed in process %s.", process.getRefId());
			throw new ImportException(message, e);
		}
	}

}
