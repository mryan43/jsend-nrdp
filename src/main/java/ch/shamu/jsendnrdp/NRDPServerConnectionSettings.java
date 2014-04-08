package ch.shamu.jsendnrdp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reprensentation of a NRDP server
 * @author mryan
 */
public class NRDPServerConnectionSettings {

	private final static Logger logger = LoggerFactory.getLogger(NRDPServerConnectionSettings.class);

	private final String url;
	private final String token;
	private final int timeout;

	/**
	 * @param url is the url of the NRDP server endpoint, example : http://nagios.mydomain.com/nrdp
	 * @param token is an authentication token configured in the NRDP server
	 * @param timeout is the maximum time to wait on the NRPD server's response (in milliseconds)
	 */
	public NRDPServerConnectionSettings(String url, String token, int timeout) {
		this.url = url;
		this.token = token;
		this.timeout = timeout;

		if (!url.startsWith("https://")) {
			logger.warn("Security warning : NRDP server URL doesn't seem to be configured to use SSL, check http://nagios.sourceforge.net/docs/nagioscore/4/en/security.html to see why this could be a problem");
		}

		logger.info("NRDP url : " + url);
		logger.info("NRDP token : [FILTERED]"); // for obvious security reasons
		logger.info("NRDP timeout :" + timeout);

	}

	public String getUrl() {
		return url;
	}

	public String getToken() {
		return token;
	}

	public int getTimeout() {
		return timeout;
	}

}
