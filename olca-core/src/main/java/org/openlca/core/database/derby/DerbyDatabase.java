package org.openlca.core.database.derby;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManagerFactory;

import org.apache.derby.jdbc.EmbeddedDriver;
import org.eclipse.persistence.jpa.PersistenceProvider;
import org.openlca.core.database.BaseDao;
import org.openlca.core.database.DatabaseContent;
import org.openlca.core.database.DatabaseException;
import org.openlca.core.database.DbUtils;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.internal.Resource;
import org.openlca.core.database.internal.ScriptRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;

public class DerbyDatabase implements IDatabase {

	private Logger log = LoggerFactory.getLogger(getClass());
	private EntityManagerFactory entityFactory;
	private String url;
	private File folder;
	private boolean closed = false;
	private BoneCP connectionPool;

	public static DerbyDatabase createInMemory(DatabaseContent content) {
		return new DerbyDatabase(content);
	}

	/** Creates an in-memory database. */
	private DerbyDatabase(DatabaseContent content) {
		registerDriver();
		String name = "olcaInMemDB"
				+ Integer.toHexString((int) (Math.random() * 1000));
		log.trace("create in memory database");
		url = "jdbc:derby:memory:" + name + ";create=true";
		createNew(url);
		fill(content);
		connect();
	}

	public DerbyDatabase(File folder) {
		registerDriver();
		this.folder = folder;
		boolean create = shouldCreateNew(folder);
		log.info("initialize database folder {}, create={}", folder, create);
		url = "jdbc:derby:" + folder.getAbsolutePath().replace('\\', '/');
		log.trace("database url: {}", url);
		if (create)
			createNew(url + ";create=true");
		connect();
	}

	private boolean shouldCreateNew(File folder) {
		// see the Derby folder specification:
		// http://db.apache.org/derby/docs/10.0/manuals/develop/develop13.html
		if (!folder.exists())
			return true;
		File log = new File(folder, "log");
		if (!log.exists())
			return true;
		File seg0 = new File(folder, "seg0");
		if (!seg0.exists())
			return true;
		return false;
	}

	private void registerDriver() {
		try {
			DriverManager.registerDriver(new EmbeddedDriver());
		} catch (Exception e) {
			throw new RuntimeException("Could not register driver", e);
		}
	}

	private void createNew(String url) {
		log.info("create new database {}", url);
		try {
			Connection con = DriverManager.getConnection(url);
			con.close();
			ScriptRunner runner = new ScriptRunner(this);
			runner.run(Resource.CURRENT_SCHEMA_DERBY.getStream(), "utf-8");
		} catch (Exception e) {
			log.error("failed to create database", e);
			throw new DatabaseException("Failed to create database", e);
		}
	}

	/**
	 * Returns the Derby database directory (see
	 * http://db.apache.org/derby/docs/10.0/manuals/develop/develop13.html). The
	 * name of the directory is equal to the database name.
	 */
	public File getDatabaseDirectory() {
		return folder;
	}

	/**
	 * Returns the folder '_olca_' within the database directory. If this folder
	 * does not exist is created when this method is called.
	 */
	public File getFileStorageLocation() {
		File dir = new File(folder, "_olca_");
		if (!dir.exists())
			dir.mkdirs();
		return dir;
	}

	/** Fill the database with the given content. */
	public void fill(DatabaseContent content) {
		if (content == null || content == DatabaseContent.EMPTY)
			return;
		log.trace("fill database with content: {}", content);
		Resource resource = null;
		if (content == DatabaseContent.ALL_REF_DATA)
			resource = Resource.REF_DATA_ALL;
		else if (content == DatabaseContent.UNITS)
			resource = Resource.REF_DATA_UNITS;
		if (resource == null)
			return;
		try {
			ScriptRunner runner = new ScriptRunner(this);
			runner.run(resource.getStream(), "utf-8");
		} catch (Exception e) {
			log.error("failed to fill database with  content", e);
		}
	}

	private void connect() {
		log.trace("connect to database: {}", url);
		Map<Object, Object> map = new HashMap<>();
		map.put("javax.persistence.jdbc.url", url);
		map.put("javax.persistence.jdbc.driver",
				"org.apache.derby.jdbc.EmbeddedDriver");
		map.put("eclipselink.classloader", getClass().getClassLoader());
		map.put("eclipselink.target-database", "Derby");
		entityFactory = new PersistenceProvider().createEntityManagerFactory(
				"openLCA", map);
		initConnectionPool();
	}

	private void initConnectionPool() {
		try {
			BoneCPConfig config = new BoneCPConfig();
			config.setJdbcUrl(url);
			connectionPool = new BoneCP(config);
		} catch (Exception e) {
			log.error("failed to initialize connection pool", e);
			throw new DatabaseException("Could not create a connection", e);
		}
	}

	@Override
	public void close() throws IOException {
		if (closed)
			return;
		log.trace("close database: {}", url);
		if (entityFactory != null && entityFactory.isOpen())
			entityFactory.close();
		if (connectionPool != null)
			connectionPool.shutdown();
		try {
			DriverManager.getConnection(url + ";shutdown=true");
			// TODO: single database shutdown throws unexpected
			// error in eclipse APP - close all connections here
			// DriverManager.getConnection("jdbc:derby:;shutdown=true");
			System.gc(); // unload embedded driver for possible restarts
			// see also
			// http://db.apache.org/derby/docs/10.4/devguide/rdevcsecure26537.html
		} catch (SQLException e) {
			// a normal shutdown of derby throws an SQL exception
			// with error code 50000 (for single database shutdown
			// 45000), otherwise an error occurred
			log.info("exception: {}", e.getErrorCode());
			if (e.getErrorCode() != 45000 && e.getErrorCode() != 50000)
				log.error(e.getMessage(), e);
			else {
				closed = true;
				log.info("database closed");
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	@Override
	public Connection createConnection() {
		log.trace("create connection: {}", url);
		try {
			if (connectionPool != null) {
				Connection con = connectionPool.getConnection();
				con.setAutoCommit(false);
				return con;
			} else {
				log.warn("no connection pool set up for {}", url);
				return DriverManager.getConnection(url);
			}
		} catch (Exception e) {
			log.error("Failed to create database connection", e);
			return null;
		}
	}

	@Override
	public EntityManagerFactory getEntityFactory() {
		return entityFactory;
	}

	@Override
	public <T> BaseDao<T> createDao(Class<T> clazz) {
		return new BaseDao<>(clazz, this);
	}

	@Override
	public String getName() {
		if (folder != null)
			return folder.getName();
		else
			return "in-memory";
	}

	@Override
	public int getVersion() {
		return DbUtils.getVersion(this);
	}

	/** Closes the database and deletes the underlying folder. */
	public void delete() throws Exception {
		if (!closed)
			close();
		if (folder != null)
			delete(folder);
	}

	private void delete(File folder) {
		log.trace("delete folder {}", folder);
		for (File f : folder.listFiles()) {
			if (f.isDirectory())
				delete(f);
			f.delete();
		}
		boolean b = folder.delete();
		log.trace("folder {} deleted? -> {}", folder, b);
	}

}
