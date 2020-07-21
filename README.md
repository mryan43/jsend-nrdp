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
```java
@Configuration
public class NagiosNRDPContext {

	@Value("${nrdp.url}")
	private String url;

	@Value("${nrdp.token}")
	private String token;

	@Value("${nrdp.timeout:2000}")
	private int timeout;

	@Value("${nrdp.threads:2}")
	private int threads;

	@Value("${nrdp.max.queue.size:10000}")
	private int maxQueueSize;
	
	@Value("${nrdp.max.requests.per.second:20}")
	private double maxRequestsPerSecond;
	
	@Bean(destroyMethod = "shutdown")
	public NonBlockingNagiosCheckSender nagiosCheckSender(){

		NRDPServerConnectionSettings settings = new NRDPServerConnectionSettings(url, token, timeout);
		return new NonBlockingNagiosCheckSender(settings, threads, maxQueueSize, maxRequestsPerSecond);
	}

}
```

Async sending of check results
==============================
The NonBlockingNagiosCheckSender implementation uses a pool of background threads to process the http request to nagios. 
 The "send" method will run in the background and log any exception.

```java
NagiosCheckSender resultSender = new NonBlockingNagiosCheckSender(nrdpConnectionSettings, CONCURRENCY_LEVEL, MAX_BACKLOG_SIZE);
NagiosCheckResult resultToSend = new NagiosCheckResult(host, serviceName, serviceState, notif.getMessage());
try {
	resultSender.send(Collections.singletonList(resultToSend));
}
catch (IOException e) { // might happen if send queue is full
	log.error("Could not send check result to Nagios", e);
}
```

 
The "sendAsync" method returns a CompletableFuture that allows you to handle the completion of the http request to Nagios (either normal or exceptionnal)


```java
NagiosCheckSender resultSender = new NonBlockingNagiosCheckSender(nrdpConnectionSettings, CONCURRENCY_LEVEL, MAX_BACKLOG_SIZE);
NagiosCheckResult resultToSend = new NagiosCheckResult(host, serviceName, serviceState, notif.getMessage());
try {
    resultSender.sendAsync(Collections.singletonList(resultToSend)).exceptionally(e -> {
      log.error("Error while sending check result to Nagios", e);
    });
}
catch (IOException e) { // might happen if send queue is full
	log.error("Could not send check result to Nagios", e);
}
```

