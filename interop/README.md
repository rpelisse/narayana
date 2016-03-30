
The following two result in both resources commiting after restarting wildfly:

curl http://localhost:7080/ejbtest/rs/remote/3528/wf/haltafter
curl http://localhost:7080/ejbtest/rs/remote/3528/wf/haltbefore

The following two fail:

curl http://localhost:8080/ejbtest/rs/remote/3700/gf/haltafter
  domain1: DummyXAResource: prepare called]] 
  server1: 3:58:12,090 INFO  [stdout] (default task-6) DummyXAResource: commit called
           top level coordinator gets moved to AssumedCompleteTransaction
           (which is wrong since xar is still prepared on domain1)






curl http://localhost:8080/ejbtest/rs/remote/3700/gf/haltbefore


curl http://localhost:8080/ejbtest/rs/remote/3700/gf/x                 
curl http://localhost:7080/ejbtest/rs/remote/3528/wf/h


asadmin create-domain --adminport 4948 domain2
 ./d.sh -a gf1 -f /home/mmusgrov/source/misc/mvc/javaee8-mvc-sample/time/target/time.war
 ./d.sh -a gf1 -f recovery/target/dummy-resource-recovery.jar 
 ./d.sh -a gf1 -f interop/target/ejbtest.war

 ./d.sh -a wf1 -f recovery/target/dummy-resource-recovery.jar 
 ./d.sh -a wf1 -f interop/target/ejbtest.war


 mvn install # build war
 ./d.sh -a gf -f target/ejbtest.war # deploy to both glassfish servers
 ./d.sh -a wf -f target/ejbtest.war # deploy to both wildfly servers
 ./d.sh -t gfgf # gf -> gf transaction propagation (passes)
 ./d.sh -t wfwf # wf -> wf transaction propagation (passes)
 ./d.sh -t wfgf # wf -> gf transaction propagation (passes)
 ./d.sh -t gfwf # gf -> wf transaction propagation (fails)

# or
curl http://localhost:7080/ejbtest/rs/remote/3700/gf/x # gf -> gf (invoke ejb on domain1 which uses jndi port 3700 to invoke domain2)
curl http://localhost:8080/ejbtest/rs/remote/3728/wf/x # wf -> wf (invoke ejb on server1 which uses jndi port 3728 to invoke server2)
curl http://localhost:8080/ejbtest/rs/remote/7001/gf/x # wf -> gf (invoke ejb on server1 which uses jndi port 7001 to invoke domain1)
curl http://localhost:7080/ejbtest/rs/remote/3528/wf/x # gf -> wf (invoke ejb on domain1 which uses jndi port 3528 to invoke server1)

# start two glassfish serves
asadmin start-domain domain1 # admin port: 4848 iiop port: 7001 http port: 7080
asadmin start-domain domain2 # admin port: 4948 iiop port: 3700 http port: 8080

asadmin start-domain --debug domain1 # jdb -attach 9009

# start two wildfly serves
cd /home/mmusgrov/source/forks/wildfly/wildfly.interop/build/target/wildfly-10.0.0.CR3-SNAPSHOT
./bin/standalone.sh -c standalone-full.xml
cd /home/mmusgrov/source/forks/wildfly/wildfly.interop/build/target/wildfly-10.0.0.CR3-SNAPSHOT-2
# set port offset property in standalone-full.xml (<property name="jboss.socket.binding.port-offset" value="200"/>)
./bin/standalone.sh -c standalone-full.xml


asadmin set server1.transaction-service.automatic-recovery=false

# app dev guide
https://docs.oracle.com/cd/E18930_01/html/821-2418/beafd.html
https://docs.oracle.com/cd/E19798-01/821-1751/ablsn/index.html

Reference Manual: https://docs.oracle.com/cd/E26576_01/doc.312/e24938/toc.htm
https://docs.oracle.com/cd/E26576_01/doc.312/e24928/transactions.htm#GSADG00606

asadmin --port 4948
asadmin> get --monitor inst1.server.transaction-service.activeids-current
remote failure: No monitoring data to report.

