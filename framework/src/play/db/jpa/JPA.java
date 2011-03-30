package play.db.jpa;

import javax.persistence.*;

import org.hibernate.ejb.Ejb3Configuration;
import play.db.DB;
import play.db.DBConfig;
import play.exceptions.JPAException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * JPA Support
 *
 * This class holds reference to all JPA configurations.
 * Each configuration has its own instance of JPAConfig.
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
 * This class also preserves backward compatibility by
 * directing static methods to the default JPAConfig-instance
 *
 * A particular JPAConfig-instance uses the DBConfig with the same configName
 */
public class JPA {

    /**
     * Holds ref to the default jpa config named defaultJPAConfigName
     */
    private static JPAConfig defaultJPAConfig = null;
    private final static Map<String, JPAConfig> jpaConfigs = new HashMap<String, JPAConfig>(1);

    protected static void addConfiguration(String configName, Ejb3Configuration cfg) {
        JPAConfig jpaConfig = new JPAConfig(cfg);
        jpaConfigs.put(configName, jpaConfig);
        if( DBConfig.defaultDbConfigName.equals(configName)) {
            defaultJPAConfig = jpaConfig;
            JPQL.createSingleton();
        }
    }

    public static JPAConfig getJPAConfig(String jpaConfigName) {
        return getJPAConfig(jpaConfigName, false);
    }

    public static JPAConfig getJPAConfig(String jpaConfigName, boolean ignoreError) {
        JPAConfig jpaConfig = jpaConfigs.get(jpaConfigName);
        if (jpaConfig==null && !ignoreError) {
            throw new JPAException("No JPAConfig is found with the name " + jpaConfigName);
        }
        return jpaConfig;
    }

    protected static void close() {
        if (isEnabled()) {
            for( JPAConfig jpaConfig : jpaConfigs.values()) {
                jpaConfig.close();
            }
            jpaConfigs.clear();
            defaultJPAConfig = null;
        }
    }

    /**
     * clear current JPA context and transaction
     * @param rollback shall current transaction be committed (false) or cancelled (true)
     */
    public static void closeTx(boolean rollback) {
        for (JPAConfig jpaConfig : jpaConfigs.values()) {
            if (jpaConfig.threadHasJPAContext()) {
                jpaConfig.getJPAContext().closeTx(rollback);
            }
        }
    }
    
    /*
     * Retrieve the current entityManager
     */ 
    public static EntityManager em() {
        return defaultJPAConfig.getJPAContext().em();
    }
    
    /*
     * Tell to JPA do not commit the current transaction
     */ 
    public static void setRollbackOnly() {
        defaultJPAConfig.getJPAContext().em().getTransaction().setRollbackOnly();
    }

    /**
     * @return true if an entityManagerFactory has started
     */
    public static boolean isEnabled() {
        return !jpaConfigs.isEmpty();
    }

    /**
     * Execute a JPQL query
     */
    public static int execute(String query) {
        return defaultJPAConfig.getJPAContext().em().createQuery(query).executeUpdate();
    }

    /*
     * Build a new entityManager.
     * (In most case you want to use the local entityManager with em)
     */ 
    public static EntityManager newEntityManager() {
        return defaultJPAConfig.newEntityManager();
    }

    /**
     * @return true if current thread is running inside a transaction
     */
    public static boolean isInsideTransaction() {
        return defaultJPAConfig.isInsideTransaction();
    }

    protected static void clear() {
        for (JPAConfig jpaConfig : jpaConfigs.values()) {
            if (jpaConfig.threadHasJPAContext()) {
                jpaConfig.getJPAContext().em().clear();
            }
        }
    }

    protected static void clearJPAContext() {
        for (JPAConfig jpaConfig : jpaConfigs.values()) {
            jpaConfig.clearJPAContext();
        }
    }
}
