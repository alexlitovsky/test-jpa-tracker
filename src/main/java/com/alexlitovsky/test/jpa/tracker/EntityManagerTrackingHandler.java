package com.alexlitovsky.test.jpa.tracker;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;

import jakarta.persistence.EntityManager;

/**
 * {@link java.lang.reflect.InvocationHandler} that tracks entities passed to
 * {@code persist}, {@code merge}, and {@code remove}, and deletes them when
 * {@code close} is called.
 *
 * @see EntityManagerTracker
 */
class EntityManagerTrackingHandler implements InvocationHandler {

	/**
	 * Identifies a persistent entity by its type and primary key.
	 */
	private static record EntityDescriptor(Class<?> type, Object id) {}

	private final EntityManager delegate;
	private final LinkedList<EntityDescriptor> insertedEntityDescriptors;
	
	EntityManagerTrackingHandler(EntityManager delegate) {
		super();
		this.delegate = delegate;
		this.insertedEntityDescriptors = new LinkedList<>();
	}

	/**
	 * Intercepts {@code persist}, {@code merge}, {@code remove}, and {@code close}
	 * calls to maintain the tracking list and trigger cleanup, then delegates to the
	 * real {@code EntityManager}.
	 *
	 * @param proxy  the proxy instance
	 * @param method the intercepted method
	 * @param args   the method arguments
	 * @return the value returned by the delegate
	 * @throws Throwable any exception thrown by the delegate
	 */
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

		Object result;
		
		try {

			switch (method.getName()) {
				case "persist" -> {
					result = method.invoke(delegate, args);
					EntityDescriptor persistDescriptor = descriptorOf(args[0]);
					if (!insertedEntityDescriptors.contains(persistDescriptor)) {
						insertedEntityDescriptors.add(persistDescriptor);
					}
				}
				case "merge" -> {
					result = method.invoke(delegate, args);
					EntityDescriptor mergeDescriptor = descriptorOf(result);
					if (!insertedEntityDescriptors.contains(mergeDescriptor)) {
						insertedEntityDescriptors.add(mergeDescriptor);
					}
				}
				case "remove" -> {
					EntityDescriptor descriptor = descriptorOf(args[0]);
					insertedEntityDescriptors.remove(descriptor);
					result = method.invoke(delegate, args);
				}
				case "close" -> {
					runInTransaction(this::removeTrackedEntities);
					result = method.invoke(delegate, args);
				}
				default -> result = method.invoke(delegate, args);
			}
		}
		catch (InvocationTargetException e) {
			throw e.getCause();
		}
		
		return result;
	}
	
	private EntityDescriptor descriptorOf(Object entity) {
		return new EntityDescriptor(
				entity.getClass(),
				delegate.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entity));
	}
	
	private void removeTrackedEntities() {
		for (EntityDescriptor descriptor : insertedEntityDescriptors.reversed()) {
			Object entity = delegate.find(descriptor.type, descriptor.id);
			if (entity != null) {
				delegate.remove(entity);
			}
		}
	}
	
	private void runInTransaction(Runnable task) {		
		delegate.getTransaction().begin();
		try {
			task.run();
			delegate.getTransaction().commit();
		}
		catch (RuntimeException e) {
			delegate.getTransaction().rollback();
			throw e;
		}
	}
}