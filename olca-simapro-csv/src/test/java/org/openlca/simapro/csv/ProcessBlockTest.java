package org.openlca.simapro.csv;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;

import org.junit.Before;
import org.junit.Test;
import org.openlca.simapro.csv.model.enums.BoundaryWithNature;
import org.openlca.simapro.csv.model.enums.CutOffRule;
import org.openlca.simapro.csv.model.enums.Geography;
import org.openlca.simapro.csv.model.enums.ProcessAllocation;
import org.openlca.simapro.csv.model.enums.ProcessCategory;
import org.openlca.simapro.csv.model.enums.ProcessType;
import org.openlca.simapro.csv.model.enums.ProductType;
import org.openlca.simapro.csv.model.enums.Representativeness;
import org.openlca.simapro.csv.model.enums.Status;
import org.openlca.simapro.csv.model.enums.Substitution;
import org.openlca.simapro.csv.model.enums.Technology;
import org.openlca.simapro.csv.model.enums.TimePeriod;
import org.openlca.simapro.csv.model.process.ProcessBlock;
import org.openlca.simapro.csv.reader.BlockReader;
import org.openlca.simapro.csv.reader.ModelReader;

public class ProcessBlockTest {

	private ProcessBlock block;

	@Before
	public void setUp() throws Exception {
		try (InputStream is = this.getClass().getResourceAsStream(
				"simple_process.csv");
				InputStreamReader reader = new InputStreamReader(is);
				BlockReader blockReader = new BlockReader(reader);
				ModelReader modelReader = new ModelReader(blockReader,
						CsvConfig.getDefault(), ProcessBlock.class)) {
			this.block = (ProcessBlock) modelReader.read();
		}
	}

	@Test
	public void testTextEntries() {
		assertEquals("DefaultX25250700002", block.getIdentifier());
		assertEquals("Test process", block.getName());
		assertEquals("First order (only primary flows)",
				block.getCapitalGoods());
		assertEquals("data entry by: [System]", block.getRecord());
		assertEquals("generated by: [System]", block.getGenerator());
		assertEquals("text for collection method", block.getCollectionMethod());
		assertEquals("text for data treatment", block.getDataTreatment());
		assertEquals("text for verification", block.getVerification());
		assertEquals("text for comment", block.getComment());
		assertEquals("text for allocation rules", block.getAllocationRules());
	}

	@Test
	public void testEnumEntries() {
		assertEquals(ProcessCategory.MATERIAL, block.getCategory());
		assertEquals(ProcessType.UNIT_PROCESS, block.getProcessType());
		assertEquals(Status.DRAFT, block.getStatus());
		assertEquals(TimePeriod.P_2005_2009, block.getTime());
		assertEquals(Geography.MIXED_DATA, block.getGeography());
		assertEquals(Technology.WORST_CASE, block.getTechnology());
		assertEquals(Representativeness.THEORETICAL_CALCULATION,
				block.getRepresentativeness());
		assertEquals(ProcessAllocation.PHYSICAL_CAUSALITY,
				block.getAllocation());
		assertEquals(Substitution.ACTUAL_SUBSTITUTION, block.getSubstitution());
		assertEquals(CutOffRule.PHYSICAL_LESS_THAN_1, block.getCutoff());
		assertEquals(BoundaryWithNature.AGRICULTURAL_PRODUCTION_SYSTEM,
				block.getBoundaryWithNature());
	}

	@Test
	public void testElementaryExchanges() {
		assertEquals("Acids", block.getResources().get(0).getName());
		assertEquals("(+-)-Citronellol", block.getEmissionsToAir().get(0)
				.getName());
		assertEquals("(1r,4r)-(+)-Camphor", block.getEmissionsToWater().get(0)
				.getName());
		assertEquals("1'-Acetoxysafrole", block.getEmissionsToSoil().get(0)
				.getName());
		assertEquals("Asbestos", block.getFinalWasteFlows().get(0).getName());
		assertEquals("Noise from bus km", block.getNonMaterialEmissions()
				.get(0).getName());
		assertEquals("venting of argon, crude, liquid", block.getSocialIssues()
				.get(0).getName());
		assertEquals("Sample economic issue", block.getEconomicIssues().get(0)
				.getName());
	}

	@Test
	public void testProductRow() {
		assertEquals(1, block.getProducts().size());
		assertEquals("my product", block.getProducts().get(0).getName());
	}

	@Test
	public void testProductExchanges() {
		for (ProductType productType : ProductType.values())
			assertEquals(1, block.getProductExchanges(productType).size());
		assertEquals("Wool, at field/US", block.getAvoidedProducts().get(0)
				.getName());
		assertEquals("Soy oil, refined, at plant/kg/RNA", block
				.getMaterialsAndFuels().get(0).getName());
		assertEquals("Electricity, biomass, at power plant/US", block
				.getElectricityAndHeat().get(0).getName());
		assertEquals("Dummy, Disposal, msw, to sanitary landfill/kg/GLO", block
				.getWasteToTreatment().get(0).getName());
	}

	@Test
	public void testParameters() {
		assertEquals(1, block.getCalculatedParameters().size());
		assertEquals(1, block.getInputParameters().size());
		assertEquals("calc_param", block.getCalculatedParameters().get(0)
				.getName());
		assertEquals("input_param", block.getInputParameters().get(0).getName());
	}

	@Test
	public void testOtherEntries() {
		SimpleDateFormat format = new SimpleDateFormat(CsvConfig.getDefault()
				.getDateFormat());
		String dateString = format.format(block.getDate());
		assertEquals("24.02.2014", dateString);
		assertEquals(false, block.getInfrastructure());
	}

	@Test
	public void testLiteratureReferences() {
		assertEquals(1, block.getLiteratureReferences().size());
		assertEquals("Ecoinvent 3", block.getLiteratureReferences().get(0)
				.getName());
	}

	@Test
	public void testSystemDescription() {
		assertEquals("U.S. LCI Database", block.getSystemDescription()
				.getName());
	}
}
