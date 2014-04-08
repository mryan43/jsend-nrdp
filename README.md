jsend-nrdp
==========

jsend-nrdp is a java library for submitting Nagios passive check results to a remote NRPD server.

Usage
=====

For maven users : 
´´´
<dependency>
	<groupId>ch.shamu</groupId>
	<artifactId>jsend-nrdp</artifactId>
	<version>${jsend-nrdp.version}</version>
</dependency>
´´´

Configuration (this example uses Spring, but you can use any mean of bean creation)
´´´
<bean id="nrdp-connection-settings" class="ch.shamu.jsendnrdp.NRDPServerConnectionSettings">
	<constructor-arg name="url" value="${nrdp.url}"/>
	<constructor-arg name="token" value="${nrdp.token}"/>
	<constructor-arg name="timeout" value="${nrdp.timeout}"/>
</bean>
´´´

Synchronous mode : NagiosCheckSenderImpl
This implementation waits for the NRDP server to respond before returning from the "send" method, giving meaningful details about the failure in case of a problem
´´´
TODO : example usage of synchronous mode
´´´

Async mode : NonBlockingNagiosCheckSender
This implementation uses a java Executor to process the "send" in a separate thread. It returns after adding the job in the executor queue, details about errors are logged

´´´
NagiosCheckSender resultSender = new NonBlockingNagiosCheckSender(nrdpConnectionSettings, CONCURRENCY_LEVEL, MAX_BACKLOG_SIZE);
NagiosCheckResult resultToSend = new NagiosCheckResult(host, serviceName, serviceState, notif.getMessage());
Collection<NagiosCheckResult> resultsToSend = Lists.newArrayList();
resultsToSend.add(resultToSend);
try {
	resultSender.send(resultsToSend);
}
catch (Exception e) {
	// we use the non blocking implementation of the nagios check sender, so this should actually never happen
	logger.error("Error sending check result to nagios", e);
}
´´´



