package org.openlca.ecospold2;

import java.util.ArrayList;
import java.util.List;

import org.jdom2.Element;

public abstract class Exchange {

	private String id;
	private String unitId;
	private Double amount; // amount is a reference type because it can be
							// optional in master data
	private String name;
	private String unitName;
	private String variableName;
	private String mathematicalRelation;
	private String casNumber;
	private String comment;
	private Uncertainty uncertainty;
	private List<Property> properties = new ArrayList<>();
	private Integer outputGroup;
	private Integer inputGroup;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUnitId() {
		return unitId;
	}

	public void setUnitId(String unitId) {
		this.unitId = unitId;
	}

	public Double getAmount() {
		return amount;
	}

	public void setAmount(Double amount) {
		this.amount = amount;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUnitName() {
		return unitName;
	}

	public void setUnitName(String unitName) {
		this.unitName = unitName;
	}

	public String getMathematicalRelation() {
		return mathematicalRelation;
	}

	public void setMathematicalRelation(String mathematicalRelation) {
		this.mathematicalRelation = mathematicalRelation;
	}

	public void setCasNumber(String casNumber) {
		this.casNumber = casNumber;
	}

	public String getCasNumber() {
		return casNumber;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public void setUncertainty(Uncertainty uncertainty) {
		this.uncertainty = uncertainty;
	}

	public Uncertainty getUncertainty() {
		return uncertainty;
	}

	public Integer getInputGroup() {
		return inputGroup;
	}

	public Integer getOutputGroup() {
		return outputGroup;
	}

	public void setInputGroup(Integer inputGroup) {
		this.inputGroup = inputGroup;
	}

	public void setOutputGroup(Integer outputGroup) {
		this.outputGroup = outputGroup;
	}

	public String getVariableName() {
		return variableName;
	}

	public void setVariableName(String variableName) {
		this.variableName = variableName;
	}

	public List<Property> getProperties() {
		return properties;
	}

	protected void readValues(Element element) {
		setAmount(In.optionalDecimal(element.getAttributeValue("amount")));
		setId(element.getAttributeValue("id"));
		setMathematicalRelation(element
				.getAttributeValue("mathematicalRelation"));
		setVariableName(element.getAttributeValue("variableName"));
		setName(In.childText(element, "name"));
		setUnitName(In.childText(element, "unitName"));
		setComment(In.childText(element, "comment"));
		setUnitId(element.getAttributeValue("unitId"));
		setUncertainty(Uncertainty.fromXml(In.child(element, "uncertainty")));
		setCasNumber(element.getAttributeValue("casNumber"));
		List<Element> propElems = In.childs(element, "property");
		for (Element propElem : propElems) {
			Property property = Property.fromXml(propElem);
			if (property != null)
				properties.add(property);
		}

		String inGroup = In.childText(element, "inputGroup");
		if (inGroup != null)
			setInputGroup(In.integer(inGroup));
		else {
			String outGroup = In.childText(element, "outputGroup");
			setOutputGroup(In.integer(outGroup));
		}
	}

	protected void writeValues(Element element) {
		if (id != null)
			element.setAttribute("id", id);
		if (unitId != null)
			element.setAttribute("unitId", unitId);
		if (amount != null)
			element.setAttribute("amount", amount.toString());
		if (mathematicalRelation != null)
			element.setAttribute("mathematicalRelation", mathematicalRelation);
		if (variableName != null)
			element.setAttribute("variableName", variableName);
		if (casNumber != null)
			element.setAttribute("casNumber", casNumber);
		if (name != null)
			Out.addChild(element, "name", name);
		if (unitName != null)
			Out.addChild(element, "unitName", unitName);
		if (comment != null)
			Out.addChild(element, "comment", comment);
		if (uncertainty != null)
			element.addContent(uncertainty.toXml());
		for (Property property : properties)
			element.addContent(property.toXml());
	}

	protected void writeInputOutputGroup(Element element) {
		if (inputGroup != null)
			Out.addChild(element, "inputGroup").setText(inputGroup.toString());
		else if (outputGroup != null)
			Out.addChild(element, "outputGroup")
					.setText(outputGroup.toString());
	}

}
