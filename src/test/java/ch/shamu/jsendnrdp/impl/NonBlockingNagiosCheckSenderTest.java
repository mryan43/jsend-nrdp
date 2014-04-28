package ch.shamu.jsendnrdp.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ch.shamu.jsendnrdp.NRDPException;
import ch.shamu.jsendnrdp.NRDPServerConnectionSettings;
import ch.shamu.jsendnrdp.NagiosCheckSender;
import ch.shamu.jsendnrdp.domain.NagiosCheckResult;
import ch.shamu.jsendnrdp.domain.State;
import ch.shamu.jsendnrdp.test.utils.NrdpTestServer;

public class NonBlockingNagiosCheckSenderTest {

	private final static int SERVER_PORT = 53662;
	private final static int NB_THREADS = 2;
	private final static int SEND_QUEUE_SIZE = 2;

	private NrdpTestServer testServer = new NrdpTestServer(SERVER_PORT);
	private NRDPServerConnectionSettings defaultSettings = new NRDPServerConnectionSettings("http://localhost:" + SERVER_PORT + "/nrdp/", "sq",
			100000);

	@Before
	public void setup() throws Exception {
		testServer.start();
	}

	@After
	public void tearDown() throws Exception {
		testServer.stop();
	}

	@Test
	public void testNonBlockingSendSuccess() throws NRDPException, IOException, TimeoutException {
		NagiosCheckSender sender = new NonBlockingNagiosCheckSender(defaultSettings, NB_THREADS, SEND_QUEUE_SIZE);

		// prepare client request
		NagiosCheckResult resultToSend = new NagiosCheckResult("localhost", "prout", State.CRITICAL, "testPayload");
		Collection<NagiosCheckResult> resultsToSend = new ArrayList<NagiosCheckResult>();
		resultsToSend.add(resultToSend);

		// prepare server response
		String response = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n";
		response += "<result>\n";
		response += "  <status>0</status>\n";
		response += "  <message>OK</message>\n";
		response += "    <meta>\n";
		response += "       <output>1 checks processed.</output>\n";
		response += "    </meta>\n";
		response += "</result>\n";

		testServer.setMockResponseData(response);

		sender.send(resultsToSend);

		// since the send process is non-blocking, we check the server state every 10 ms to see if the request arrived (for 2 seconds max)
		long startTime = System.currentTimeMillis();
		testServer.setResponseReceived(false);
		while (testServer.isResponseReceived() == false) {
			try {
				Thread.sleep(10);
			}
			catch (InterruptedException e) {
				//
			}
			if (System.currentTimeMillis() - startTime > 2000) {
				throw new TimeoutException("The test NRDP server did not receive any request after 2 seconds");
			}
		}

		// expected request xml data
		String requestXML = "<?xml version='1.0'?>\n";
		requestXML += "  <checkresults>\n";
		requestXML += "    <checkresult type='service' checktype='1'>\n";
		requestXML += "      <hostname>localhost</hostname>\n";
		requestXML += "      <servicename>prout</servicename>\n";
		requestXML += "      <state>2</state>\n";
		requestXML += "      <output>testPayload</output>\n";
		requestXML += "    </checkresult>\n";
		requestXML += "  </checkresults>\n";

		Assert.assertEquals(requestXML, testServer.getXmlData());
		Assert.assertEquals("submitcheck", testServer.getCmd());
		Assert.assertEquals("sq", testServer.getToken());

	}

	@Test(expected = IOException.class)
	public void testNonBlockingSendQueueOverload() throws NRDPException, IOException {
		NagiosCheckSender sender = new NonBlockingNagiosCheckSender(defaultSettings, NB_THREADS, SEND_QUEUE_SIZE);

		// prepare client request
		NagiosCheckResult resultToSend = new NagiosCheckResult("localhost", "prout", State.CRITICAL, "testPayload");
		Collection<NagiosCheckResult> resultsToSend = new ArrayList<NagiosCheckResult>();
		resultsToSend.add(resultToSend);

		testServer.setDelay(100);

		for (int i = 0; i < 100; i++) {
			sender.send(resultsToSend);
		}
	}

}
