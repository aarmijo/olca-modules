package org.openlca.io.ilcd.output;

import java.io.File;

import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Source;
import org.openlca.core.model.Version;
import org.openlca.ilcd.commons.ClassificationInformation;
import org.openlca.ilcd.io.DataStore;
import org.openlca.ilcd.io.DataStoreException;
import org.openlca.ilcd.sources.AdministrativeInformation;
import org.openlca.ilcd.sources.DataEntry;
import org.openlca.ilcd.sources.DataSetInformation;
import org.openlca.ilcd.sources.DigitalFileReference;
import org.openlca.ilcd.sources.Publication;
import org.openlca.ilcd.sources.SourceInformation;
import org.openlca.ilcd.util.LangString;
import org.openlca.ilcd.util.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SourceExport {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	private DataStore dataStore;
	private IDatabase database;
	private String baseUri;

	public SourceExport(IDatabase database, DataStore dataStore) {
		this.dataStore = dataStore;
		this.database = database;
	}

	public void setBaseUri(String baseUri) {
		this.baseUri = baseUri;
	}

	public org.openlca.ilcd.sources.Source run(Source source)
			throws DataStoreException {
		log.trace("Run source export with {}", source);
		DataSetInformation dataSetInfo = makeDateSetInfo(source);
		org.openlca.ilcd.sources.Source iSource = new org.openlca.ilcd.sources.Source();
		iSource.setVersion("1.1");
		SourceInformation info = new SourceInformation();
		iSource.setSourceInformation(info);
		info.setDataSetInformation(makeDateSetInfo(source));
		iSource.setAdministrativeInformation(makeAdminInfo(source));
		File extFile = getExternalFile(source);
		if (extFile == null)
			dataStore.put(iSource, source.getRefId());
		else {
			addFileRef(dataSetInfo, extFile);
			dataStore.put(iSource, source.getRefId(), extFile);
		}
		return iSource;
	}

	private File getExternalFile(Source source) {
		String name = source.getExternalFile();
		if (name == null)
			return null;
		File dbDir = database.getFileStorageLocation();
		if (dbDir == null)
			return null;
		File docDir = new File(dbDir, "external_docs");
		if (!docDir.exists())
			return null;
		File file = new File(docDir, name);
		return file.exists() ? file : null;
	}

	private DataSetInformation makeDateSetInfo(Source source) {
		log.trace("Create data set information.");
		DataSetInformation info = new DataSetInformation();
		info.setUUID(source.getRefId());
		LangString.addLabel(info.getShortName(), source.getName());
		if (source.getDescription() != null) {
			LangString.addFreeText(info.getSourceDescriptionOrComment(),
					source.getDescription());
		}
		addTextReference(source, info);
		CategoryConverter converter = new CategoryConverter();
		ClassificationInformation classInfo = converter
				.getClassificationInformation(source.getCategory());
		info.setClassificationInformation(classInfo);
		return info;
	}

	private void addTextReference(Source source, DataSetInformation dataSetInfo) {
		log.trace("Create text reference.");
		String cit = source.getTextReference();
		if (cit == null)
			return;
		if (source.getYear() != null)
			cit += " " + source.getYear();
		dataSetInfo.setSourceCitation(cit);
	}

	private void addFileRef(DataSetInformation info, File extFile) {
		DigitalFileReference fileRef = new DigitalFileReference();
		fileRef.setUri(extFile.getName());
		info.getReferenceToDigitalFile().add(fileRef);
	}

	private AdministrativeInformation makeAdminInfo(Source source) {
		AdministrativeInformation info = new AdministrativeInformation();
		DataEntry entry = new DataEntry();
		info.setDataEntryBy(entry);
		entry.setTimeStamp(Out.getTimestamp(source));
		entry.getReferenceToDataSetFormat().add(Reference.forIlcdFormat());
		addPublication(info, source);
		return info;
	}

	private void addPublication(AdministrativeInformation info, Source source) {
		Publication pub = new Publication();
		info.setPublicationAndOwnership(pub);
		pub.setDataSetVersion(Version.asString(source.getVersion()));
		if (baseUri == null)
			baseUri = "http://openlca.org/ilcd/resource/";
		if (!baseUri.endsWith("/"))
			baseUri += "/";
		pub.setPermanentDataSetURI(baseUri + "sources/" + source.getRefId());
	}
}
