package ch.shamu.jsendnrdp.domain;

/**
 * A Nagios check result
 * @author mryan
 */
public class NagiosCheckResult {

	public final static int ACTIVE_CHECK_TYPE = 0; // Nagios specific
	public final static int PASSIVE_CHECK_TYPE = 1; // Nagios specific

	private final String host;
	private final String message;
	private final String service;
	private final State state;

	/**
	 * Instantiate a Nagios check result
	 * @param service is the id of the service in Nagios
	 * @param state is the computed {@link ch.shamu.jsendnrdp.domain.State} of the check
	 * @param message is a free text to send to Nagios to describe the result of the check (supports multiple lines)
	 */
	public NagiosCheckResult(String host, String service, State state, String message) {
		this.host = host;
		this.message = message;
		this.service = service;
		this.state = state;
	}

	public String getHost() {
		return host;
	}

	public String getMessage() {
		return message;
	}

	public String getService() {
		return service;
	}

	public State getState() {
		return state;
	}

        /**
         * This method is invoked after confirming
         * that the check result is submitted to NRDP.
         * Can be overridden by the caller and serve
         * as notification mechanism.
         */
        public void afterSend() {}

}
