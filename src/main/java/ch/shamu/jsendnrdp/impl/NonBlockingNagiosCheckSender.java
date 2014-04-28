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

/**
 * {@inheritDoc}
 * 
 * <pre>
 * This implementation uses a job queue of alerts to send. 
 * A ThreadPoolExecutor then sends the alerts from this queue. 
 * Due to the asynchronous nature of this sender, the only exception that can be thrown by the "send" 
 * method is if the maxQueueSize is reached (IOException).
 * Errors during processing are logged with an ERROR level
 * The executor is configurable, if none is provided, a ScheduledThreadPoolExecutor with 4 threads will be created
 * The send method can throw an
 * </pre>
 * @author mryan
 */
public class NonBlockingNagiosCheckSender implements NagiosCheckSender {

	private final static Logger logger = LoggerFactory.getLogger(NonBlockingNagiosCheckSender.class);

	private final int maxQueueSize;
	private final ThreadPoolExecutor executor;
	private final NagiosCheckSender sender;

	/**
	 * Bean that knows how to send nagios alerts in a non blocking way
	 * @param server is the nrdp server connection settings
	 * @param nbThreads is the number of worker threads for sending nagios alerts
	 * @param maxQueueSize is the maximum number of queued alerts to send before rejecting jobs (
	 */
	public NonBlockingNagiosCheckSender(NRDPServerConnectionSettings server, int nbThreads, int maxQueueSize) {
		this.sender = new NagiosCheckSenderImpl(server);

		this.executor = new ScheduledThreadPoolExecutor(nbThreads, new ThreadFactory() {
			private int count = 0;

			public Thread newThread(Runnable r) {
				// pretty naming of threads
				return new Thread(r, "nrdp-sender" + "-" + count++);
			}
		});
		this.maxQueueSize = maxQueueSize;
	}

	public void send(Collection<NagiosCheckResult> checkResults) throws NRDPException, IOException {
		// artificially put a boundary on the executor queue.
		if (executor.getQueue().size() >= maxQueueSize) {
			throw new IOException("Nagios check result could not be submitted : maximum number of queued results to send reached ("
					+ maxQueueSize + ")");
		}
		executor.execute(new NonBlockingSender(checkResults));
	}

	private class NonBlockingSender implements Runnable {

		private Collection<NagiosCheckResult> results;

		public NonBlockingSender(Collection<NagiosCheckResult> results) {
			this.results = results;
		}

		public void run() {
			try {
				sender.send(results);
			}
			catch (Exception e) {
				logger.error("Problem sending nagios check result to NRDP server: ", e);
			}
		}
	}

}
