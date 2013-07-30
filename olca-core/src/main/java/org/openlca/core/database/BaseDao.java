package org.openlca.core.database;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseDao<T> implements IDao<T> {

	/**
	 * A database dependent field for the maximum size of lists in JPQL queries.
	 */
	static final int MAX_LIST_SIZE = 1000;

	protected Class<T> entityType;
	protected Logger log = LoggerFactory.getLogger(this.getClass());
	private IDatabase database;

	public BaseDao(Class<T> entityType, IDatabase database) {
		this.entityType = entityType;
		this.database = database;
	}

	protected IDatabase getDatabase() {
		return database;
	}

	@Override
	public boolean contains(long id) {
		return getForId(id) != null;
	}

	@Override
	public void delete(T entity) {
		if (entity == null)
			return;
		EntityManager em = createManager();
		try {
			em.getTransaction().begin();
			em.remove(em.merge(entity));
			em.getTransaction().commit();
		} finally {
			em.close();
		}
	}

	@Override
	public void deleteAll(Collection<T> entities) {
		if (entities == null)
			return;
		EntityManager em = createManager();
		try {
			em.getTransaction().begin();
			for (T entity : entities) {
				log.trace("About to remove entity {}", entity);
				em.remove(em.merge(entity));
			}
			em.getTransaction().commit();
		} finally {
			em.close();
		}
	}

	@Override
	public T update(T entity) {
		if (entity == null)
			return null;
		EntityManager em = createManager();
		try {
			em.getTransaction().begin();
			T retval = em.merge(entity);
			em.getTransaction().commit();
			return retval;
		} finally {
			em.close();
		}
	}

	@Override
	public T insert(T entity) {
		if (entity == null)
			return null;
		EntityManager em = createManager();
		try {
			em.getTransaction().begin();
			em.persist(entity);
			em.getTransaction().commit();
			return entity;
		} finally {
			em.close();
		}
	}

	@Override
	public T getForId(long id) {
		log.trace("get {} for id={}", entityType, id);
		EntityManager entityManager = createManager();
		try {
			T o = entityManager.find(entityType, id);
			return o;
		} finally {
			entityManager.close();
		}
	}

	@Override
	public List<T> getForIds(Set<Long> ids) {
		if (ids == null || ids.isEmpty())
			return Collections.emptyList();
		if (ids.size() <= MAX_LIST_SIZE)
			return fetchForIds(ids);
		return fetchForChunkedIds(ids);
	}

	private List<T> fetchForChunkedIds(Set<Long> ids) {
		List<Long> rest = new ArrayList<>(ids);
		List<T> results = new ArrayList<>();
		while (!rest.isEmpty()) {
			int toPos = rest.size() > MAX_LIST_SIZE ? MAX_LIST_SIZE : rest
					.size();
			List<Long> nextChunk = rest.subList(0, toPos);
			List<T> chunkResults = fetchForIds(nextChunk);
			results.addAll(chunkResults);
			nextChunk.clear(); // clears also the elements in rest
		}
		return results;
	}

	private List<T> fetchForIds(Collection<Long> ids) {
		EntityManager em = createManager();
		try {
			String jpql = "SELECT o FROM " + entityType.getSimpleName()
					+ " o WHERE o.id IN :ids";
			TypedQuery<T> query = em.createQuery(jpql, entityType);
			query.setParameter("ids", ids);
			return query.getResultList();
		} finally {
			em.close();
		}
	}

	@Override
	public List<T> getAll() {
		log.debug("Select all for class {}", entityType);
		EntityManager em = createManager();
		try {
			String jpql = "SELECT o FROM ".concat(entityType.getSimpleName())
					.concat(" o");
			TypedQuery<T> query = em.createQuery(jpql, entityType);
			List<T> results = query.getResultList();
			log.debug("{} results", results.size());
			return results;
		} finally {
			em.close();
		}
	}

	@Override
	public List<T> getAll(String jpql, Map<String, ? extends Object> parameters) {
		EntityManager em = createManager();
		try {
			TypedQuery<T> query = em.createQuery(jpql, entityType);
			for (String param : parameters.keySet()) {
				query.setParameter(param, parameters.get(param));
			}
			List<T> results = query.getResultList();
			return results;
		} finally {
			em.close();
		}
	}

	@Override
	public T getFirst(String jpql, Map<String, ? extends Object> parameters) {
		List<T> list = getAll(jpql, parameters);
		if (list.isEmpty())
			return null;
		return list.get(0);
	}

	@Override
	public long getCount(String jpql, Map<String, Object> parameters) {
		EntityManager em = createManager();
		try {
			TypedQuery<Long> query = em.createQuery(jpql, Long.class);
			for (String param : parameters.keySet()) {
				query.setParameter(param, parameters.get(param));
			}
			Long count = query.getSingleResult();
			return count == null ? 0 : count;
		} finally {
			em.close();
		}
	}

	@Override
	public void deleteAll() {
		log.trace("delete all instances of {}", entityType);
		deleteAll(getAll());
	}

	protected EntityManager createManager() {
		EntityManager em = getDatabase().getEntityFactory()
				.createEntityManager();
		return em;
	}

	protected Query query() {
		return Query.on(database);
	}

	public T detach(T val) {
		EntityManager em = createManager();
		try {
			em.detach(val);
			return val;
		} catch (Exception e) {
			log.error("detaching entity failed ", e);
			return val;
		}
	}

}
