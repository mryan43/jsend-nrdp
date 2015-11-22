package ch.shamu.jsendnrdp.impl;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.shamu.jsendnrdp.NRDPException;
import ch.shamu.jsendnrdp.NRDPServerConnectionSettings;
import ch.shamu.jsendnrdp.NagiosCheckSender;
import ch.shamu.jsendnrdp.domain.NagiosCheckResult;

import com.google.common.util.concurrent.RateLimiter;

/**
 * <pre>
 * This implementation uses a bounded queue of alerts to send (jobs). 
 * A ThreadPoolExecutor processes those jobs. 
 * Due to the asynchronous nature of this sender, the only exception that can be thrown by the "send" 
 * method is if the maxQueueSize is reached (IOException). All exception which can occur during job execution are logged.
 * This implementation features a configurable level of concurrency and throttling of job executions.
 * This allows to protect the remote nagios server if an application tries to send too many check results too fast.
 * </pre>
 * @author mryan
 */
public class NonBlockingNagiosCheckSender implements NagiosCheckSender {

	private final static Logger logger = LoggerFactory.getLogger(NonBlockingNagiosCheckSender.class);

	private final int maxQueueSize;
	private final ThreadPoolExecutor executor;
	private final NagiosCheckSender sender;
	private final RateLimiter rateLimiter;

	/**
	 * Bean that knows how to send nagios alerts in a non blocking way, has configurable concurrency level and supports throttling
	 * @param server is the nrdp server connection settings
	 * @param nbThreads is the number of worker threads for sending nagios alerts (concurrency level)
	 * @param maxQueueSize is the maximum number of queued jobs before starting rejecting new job requests (IOException) (0 -> go on until
	 *            OutOfMemory) jobs currently in execution are not taken into account when computing queue size.
	 * 
	 * <pre>
	 * Note that this version does not throttle job executions.
	 * 
	 *            <pre>
	 */
	public NonBlockingNagiosCheckSender(NRDPServerConnectionSettings server, int nbThreads, int maxQueueSize) {
		this(server, nbThreads, maxQueueSize, 0d);
	}

	/**
	 * Bean that knows how to send nagios alerts in a non blocking way, has configurable concurrency level and supports throttling
	 * @param server is the nrdp server connection settings
	 * @param nbThreads is the number of worker threads for sending nagios alerts (concurrency level)
	 * @param maxQueueSize is the maximum number of queued jobs before starting rejecting new job requests (IOException) (0 -> queue jobs until
	 *            OutOfMemory, please don't...) jobs currently in execution are not taken into account when computing queue size.
	 * @param maxRequestRate throttling of requests sent to the server, it's the maximum number of requests send to the server per second (0 ->
	 *            unlimited). The jobs currently in execution will block in order to respect this rate.
	 */
	public NonBlockingNagiosCheckSender(NRDPServerConnectionSettings server, int nbThreads, int maxQueueSize, double maxRequestsPerSeconds) {
		this.sender = new NagiosCheckSenderImpl(server);

		this.executor = new ScheduledThreadPoolExecutor(nbThreads, new ThreadFactory() {
			private int count = 0;

			public Thread newThread(Runnable r) {
				// pretty naming of threads
				return new Thread(r, "nrdp-sender" + "-" + count++);
			}
		});
		this.maxQueueSize = maxQueueSize;
		if (maxRequestsPerSeconds == 0d) {
			this.rateLimiter = RateLimiter.create(Double.MAX_VALUE); // FIRE AT WILL !
		} else {
			this.rateLimiter = RateLimiter.create(maxRequestsPerSeconds);
		}

	}

	public void send(Collection<NagiosCheckResult> checkResults) throws NRDPException, IOException {
		// deal with binding of the queue
		if (maxQueueSize > 0 && executor.getQueue().size() >= maxQueueSize) {
			throw new IOException("Nagios check result could not be submitted : maximum number of queued results to send reached ("
					+ maxQueueSize + ")");
		}
		executor.execute(new NonBlockingSender(checkResults));
	}

        /**
         * Shuts down the underlying executor. No new results should be sent
         * through this sender after this method is invoked.
         */
        public void shutdown() {
            executor.shutdown();
        }

	private class NonBlockingSender implements Runnable {

		private Collection<NagiosCheckResult> results;

		public NonBlockingSender(Collection<NagiosCheckResult> results) {
			this.results = results;
		}

		public void run() {
			try {
				double waitTime = rateLimiter.acquire(); // Eventually wait because of throttling
				if (waitTime > 0) {
					logger.debug("job throttling wait : {}", waitTime);
				}
				sender.send(results);
			}
			catch (Exception e) {
				logger.error("Problem sending nagios check result to NRDP server: ", e);
			}
		}
	}

}
