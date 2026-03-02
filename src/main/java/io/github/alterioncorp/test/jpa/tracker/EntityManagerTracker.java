package io.github.alterioncorp.test.jpa.tracker;

import java.lang.reflect.Proxy;

import jakarta.persistence.EntityManager;

/**
 * Factory for creating a tracking {@link EntityManager} proxy.
 *
 * <p>The proxy records entities passed to {@code persist}, {@code merge}, and
 * {@code remove}, and deletes all tracked entities when {@code close} is called.
 *
 * <p>Intended for use in tests where inserted data must be rolled back without
 * relying on transaction rollback (e.g. when the code under test commits its own
 * transaction).
 */
public class EntityManagerTracker {

	private EntityManagerTracker() {}

	/**
	 * Creates a tracking proxy wrapping the given {@link EntityManager}.
	 * Tracked entities are automatically deleted when the proxy is {@code close}d.
	 *
	 * @param entityManager the real {@code EntityManager} to wrap
	 * @return a proxied {@code EntityManager} that tracks persistence operations
	 */
	public static EntityManager proxy(EntityManager entityManager) {

		EntityManagerTrackingHandler trackingHandler = new EntityManagerTrackingHandler(entityManager);

		return (EntityManager) Proxy.newProxyInstance(
				Thread.currentThread().getContextClassLoader(),
				new Class[] { EntityManager.class },
				trackingHandler);
	}
}
