package play.db.jpa;

import javax.persistence.*;

import org.hibernate.ejb.Ejb3Configuration;
import play.Invoker;
import play.exceptions.JPAException;

/**
 * JPA Support for a specific JPA/DB configuration
 *
 * dbConfigName corresponds to properties-names in application.conf.
 *
 * The default DBConfig is the one configured using 'db.' in application.conf
 *
 * dbConfigName = 'other' is configured like this:
 *
 * db_other = mem
 * db_other.user = batman
 *
 *
 * A particular JPAConfig-instance uses the DBConfig with the same configName
 */

public class JPAConfig {
    private EntityManagerFactory entityManagerFactory = null;
    private ThreadLocal<JPAContext> local = new ThreadLocal<JPAContext>();
    public final JPQL jpql;

    protected JPAConfig(Ejb3Configuration cfg) {
        entityManagerFactory = cfg.buildEntityManagerFactory();
        jpql = new JPQL(this);
    }

    protected void close() {
        if (isEnabled()) {
            entityManagerFactory.close();
            entityManagerFactory = null;
        }
    }

    /**
     * @return true if an entityManagerFactory has started
     */
    public boolean isEnabled() {
        return entityManagerFactory != null;
    }

    /*
     * Build a new entityManager.
     * (In most case you want to use the local entityManager with em)
     */
    public EntityManager newEntityManager() {
        return entityManagerFactory.createEntityManager();
    }

    /**
     * gets the active or create new
     * @return the active JPAContext bound to current thread
     */
    public JPAContext getJPAContext() {
        return getJPAContext(null);
    }


    /**
     * gets the active or create new. manualReadOnly is only used if we're create new context
     * @param manualReadOnly is not null, this value is used instead of value from @Transactional.readOnly
     * @return the active JPAContext bound to current thread
     */
    protected JPAContext getJPAContext(Boolean manualReadOnly) {
        JPAContext context = local.get();
        if ( context == null) {
            // throw new JPAException("The JPAContext is not initialized. JPA Entity Manager automatically start when one or more classes annotated with the @javax.persistence.Entity annotation are found in the application.");

            // This is the first time someone tries to use JPA in this thread.
            // we must initialize it

            if(Invoker.InvocationContext.current().getAnnotation(NoTransaction.class) != null ) {
                //Called method or class is annotated with @NoTransaction telling us that
                //we should not start a transaction
                throw new JPAException("Cannot create JPAContext due to @NoTransaction");
            }

            boolean readOnly = false;
            if (manualReadOnly!=null) {
                readOnly = manualReadOnly;
            } else {
                Transactional tx = Invoker.InvocationContext.current().getAnnotation(Transactional.class);
                if (tx != null) {
                    readOnly = tx.readOnly();
                }
            }
            context = new JPAContext(this, readOnly, JPAPlugin.autoTxs);

            local.set(context);
        }
        return context;
    }

    protected void clearJPAContext() {
        JPAContext context = local.get();
        if (context != null) {
            try {
                context.close();
            } catch(Exception e) {
                // Let's it fail
            }
            local.remove();
        }
    }

    /**
     * @return true if JPA is enabled in current thread
     */
    public boolean threadHasJPAContext() {
        return local.get() != null;
    }

    public boolean isInsideTransaction() {
        if (!threadHasJPAContext()) {
            return false;
        }
        return getJPAContext().isInsideTransaction();
    }
}
