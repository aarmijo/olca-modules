package org.openlca.io.xls.process.output;

import org.apache.poi.ss.usermodel.Sheet;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.Unit;
import org.openlca.io.CategoryPath;
import org.openlca.io.xls.Excel;
import org.openlca.util.Strings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

class IOSheet {

	private final Config config;
	private final Sheet sheet;
	private final boolean forInputs;
	private int row = 0;

	private IOSheet(Config config, boolean forInputs) {
		this.config = config;
		this.forInputs = forInputs;
		String sheetName = forInputs ? "Inputs" : "Outputs";
		sheet = config.workbook.createSheet(sheetName);
	}

	public static void writeInputs(Config config) {
		new IOSheet(config, true).write();
	}

	public static void writeOutputs(Config config) {
		new IOSheet(config, false).write();
	}

	private void write() {
		witeHeader();
		row++;
		for (Exchange exchange : getExchanges()) {
			if (exchange.getFlow() == null)
				continue;
			write(exchange);
			row++;
		}
		Excel.autoSize(sheet, 0, 8);
	}

	private void witeHeader() {
		config.header(sheet, row, 0, "Flow");
		config.header(sheet, row, 1, "Category");
		config.header(sheet, row, 2, "Flow property");
		config.header(sheet, row, 3, "Unit");
		config.header(sheet, row, 4, "Amount");
		config.header(sheet, row, 5, "Formula");
		config.header(sheet, row, 6, "Uncertainty");
		config.header(sheet, row, 7, "(g)mean | mode");
		config.header(sheet, row, 8, "SD | GSD");
		config.header(sheet, row, 9, "Minimum");
		config.header(sheet, row, 10, "Maximum");
	}

	private void write(Exchange exchange) {
		Flow flow = exchange.getFlow();
		Excel.cell(sheet, row, 0, flow.getName());
		Excel.cell(sheet, row, 1, CategoryPath.getFull(flow.getCategory()));
		Excel.cell(sheet, row, 2, getFlowProperty(exchange));
		Excel.cell(sheet, row, 3, getUnit(exchange));
		Excel.cell(sheet, row, 4, exchange.getAmountValue());
		Excel.cell(sheet, row, 5, exchange.getAmountFormula());
		config.uncertainty(sheet, row, 6, exchange.getUncertainty());
	}

	private String getFlowProperty(Exchange exchange) {
		FlowPropertyFactor factor = exchange.getFlowPropertyFactor();
		if (factor == null)
			return null;
		FlowProperty prop = factor.getFlowProperty();
		return prop == null ? null : prop.getName();
	}

	private String getUnit(Exchange exchange) {
		Unit unit = exchange.getUnit();
		return unit == null ? null : unit.getName();
	}

	private List<Exchange> getExchanges() {
		List<Exchange> exchanges = new ArrayList<>();
		for (Exchange exchange : config.process.getExchanges()) {
			if (exchange.isInput() == forInputs)
				exchanges.add(exchange);
		}
		Collections.sort(exchanges, new Comparator<Exchange>() {
			@Override
			public int compare(Exchange e1, Exchange e2) {
				if (e1.getFlow() == null || e2.getFlow() == null)
					return 0;
				return Strings.compare(e1.getFlow().getName(), e2.getFlow()
						.getName());
			}
		});
		return exchanges;
	}
}
