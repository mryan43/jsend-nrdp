package ch.shamu.jsendnrdp;

import java.io.IOException;
import java.util.Collection;

import ch.shamu.jsendnrdp.domain.NagiosCheckResult;

/**
 * This interface describes the ability to send check results to Nagios
 * @author mryan
 */
public interface NagiosCheckSender {
	/**
	 * attempt to send a check result to Nagios
	 * @param checkResults is a list of results to send
	 * @throws NRDPException thrown if an error occurs while sending the passive check
	 * @throws IOException thrown if I/O error occurs while trying to establish connection with nagios host
	 */
	void send(Collection<NagiosCheckResult> checkResults) throws NRDPException, IOException;
}
