package ch.shamu.jsendnrdp.impl;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.shamu.jsendnrdp.NRDPException;
import ch.shamu.jsendnrdp.NRDPServerConnectionSettings;
import ch.shamu.jsendnrdp.NagiosCheckSender;
import ch.shamu.jsendnrdp.domain.NagiosCheckResult;

import com.google.common.util.concurrent.RateLimiter;

/**
 * Instances of this class are used to send the check results
 * by a non-blocking threads. The caller may get access to these
 * instances when using a custom executor.
 */
public class NonBlockingSender implements Runnable {

	private final static Logger logger =
            LoggerFactory.getLogger(NonBlockingSender.class);

        private final Collection<NagiosCheckResult> results;
        private final NagiosCheckSender sender;
        private final RateLimiter rateLimiter;

        public NonBlockingSender(Collection<NagiosCheckResult> results, NagiosCheckSender sender, RateLimiter rateLimiter) {
                this.results = results;
                this.sender = sender;
                this.rateLimiter = rateLimiter;
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

        public Collection<NagiosCheckResult> getResults() {
            return results;
        }


}

