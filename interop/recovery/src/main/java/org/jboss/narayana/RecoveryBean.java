package org.jboss.narayana;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import com.arjuna.ats.arjuna.recovery.RecoveryManager;
import com.arjuna.ats.arjuna.recovery.RecoveryModule;
import com.arjuna.ats.jta.recovery.SerializableXAResourceDeserializer;
import com.arjuna.ats.jta.recovery.XAResourceRecoveryHelper;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Enumeration;
import java.util.Vector;

import org.jboss.narayana.DummyXAResource;

@Singleton
@Startup
public class RecoveryBean implements RecoveryRegistration {
    static private final String EE_TM = "java:/TransactionManager";
    static private final String GF_TM = "java:appserver/TransactionManager";
    static private final String WL_TM = "weblogic.transaction.TransactionManager";
    private static final String XAR_NAME = "Dummy";

    private boolean registered;
    private boolean isWF;
    private SerializableXAResourceDeserializer deserializer;
    private XAResourceRecoveryHelper xarHelper;

    @PostConstruct
    private void init() {
        System.out.printf("registering RecoveryBean%n");
        isWF = System.getProperty("jboss.node.name") != null;

        if (isWF) {
            deserializer = new SerializableXAResourceDeserializer() {
                @Override
                public boolean canDeserialze(String className) {
                    return true;
                }

                @Override
                public XAResource deserialze(ObjectInputStream ois) throws IOException, ClassNotFoundException {
                    return new DummyXAResource();
                }
            };

            xarHelper = new XAResourceRecoveryHelper() {
                public boolean initialise(String p) throws Exception {
                    return true;
                }

                public XAResource[] getXAResources() throws Exception {
                    return new XAResource[]{new DummyXAResource()};
                }
            };
        }
        register();
    }

    @PreDestroy
    private void fini() {
        System.out.printf("unregistering RecoveryBean%n");
        unregister();
    }

    public void register() {
        try {
            registerRecoveryResources(isWF);
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void unregister() {
        System.out.printf("register registered=%b%n", registered);
        if (!registered)
            return;

        if (isWF) {
            RecoveryManager manager = RecoveryManager.manager(RecoveryManager.INDIRECT_MANAGEMENT);
            Vector recoveryModules = manager.getModules();

            if (recoveryModules != null) {
                Enumeration modules = recoveryModules.elements();

                while (modules.hasMoreElements()) {
                    RecoveryModule m = (RecoveryModule) modules.nextElement();

                    if (m instanceof com.arjuna.ats.internal.jta.recovery.jts.XARecoveryModule) {
                        com.arjuna.ats.internal.jta.recovery.jts.XARecoveryModule xarm = (com.arjuna.ats.internal.jta.recovery.jts.XARecoveryModule) m;
                        // no deserializer remove op
                        xarm.removeXAResourceRecoveryHelper(xarHelper);
                    } else if (m instanceof com.arjuna.ats.internal.jta.recovery.arjunacore.XARecoveryModule) {
                        com.arjuna.ats.internal.jta.recovery.arjunacore.XARecoveryModule xarm = (com.arjuna.ats.internal.jta.recovery.arjunacore.XARecoveryModule) m;
                        // no deserializer remove op
                        xarm.removeXAResourceRecoveryHelper(xarHelper);
                    }
                }
            }
        }

        registered = false;
    }

    private void registerRecoveryResources(boolean isWF) throws NamingException {
        if (isWF) {
            RecoveryManager manager = RecoveryManager.manager(RecoveryManager.INDIRECT_MANAGEMENT);
            Vector recoveryModules = manager.getModules();

            if (recoveryModules != null) {
                Enumeration modules = recoveryModules.elements();

                while (modules.hasMoreElements()) {
                    RecoveryModule m = (RecoveryModule) modules.nextElement();

                    if (m instanceof com.arjuna.ats.internal.jta.recovery.jts.XARecoveryModule) {
                        com.arjuna.ats.internal.jta.recovery.jts.XARecoveryModule xarm = (com.arjuna.ats.internal.jta.recovery.jts.XARecoveryModule) m;

                        xarm.addSerializableXAResourceDeserializer(deserializer);
                        xarm.addXAResourceRecoveryHelper(xarHelper);
                    } else if (m instanceof com.arjuna.ats.internal.jta.recovery.arjunacore.XARecoveryModule) {
                        com.arjuna.ats.internal.jta.recovery.arjunacore.XARecoveryModule xarm = (com.arjuna.ats.internal.jta.recovery.arjunacore.XARecoveryModule) m;

                        xarm.addSerializableXAResourceDeserializer(deserializer);
                        xarm.addXAResourceRecoveryHelper(xarHelper);
                    }
                }
            }
        } else {
            TransactionManager tm = (TransactionManager) new InitialContext().lookup(GF_TM);
            //ResourceRecoveryManagerImpl.registerRecoveryResourceHandler(xaResource);
            try {
                // com.sun.enterprise.transaction.api.JavaEETransactionManager
                java.lang.reflect.Method method = tm.getClass().getMethod("registerRecoveryResourceHandler", XAResource.class);
                method.invoke(tm, new DummyXAResource());
            } catch (Exception e) {
                System.out.printf("Error registering recovery resources: %s%n", e.getMessage());
                e.printStackTrace();
            }
        } //  (TransactionManager) new InitialContext().lookup(WL_TM).registerStaticResource(XAR_NAME, new DummyXAResource());
    }
}
