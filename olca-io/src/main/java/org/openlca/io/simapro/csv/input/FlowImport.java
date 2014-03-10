package org.openlca.io.simapro.csv.input;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.openlca.core.database.FlowDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.AllocationFactor;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.Category;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.PedigreeMatrix;
import org.openlca.core.model.PedigreeMatrixRow;
import org.openlca.core.model.Process;
import org.openlca.core.model.Uncertainty;
import org.openlca.core.model.UncertaintyType;
import org.openlca.core.model.Unit;
import org.openlca.expressions.FormulaInterpreter;
import org.openlca.expressions.InterpreterException;
import org.openlca.io.Categories;
import org.openlca.io.UnitMapping;
import org.openlca.io.maps.ImportMap;
import org.openlca.io.maps.content.CSVElementaryCategoryContent;
import org.openlca.io.maps.content.SPElementaryFlowContent;
import org.openlca.io.maps.content.CSVProductFlowContent;
import org.openlca.simapro.csv.model.IDistribution;
import org.openlca.simapro.csv.model.SPDataSet;
import org.openlca.simapro.csv.model.SPLogNormalDistribution;
import org.openlca.simapro.csv.model.SPPedigreeMatrix;
import org.openlca.simapro.csv.model.SPProcess;
import org.openlca.simapro.csv.model.SPWasteTreatment;
import org.openlca.simapro.csv.model.enums.DistributionParameter;
import org.openlca.simapro.csv.model.enums.ElementaryFlowType;
import org.openlca.simapro.csv.model.enums.ProcessCategory;
import org.openlca.simapro.csv.model.enums.ProductType;
import org.openlca.simapro.csv.model.enums.SubCompartment;
import org.openlca.simapro.csv.model.process.ElementaryExchangeRow;
import org.openlca.simapro.csv.model.process.ProductExchangeRow;
import org.openlca.simapro.csv.model.process.ProductOutputRow;
import org.openlca.simapro.csv.model.process.WasteTreatmentRow;
import org.openlca.simapro.csv.model.refdata.ElementaryFlowRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowImport {

	private Logger log = LoggerFactory.getLogger(getClass());
	private IDatabase database;
	private SPDataSet dataEntry;
	private Process process;
	private CSVImportCache cache;
	private FlowDao flowDao;
	private UnitMapping unitMapping;
	private FormulaInterpreter interpreter;
	private long scopeId;
	private ImportMap<SPElementaryFlowContent> elemFlowMap;
	private ImportMap<CSVProductFlowContent> productFlowMap;
	private ImportMap<CSVElementaryCategoryContent> categoryMap;

	FlowImport(IDatabase database, CSVImportCache cache,
			FormulaInterpreter interpreter, long scopeId,
			ImportMap<SPElementaryFlowContent> elemFlowMap,
			ImportMap<CSVProductFlowContent> productFlowMap,
			ImportMap<CSVElementaryCategoryContent> categoryMap) {
		this.cache = cache;
		this.database = database;
		this.interpreter = interpreter;
		this.scopeId = scopeId;
		flowDao = new FlowDao(database);
		unitMapping = UnitMapping.createDefault(database);
		this.elemFlowMap = elemFlowMap;
		this.productFlowMap = productFlowMap;
		this.categoryMap = categoryMap;
	}

	void importFlows(Process process, SPDataSet dataEntry)
			throws InterpreterException {
		this.process = process;
		this.dataEntry = dataEntry;
		elementaryExchanges();
		productExchanges();
		if (dataEntry instanceof SPProcess)
			processRefs((SPProcess) dataEntry);
		else if (dataEntry instanceof SPWasteTreatment)
			wasteRef((SPWasteTreatment) dataEntry);
		calculateExchangesAmount();
	}

	private void elementaryExchanges() {
		for (ElementaryExchangeRow elementaryFlow : dataEntry.getElementaryFlows()) {
			Flow flow = findOrCreate(elementaryFlow);
			Exchange exchange = new Exchange();
			process.getExchanges().add(exchange);
			exchange.setFlow(flow);
			exchange.setAvoidedProduct(false);
			exchange.setUnit(unitMapping.getEntry(elementaryFlow.getUnit())
					.getUnit());
			setFlowPropertyFactor(exchange, elementaryFlow.getUnit());

			if (elementaryFlow.getType() == ElementaryFlowType.RESOURCE)
				exchange.setInput(true);
			else
				exchange.setInput(false);
			setAmount(exchange, elementaryFlow.getAmount());
			setUncertainty(exchange, elementaryFlow.getUncertainty(),
					elementaryFlow.getAmount());
		}
	}

	private void productExchanges() {
		for (ProductExchangeRow productFlow : dataEntry.getProductFlows()) {
			Flow flow = findOrCreate(productFlow);
			Exchange exchange = new Exchange();
			process.getExchanges().add(exchange);
			exchange.setFlow(flow);
			setFlowPropertyFactor(exchange, productFlow.getUnit());
			exchange.setUnit(unitMapping.getEntry(productFlow.getUnit())
					.getUnit());
			if (productFlow.getType() == ProductType.AVOIDED_PRODUCT)
				exchange.setAvoidedProduct(true);
			if (productFlow.getType() == ProductType.WASTE_TREATMENT)
				exchange.setInput(true);
			else
				exchange.setInput(false);
			setAmount(exchange, productFlow.getAmount());
			setUncertainty(exchange, productFlow.getUncertainty(),
					productFlow.getAmount());
		}
	}

	private void processRefs(SPProcess spProcess) {
		ProductOutputRow refProduct = spProcess.getReferenceProduct();
		Exchange exchange = new Exchange();
		process.getExchanges().add(exchange);
		process.setQuantitativeReference(exchange);
		exchange.setFlow(findOrCreate(refProduct));
		exchange.setInput(false);
		exchange.setUnit(unitMapping.getEntry(refProduct.getUnit()).getUnit());
		setFlowPropertyFactor(exchange, refProduct.getUnit());
		setAmount(exchange, refProduct.getAmount());
		if (spProcess.getByProducts().length > 0) {
			process.setDefaultAllocationMethod(AllocationMethod.CAUSAL);
			createAllocation(exchange, refProduct.getAllocation());
			byProducts(spProcess.getByProducts());
		}
	}

	private void byProducts(ProductOutputRow[] byProducts) {
		for (ProductOutputRow product : byProducts) {
			Exchange exchange = new Exchange();
			process.getExchanges().add(exchange);
			exchange.setFlow(findOrCreate(product));
			setAmount(exchange, product.getAmount());
			setFlowPropertyFactor(exchange, product.getUnit());
			exchange.setInput(false);
			exchange.setUnit(unitMapping.getEntry(product.getUnit()).getUnit());
			createAllocation(exchange, product.getAllocation());
		}

	}

	private void createAllocation(Exchange product, Double allocation) {
		for (Exchange exchange : process.getExchanges()) {
			if (exchange.getFlow().getFlowType() == FlowType.PRODUCT_FLOW)
				continue;
			if (exchange.getFlow().getFlowType() == FlowType.WASTE_FLOW)
				continue;
			AllocationFactor factor = new AllocationFactor();
			process.getAllocationFactors().add(factor);
			factor.setAllocationType(AllocationMethod.CAUSAL);
			factor.setExchange(exchange);
			// TODO hier soll ein exchange rein. mit Michael besprechen °!°
			factor.setProductId(0);
			factor.setValue(allocation);
		}

	}

	private void wasteRef(SPWasteTreatment wasteTreatment) {
		WasteTreatmentRow wasteSpecification = wasteTreatment
				.getWasteSpecification();
		Exchange exchange = new Exchange();
		process.getExchanges().add(exchange);
		process.setQuantitativeReference(exchange);
		exchange.setFlow(findOrCreate(wasteSpecification));
		setFlowPropertyFactor(exchange, wasteSpecification.getUnit());
		exchange.setUnit(unitMapping.getEntry(wasteSpecification.getUnit())
				.getUnit());
		setAmount(exchange, wasteSpecification.getAmount());
		exchange.setInput(false);
	}

	private void calculateExchangesAmount() throws InterpreterException {
		for (Exchange exchange : process.getExchanges())
			if (Utils.nullCheck(exchange.getAmountFormula()))
				exchange.setAmountValue(interpreter.getScope(scopeId).eval(
						exchange.getAmountFormula()));
	}

	private void setAmount(Exchange exchange, String amount) {
		if (Utils.isNumeric(amount))
			exchange.setAmountValue(Double.parseDouble(amount.replace(",", ".")));
		else
			exchange.setAmountFormula(amount);
	}

	private void setDefaultFlowProperty(Flow flow, String unit) {
		FlowProperty flowProperty = unitMapping.getFlowProperty(unit);
		FlowPropertyFactor flowPropertyFactor = new FlowPropertyFactor();
		flowPropertyFactor.setFlowProperty(flowProperty);
		// TODO: check if conversionFactor 1 is right!
		flowPropertyFactor.setConversionFactor(1);
		flow.setReferenceFlowProperty(flowProperty);
		flow.getFlowPropertyFactors().add(flowPropertyFactor);
	}

	private String getFlowFormula(String comment) {
		switch (comment.toLowerCase()) {
		case "formula:":
		case " formula:":
		case "\"formula:":
			String formula[] = comment.split("\n");
			if (formula[0] != null && formula[0].equals(""))
				return formula[0].substring(formula[0].indexOf(":") + 1);
		}
		return null;
	}

	private Category getElementaryFlowCategory(ElementaryExchangeRow elementaryFlow) {
		String compartment = mapCompartmentCategory(elementaryFlow.getType());
		String subCompartment = mapSubcompartmentCategory(elementaryFlow
				.getSubCompartment());
		Category rootCategory = Categories.findOrCreateRoot(database,
				ModelType.FLOW, "Elementary flows");
		Category parentCategory = Categories.findOrAddChild(database,
				rootCategory, compartment);
		return Categories.findOrAddChild(database, parentCategory,
				subCompartment);
	}

	private void setFlowPropertyFactor(Exchange exchange, String unit) {
		for (FlowPropertyFactor factor : exchange.getFlow()
				.getFlowPropertyFactors())
			for (Unit u : factor.getFlowProperty().getUnitGroup().getUnits())
				if (u.getName().equals(unit))
					exchange.setFlowPropertyFactor(factor);
	}

	private void setUncertainty(Exchange exchange, IDistribution distribution,
			String amount) {
		if (!Utils.isNumeric(amount))
			return;
		exchange.setUncertainty(convertUncertainty(distribution,
				Double.parseDouble(amount.replace(",", "."))));
		String pedigree = getPedigreeString(distribution);
		if (pedigree != null)
			exchange.setPedigreeUncertainty(pedigree);
	}

	private Uncertainty convertUncertainty(IDistribution distribution,
			double exchangeAmount) {
		Uncertainty uncertainty = new Uncertainty();
		uncertainty.setDistributionType(UncertaintyType.NONE);
		if (distribution == null)
			return uncertainty;
		switch (distribution.getType()) {
		case LOG_NORMAL:
			uncertainty.setDistributionType(UncertaintyType.LOG_NORMAL);
			// TODO check value 1
			uncertainty.setParameter1Value(0.0);
			uncertainty
					.setParameter2Value(distribution
							.getDistributionParameter(DistributionParameter.SQUARED_STANDARD_DEVIATION));
			break;
		case NORMAL:
			uncertainty.setDistributionType(UncertaintyType.NORMAL);
			// TODO check value 1
			uncertainty.setParameter1Value(0.0);
			uncertainty
					.setParameter2Value(distribution
							.getDistributionParameter(DistributionParameter.DOUBLED_STANDARD_DEVIATION));
			break;
		case TRIANGLE:
			uncertainty.setDistributionType(UncertaintyType.TRIANGLE);
			uncertainty
					.setParameter1Value(distribution
							.getDistributionParameter(DistributionParameter.MINIMUM));
			uncertainty.setParameter2Value(exchangeAmount);
			uncertainty
					.setParameter3Value(distribution
							.getDistributionParameter(DistributionParameter.MAXIMUM));
			break;
		case UNIFORM:
			uncertainty.setDistributionType(UncertaintyType.UNIFORM);
			uncertainty
					.setParameter1Value(distribution
							.getDistributionParameter(DistributionParameter.MINIMUM));
			uncertainty
					.setParameter2Value(distribution
							.getDistributionParameter(DistributionParameter.MAXIMUM));
			break;
		case UNDEFINED:
			uncertainty.setDistributionType(UncertaintyType.NONE);
			break;
		}
		return uncertainty;
	}

	private String getPedigreeString(IDistribution distribution) {
		if (!(distribution instanceof SPLogNormalDistribution))
			return null;
		SPLogNormalDistribution tempDistribution = (SPLogNormalDistribution) distribution;
		SPPedigreeMatrix spPedigreeMatrix = tempDistribution
				.getPedigreeMatrix();
		if (spPedigreeMatrix == null)
			return null;

		// TODO: check that is converted properly
		Map<PedigreeMatrixRow, Integer> matrix = new HashMap<>();
		matrix.put(PedigreeMatrixRow.RELIABILITY,
				getSPPedigreeValue(spPedigreeMatrix.reliability.getKey()));
		matrix.put(PedigreeMatrixRow.COMPLETENESS,
				getSPPedigreeValue(spPedigreeMatrix.completeness.getKey()));
		matrix.put(PedigreeMatrixRow.TIME,
				getSPPedigreeValue(spPedigreeMatrix.temporalCorrelation
						.getKey()));
		matrix.put(PedigreeMatrixRow.GEOGRAPHY,
				getSPPedigreeValue(spPedigreeMatrix.geographicalCorrelation
						.getKey()));
		matrix.put(PedigreeMatrixRow.TECHNOLOGY,
				getSPPedigreeValue(spPedigreeMatrix.technologicalCorrelation
						.getKey()));
		return PedigreeMatrix.toString(matrix);
	}

	private Integer getSPPedigreeValue(String value) {
		if (value.toLowerCase().equals("na"))
			return 1;
		return Integer.valueOf(value);
	}

	private Flow findOrCreate(ElementaryExchangeRow elementaryFlow) {
		if (elementaryFlow == null)
			return null;
		String refId = elemFlowMap.getOlcaId(CSVKeyGen
				.forElementaryFlow(elementaryFlow));
		if (refId == null)
			refId = CSVKeyGen.forElementaryFlow(elementaryFlow);
		Flow flow = flowDao.getForRefId(refId);
		if (flow != null)
			return flow;
		flow = new Flow();
		ElementaryFlowRow substance = cache.substanceMap.get(elementaryFlow.getName()
				+ elementaryFlow.getType().getValue());
		// TODO right exception
		if (substance == null)
			throw new IllegalAccessError();
		flow.setRefId(refId);
		flow.setName(substance.getName());
		setDefaultFlowProperty(flow, substance.getReferenceUnit());
		if (Utils.nullCheck(substance.getCASNumber()))
			flow.setCasNumber(substance.getCASNumber());
		if (Utils.nullCheck(substance.getComment())) {
			flow.setDescription(substance.getComment());
			String formula = getFlowFormula(substance.getComment());
			if (Utils.nullCheck(formula))
				flow.setFormula(formula);
		}
		flow.setCategory(getElementaryFlowCategory(elementaryFlow));
		flow.setFlowType(FlowType.ELEMENTARY_FLOW);
		flowDao.insert(flow);
		return flow;
	}

	private Flow findOrCreate(ProductExchangeRow productFlow) {
		if (productFlow == null)
			return null;
		if (!productFlow.hasReferenceData()) {
			log.debug("Can not find process for product: "
					+ productFlow.getName());
			// TODO: find a better name
			if (productFlow.getType() == ProductType.WASTE_TREATMENT)
				productFlow.setProcessCategory(ProcessCategory.WASTE_TREATMENT);
			else
				productFlow.setProcessCategory(ProcessCategory.MATERIAL);
			productFlow.setReferenceCategory("SimaPro not found");
		}
		String refId = productFlowMap.getOlcaId(CSVKeyGen
				.forProcess(productFlow.getName()));
		if (refId == null)
			refId = CSVKeyGen.forProduct(productFlow.getName());
		Flow flow = flowDao.getForRefId(refId);
		if (flow != null)
			return flow;
		flow = new Flow();
		flow.setRefId(refId);
		flow.setName(productFlow.getName());
		flow.setFlowType(FlowType.PRODUCT_FLOW);
		setDefaultFlowProperty(flow, productFlow.getUnit());
		flow.setCategory(Utils.createCategoryTree(database, categoryMap,
				ModelType.FLOW, productFlow.getProcessCategory().getValue(),
				productFlow.getReferenceCategory()));
		if (Utils.nullCheck(productFlow.getComment()))
			flow.setDescription(productFlow.getComment());
		flowDao.insert(flow);
		return flow;
	}

	private Flow findOrCreate(ProductOutputRow product) {
		if (product == null)
			return null;
		String refId = productFlowMap.getOlcaId(CSVKeyGen.forProduct(product
				.getName()));
		if (refId == null)
			refId = CSVKeyGen.forProduct(product.getName());
		// TODO: check category
		Flow flow = flowDao.getForRefId(refId);
		if (flow != null)
			return flow;
		flow = new Flow();
		flow.setRefId(UUID.randomUUID().toString());
		flow.setName(product.getName());
		setDefaultFlowProperty(flow, product.getUnit());
		if (Utils.nullCheck(product.getComment()))
			flow.setDescription(product.getComment());
		flow.setCategory(Utils.createCategoryTree(database, categoryMap,
				ModelType.FLOW, dataEntry.getDocumentation().getCategory()
						.getValue(), product.getCategory()));
		flow.setFlowType(FlowType.PRODUCT_FLOW);
		flowDao.insert(flow);
		return flow;
	}

	private Flow findOrCreate(WasteTreatmentRow wasteSpecification) {
		if (wasteSpecification == null)
			return null;
		String refId = productFlowMap.getOlcaId(CSVKeyGen
				.forProcess(wasteSpecification.getName()));
		if (refId == null)
			refId = CSVKeyGen.forProduct(wasteSpecification.getName());
		Flow flow = flowDao.getForRefId(refId);
		if (flow != null)
			return flow;
		flow = new Flow();
		flow.setRefId(refId);
		flow.setFlowType(FlowType.PRODUCT_FLOW);
		flow.setName(wasteSpecification.getName());
		setDefaultFlowProperty(flow, wasteSpecification.getUnit());
		flow.setCategory(Utils.createCategoryTree(database, categoryMap,
				ModelType.FLOW, dataEntry.getDocumentation().getCategory()
						.getValue(), wasteSpecification.getCategory()));
		if (Utils.nullCheck(wasteSpecification.getComment()))
			flow.setDescription(wasteSpecification.getComment());
		flowDao.insert(flow);
		return flow;
	}

	private String mapSubcompartmentCategory(SubCompartment subCompartment) {
		// TODO:
		if (subCompartment == null)
			subCompartment = SubCompartment.UNSPECIFIED;

		String subCategoryName = null;
		switch (subCompartment) {
		case AIRBORNE_HIGH_POP:
			subCategoryName = "high population density";
			break;
		case AIRBORNE_LOW_POP:
			subCategoryName = "low population density";
			break;
		case AIRBORNE_LOW_POP_LONG_TERM:
			subCategoryName = "low population density, long-term";
			break;
		case AIRBORNE_STATOSPHERE_TROPOSHERE:
			subCategoryName = "lower stratosphere + upper troposphere";
			break;
		case WATERBORNE_GROUNDWATER:
			subCategoryName = "ground water";
			break;
		case WATERBORNE_GROUNDWATER_LONG_TERM:
			subCategoryName = "ground water, long-term";
			break;
		default:
			subCategoryName = subCompartment.getValue();
			break;
		}
		return subCategoryName;
	}

	private String mapCompartmentCategory(ElementaryFlowType type) {
		String category = null;
		switch (type) {
		case EMISSION_TO_AIR:
			category = "air";
			break;
		case EMISSION_TO_SOIL:
			category = "soil";
			break;
		case EMISSION_TO_WATER:
			category = "water";
			break;
		case RESOURCE:
			category = "resource";
			break;
		default:
			category = type.getValue();
			break;
		}
		return category;
	}
}