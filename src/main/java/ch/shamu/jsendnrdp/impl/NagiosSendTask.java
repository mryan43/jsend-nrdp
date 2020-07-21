package ch.shamu.jsendnrdp.impl;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.RateLimiter;

import ch.shamu.jsendnrdp.NagiosCheckSender;
import ch.shamu.jsendnrdp.domain.NagiosCheckResult;

/**
 * Instances of this class are used to send .
 */
public class NagiosSendTask implements Runnable {

	private final static Logger logger = LoggerFactory.getLogger(NagiosSendTask.class);

	private final Collection<NagiosCheckResult> results;
	private final NagiosCheckSender sender;
	private final RateLimiter rateLimiter;
	private final CompletableFuture<Collection<NagiosCheckResult>> completableFuture = new CompletableFuture<>();

	public NagiosSendTask(Collection<NagiosCheckResult> results, NagiosCheckSender sender, RateLimiter rateLimiter) {
		this.results = results;
		this.sender = sender;
		this.rateLimiter = rateLimiter;
	}

	public void run() {
		try {
			double waitTime = rateLimiter.acquire(); // Eventually wait because of throttling
			if (waitTime > 0) {
				logger.debug("task throttling wait : {}", waitTime);
			}
			sender.send(results);
			completableFuture.complete(results);
		}
		catch (Throwable e) {
			completableFuture.completeExceptionally(e);
		}
	}

	public Collection<NagiosCheckResult> getResults() {
		return results;
	}

	public CompletableFuture<Collection<NagiosCheckResult>> getCompletableFuture() {
		return completableFuture;
	}
}

