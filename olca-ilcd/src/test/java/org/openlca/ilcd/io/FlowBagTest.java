package org.openlca.ilcd.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.openlca.ilcd.commons.Category;
import org.openlca.ilcd.commons.FlowType;
import org.openlca.ilcd.flows.Flow;
import org.openlca.ilcd.flows.FlowPropertyReference;
import org.openlca.ilcd.io.XmlBinder;
import org.openlca.ilcd.util.FlowBag;

public class FlowBagTest {

	private FlowBag bag;

	@Before
	public void setUp() throws Exception {
		try (InputStream stream = this.getClass().getResourceAsStream(
				"flow.xml")) {
			XmlBinder binder = new XmlBinder();
			Flow flow = binder.fromStream(Flow.class, stream);
			bag = new FlowBag(flow);
		}
	}

	@Test
	public void testGetId() {
		assertEquals("0d7a3ad1-6556-11dd-ad8b-0800200c9a66", bag.getId());
	}

	@Test
	public void testGetName() {
		assertEquals("glycidol", bag.getName());
	}

	@Test
	public void testGetCasNumber() {
		assertEquals("000556-52-5", bag.getCasNumber());
	}

	@Test
	public void testGetSumFormula() {
		assertEquals("C3H6O2", bag.getSumFormula());
	}

	@Test
	public void testGetReferenceFlowPropertyId() {
		assertEquals(Integer.valueOf(0), bag.getReferenceFlowPropertyId());
	}

	@Test
	public void testGetFlowType() {
		assertEquals(FlowType.ELEMENTARY_FLOW, bag.getFlowType());
	}

	@Test
	public void testGetFlowPropertyReferences() {
		List<FlowPropertyReference> flowPropertyReferences = bag
				.getFlowPropertyReferences();
		assertTrue(flowPropertyReferences.size() == 1);
		FlowPropertyReference ref = flowPropertyReferences.get(0);
		assertEquals("93a60a56-a3c8-11da-a746-0800200b9a66", ref
				.getFlowProperty().getUuid());
	}

	@Test
	public void testGetSortedClasses() {
		List<org.openlca.ilcd.commons.Class> classes = bag.getSortedClasses();
		assertTrue(classes.isEmpty());
	}

	@Test
	public void testGetSortedCompartments() {
		List<Category> categories = bag.getSortedCompartments();
		assertTrue(categories.size() == 3);
		assertEquals("Emissions", categories.get(0).getValue().trim());
		assertEquals("Emissions to air", categories.get(1).getValue().trim());
		assertEquals("Emissions to lower stratosphere and upper troposphere",
				categories.get(2).getValue().trim());
	}

}
