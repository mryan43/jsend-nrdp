jsend-nrdp
==========

jsend-nrdp is a java library for submitting Nagios passive check results to a remote NRDP server.

Maven
=====
```xml
<dependency>
  <groupId>ch.shamu</groupId>
  <artifactId>jsend-nrdp</artifactId>
  <version>${jsend-nrdp.version}</version>
</dependency>
```

Configuration
=============
this example uses Spring, but you can use any mean of bean creation.
```xml
<bean id="nrdp-connection-settings" class="ch.shamu.jsendnrdp.NRDPServerConnectionSettings">
	<constructor-arg name="url" value="${nrdp.url}"/>
	<constructor-arg name="token" value="${nrdp.token}"/>
	<constructor-arg name="timeout" value="${nrdp.timeout}"/>
</bean>
```

Async sending of check results
==============================
The NonBlockingNagiosCheckSender implementation uses a java Executor to process the "send" in a separate thread. It returns after adding the job in the executor queue, eventual details about errors are logged.

```java
NagiosCheckSender resultSender = new NonBlockingNagiosCheckSender(nrdpConnectionSettings, CONCURRENCY_LEVEL, MAX_BACKLOG_SIZE);
NagiosCheckResult resultToSend = new NagiosCheckResult(host, serviceName, serviceState, notif.getMessage());
Collection<NagiosCheckResult> resultsToSend = Lists.newArrayList();
resultsToSend.add(resultToSend);
try {
	resultSender.send(resultsToSend);
}
catch (Exception e) {
	logger.error("Error sending check result to nagios", e);
}
```



