package org.openlca.core.model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "tbl_processes")
public class Process extends CategorizedEntity {

	@Column(name = "default_allocation_method")
	@Enumerated(EnumType.STRING)
	private AllocationMethod defaultAllocationMethod;

	@OneToMany(cascade = { CascadeType.ALL }, orphanRemoval = true)
	@JoinColumn(name = "f_process")
	private final List<AllocationFactor> allocationFactors = new ArrayList<>();

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "f_owner")
	private List<Exchange> exchanges = new ArrayList<>();

	@OneToOne
	@JoinColumn(name = "f_location")
	private Location location;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "f_owner")
	private final List<Parameter> parameters = new ArrayList<>();

	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "f_process_doc")
	private ProcessDocumentation documentation;

	@Column(name = "process_type")
	@Enumerated(EnumType.STRING)
	private ProcessType processType = ProcessType.UNIT_PROCESS;

	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "f_quantitative_reference")
	private Exchange quantitativeReference;

	@Column(name = "infrastructure_process")
	private boolean infrastructureProcess;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "f_process")
	private final List<ProcessCostEntry> costEntries = new ArrayList<>();

	@Lob
	@Column(name = "kmz")
	private byte[] kmz;

	public ProcessDocumentation getDocumentation() {
		return documentation;
	}

	public void setDocumentation(ProcessDocumentation documentation) {
		this.documentation = documentation;
	}

	@Override
	public Process clone() {
		return new ProcessCopy().create(this);
	}

	public Location getLocation() {
		return location;
	}

	public ProcessType getProcessType() {
		return processType;
	}

	public Exchange getQuantitativeReference() {
		return quantitativeReference;
	}

	public List<Exchange> getExchanges() {
		return exchanges;
	}

	public List<Parameter> getParameters() {
		return parameters;
	}

	public boolean isInfrastructureProcess() {
		return infrastructureProcess;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

	public void setProcessType(ProcessType processType) {
		this.processType = processType;
	}

	public void setQuantitativeReference(Exchange quantitativeReference) {
		this.quantitativeReference = quantitativeReference;
	}

	public void setInfrastructureProcess(boolean infrastructureProcess) {
		this.infrastructureProcess = infrastructureProcess;
	}

	public AllocationMethod getDefaultAllocationMethod() {
		return defaultAllocationMethod;
	}

	public void setDefaultAllocationMethod(AllocationMethod method) {
		this.defaultAllocationMethod = method;
	}

	public List<AllocationFactor> getAllocationFactors() {
		return allocationFactors;
	}

	public List<ProcessCostEntry> getCostEntries() {
		return costEntries;
	}

	public byte[] getKmz() {
		return kmz;
	}

	public void setKmz(byte[] kmz) {
		this.kmz = kmz;
	}

}
