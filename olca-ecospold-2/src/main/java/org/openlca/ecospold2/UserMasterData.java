package org.openlca.ecospold2;

import java.util.ArrayList;
import java.util.List;

import org.jdom2.Element;

public class UserMasterData {

	private List<Unit> units = new ArrayList<>();
	private List<ActivityName> activityNames = new ArrayList<>();
	private List<Company> companies = new ArrayList<>();
	private List<Person> persons = new ArrayList<>();
	private List<Source> sources = new ArrayList<>();
	private List<IntermediateExchange> intermediateExchanges = new ArrayList<>();
	private List<ElementaryExchange> elementaryExchanges = new ArrayList<>();
	private List<Geography> geographies = new ArrayList<>();
	private List<ActivityIndexEntry> activityIndexEntries = new ArrayList<>();
	private List<Parameter> parameters = new ArrayList<>();

	public List<ActivityName> getActivityNames() {
		return activityNames;
	}

	public List<IntermediateExchange> getIntermediateExchanges() {
		return intermediateExchanges;
	}

	public List<ElementaryExchange> getElementaryExchanges() {
		return elementaryExchanges;
	}

	public List<Unit> getUnits() {
		return units;
	}

	public List<ActivityIndexEntry> getActivityIndexEntries() {
		return activityIndexEntries;
	}

	public List<Geography> getGeographies() {
		return geographies;
	}

	public List<Parameter> getParameters() {
		return parameters;
	}

	public void setParameters(List<Parameter> parameters) {
		this.parameters = parameters;
	}

	public List<Person> getPersons() {
		return persons;
	}

	public List<Company> getCompanies() {
		return companies;
	}

	public List<Source> getSources() {
		return sources;
	}

	public Element toXml() {
		Element e = new Element("usedUserMasterData", IO.MD_NS);
		for (Unit unit : units)
			e.addContent(unit.toXml(IO.MD_NS));
		for (ActivityName activityName : activityNames)
			e.addContent(activityName.toXml(IO.MD_NS));
		for (Company company : companies)
			e.addContent(company.toXml(IO.MD_NS));
		for (Person person : persons)
			e.addContent(person.toXml(IO.MD_NS));
		for (Source source : sources)
			e.addContent(source.toXml(IO.MD_NS));
		for (ElementaryExchange elemFlow : elementaryExchanges)
			e.addContent(elemFlow.toXml(IO.MD_NS));
		for (IntermediateExchange techFlow : intermediateExchanges)
			e.addContent(techFlow.toXml(IO.MD_NS));
		for (ActivityIndexEntry indexEntry : activityIndexEntries)
			e.addContent(indexEntry.toXml(IO.MD_NS));
		return e;
	}

}
