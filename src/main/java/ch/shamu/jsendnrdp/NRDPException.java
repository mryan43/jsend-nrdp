package ch.shamu.jsendnrdp;

/**
 * Exceptions to throw when the NRDP server returned an error
 * @author mryan
 */
public class NRDPException extends Exception {

	private static final long serialVersionUID = -8134402873078195801L;

	/**
	 * Constructs an instance of <code>NRDPException</code> with the cause
	 * @param msg the detail message.
	 * @param cause the cause
	 */
	public NRDPException(String msg, Throwable cause) {
		super(msg, cause);
	}

	/**
	 * Constructs an instance of <code>NRDPException</code> without a cause
	 * @param msg the detail message.
	 */
	public NRDPException(String msg) {
		super(msg);
	}

}
