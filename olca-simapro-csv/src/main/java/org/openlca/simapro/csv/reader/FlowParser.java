package org.openlca.simapro.csv.reader;

import org.openlca.simapro.csv.CsvUtils;
import org.openlca.simapro.csv.model.ElementaryExchangeRow;
import org.openlca.simapro.csv.model.ProductInputRow;
import org.openlca.simapro.csv.model.SPWasteSpecification;
import org.openlca.simapro.csv.model.enums.ElementaryFlowType;
import org.openlca.simapro.csv.model.enums.ProductFlowType;
import org.openlca.simapro.csv.model.process.ProductOutputRow;
import org.openlca.simapro.csv.model.refdata.ElementaryFlowRow;
import org.openlca.simapro.csv.parser.exception.CSVParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

class FlowParser {

	private Logger log = LoggerFactory.getLogger(getClass());

	private String csvSeperator;
	private Map<String, String[]> index;

	FlowParser(String csvSeperator, Map<String, String[]> index) {
		log.trace("create flow parser with separator: {}", csvSeperator);
		this.csvSeperator = csvSeperator;
		this.index = index;
	}

	ElementaryExchangeRow parseElementaryFlow(String line,
			ElementaryFlowType type) {
		ElementaryExchangeRow exchange = ElementaryExchangeRow.fromCsv(line,
				csvSeperator);
		exchange.setType(type);
		return exchange;
	}

	ProductInputRow getProductFlow(String line, ProductFlowType type) {
		ProductInputRow input = ProductInputRow.fromCsv(line, csvSeperator);
		input.setType(type);
		return input;
	}

	ProductOutputRow readReferenceProduct(String line) throws CSVParserException {
		line += csvSeperator + " ";
		String split[] = line.split(csvSeperator);
		if (split.length < 7)
			throw new CSVParserException("Error in product line: " + line);
		ProductOutputRow product = new ProductOutputRow();
		product.setName(split[0]);
		product.setUnit(split[1]);
		product.setAmount(split[2]);
		product.setAllocation(Double.parseDouble(CsvUtils
				.formatNumber(split[3])));
		product.setWasteType(split[4]);
		product.setCategory(split[5]);
		String comment = split[6];
		for (int i = 7; i < (split.length - 1); i++)
			comment += csvSeperator + split[i];
		product.setComment(comment);
		return product;
	}

	SPWasteSpecification readWasteSpecification(String line)
			throws CSVParserException {
		line += csvSeperator + " ";
		String split[] = line.split(csvSeperator);
		if (split.length < 6)
			throw new CSVParserException("Error in waste specification line: "
					+ line);
		SPWasteSpecification waste = new SPWasteSpecification();
		waste.setName(split[0]);
		waste.setUnit(split[1]);
		waste.setAmount(split[2]);
		waste.setWasteType(split[3]);
		waste.setCategory(split[4]);
		String comment = split[5];
		for (int i = 6; i < (split.length - 1); i++) {
			comment += csvSeperator + split[i];
		}
		waste.setComment(comment);
		return waste;
	}

	static ElementaryFlowRow parseSubstance(String line, String csvSeperator,
			ElementaryFlowType type) {
		line += csvSeperator + " ";
		String split[] = line.split(csvSeperator);
		String name = split[0];
		String referenceUnit = split[1];
		String cas = split[2];
		String comment = split[3];

		for (int i = 4; i < (split.length - 1); i++) {
			comment += csvSeperator + split[i];
		}

		ElementaryFlowRow substance = new ElementaryFlowRow(name, referenceUnit);
		substance.setReferenceUnit(referenceUnit);
		substance.setCASNumber(cas);
		substance.setComment(comment);
		substance.setFlowType(type);
		return substance;
	}

}