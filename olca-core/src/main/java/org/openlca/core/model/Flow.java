package org.openlca.core.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "tbl_flows")
public class Flow extends CategorizedEntity {

	@Column(name = "flow_type")
	@Enumerated(EnumType.STRING)
	private FlowType flowType;

	@Column(name = "cas_number")
	private String casNumber;

	@OneToMany(cascade = { CascadeType.ALL }, orphanRemoval = true)
	@JoinColumn(name = "f_flow")
	private final List<FlowPropertyFactor> flowPropertyFactors = new ArrayList<>();

	@Column(name = "formula")
	private String formula;

	@Column(name = "infrastructure_flow")
	private boolean infrastructureFlow;

	@OneToOne
	@JoinColumn(name = "f_location")
	private Location location;

	@OneToOne
	@JoinColumn(name = "f_reference_flow_property")
	private FlowProperty referenceFlowProperty;

	@Override
	public Flow clone() {
		Flow flow = new Flow();
		flow.setRefId(UUID.randomUUID().toString());
		flow.setName(getName());
		flow.setCategory(getCategory());
		flow.setDescription(getDescription());
		flow.setFlowType(getFlowType());
		flow.setCasNumber(getCasNumber());
		flow.setFormula(getFormula());
		flow.setInfrastructureFlow(isInfrastructureFlow());
		flow.setLocation(getLocation());
		flow.setReferenceFlowProperty(getReferenceFlowProperty());
		for (FlowPropertyFactor factor : getFlowPropertyFactors()) {
			flow.getFlowPropertyFactors().add(factor.clone());
		}
		return flow;
	}

	public FlowType getFlowType() {
		return flowType;
	}

	public void setFlowType(FlowType flowType) {
		this.flowType = flowType;
	}

	public String getCasNumber() {
		return casNumber;
	}

	public void setCasNumber(String casNumber) {
		this.casNumber = casNumber;
	}

	public String getFormula() {
		return formula;
	}

	public void setFormula(String formula) {
		this.formula = formula;
	}

	public boolean isInfrastructureFlow() {
		return infrastructureFlow;
	}

	public void setInfrastructureFlow(boolean infrastructureFlow) {
		this.infrastructureFlow = infrastructureFlow;
	}

	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

	public FlowProperty getReferenceFlowProperty() {
		return referenceFlowProperty;
	}

	public void setReferenceFlowProperty(FlowProperty referenceFlowProperty) {
		this.referenceFlowProperty = referenceFlowProperty;
	}

	public List<FlowPropertyFactor> getFlowPropertyFactors() {
		return flowPropertyFactors;
	}

	public FlowPropertyFactor getReferenceFactor() {
		if (referenceFlowProperty == null)
			return null;
		for (FlowPropertyFactor f : getFlowPropertyFactors()) {
			if (Objects.equals(referenceFlowProperty, f.getFlowProperty()))
				return f;
		}
		return null;
	}

	public FlowPropertyFactor getFactor(FlowProperty property) {
		for (FlowPropertyFactor f : getFlowPropertyFactors())
			if (Objects.equals(f.getFlowProperty(), property))
				return f;
		return null;
	}

}
