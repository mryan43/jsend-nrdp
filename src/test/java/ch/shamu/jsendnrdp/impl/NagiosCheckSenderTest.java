package ch.shamu.jsendnrdp.impl;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collection;

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

public class NagiosCheckSenderTest {

	private final static int SERVER_PORT = 53662;

	private NrdpTestServer testServer = new NrdpTestServer(SERVER_PORT);
	private NRDPServerConnectionSettings defaultSettings = new NRDPServerConnectionSettings("http://localhost:" + SERVER_PORT + "/nrdp/", "sq",
			100000);
	private NagiosCheckSender sender = new NagiosCheckSenderImpl(defaultSettings);

	@Before
	public void setup() throws Exception {
		testServer.start();
	}

	@After
	public void tearDown() throws Exception {
		testServer.stop();
	}

	@Test
	public void testSendOneResultSuccess() throws NRDPException, IOException {

                final boolean [] successFlag = new boolean[1];

		// prepare client request
		NagiosCheckResult resultToSend = new NagiosCheckResult("localhost", "prout", State.CRITICAL, "testPayload") {
			public void afterSend() { successFlag[0] = true; }
		};
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
		Assert.assertTrue("afterSend flag", successFlag[0]);

	}

	@Test
	public void testSendMultipleResultsSuccess() throws NRDPException, IOException {

		// prepare client request
		NagiosCheckResult resultToSend1 = new NagiosCheckResult("localhost1", "prout1", State.CRITICAL, "testPayload1");
		NagiosCheckResult resultToSend2 = new NagiosCheckResult("localhost2", "prout2", State.WARNING, "testPayload2");
		Collection<NagiosCheckResult> resultsToSend = new ArrayList<NagiosCheckResult>();
		resultsToSend.add(resultToSend1);
		resultsToSend.add(resultToSend2);

		// prepare server response
		String response = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n";
		response += "<result>\n";
		response += "  <status>0</status>\n";
		response += "  <message>OK</message>\n";
		response += "    <meta>\n";
		response += "       <output>2 checks processed.</output>\n";
		response += "    </meta>\n";
		response += "</result>\n";

		testServer.setMockResponseData(response);

		sender.send(resultsToSend);

		// expected request xml data

		String requestXML = "<?xml version='1.0'?>\n";
		requestXML += "  <checkresults>\n";
		requestXML += "    <checkresult type='service' checktype='1'>\n";
		requestXML += "      <hostname>localhost1</hostname>\n";
		requestXML += "      <servicename>prout1</servicename>\n";
		requestXML += "      <state>2</state>\n";
		requestXML += "      <output>testPayload1</output>\n";
		requestXML += "    </checkresult>\n";
		requestXML += "    <checkresult type='service' checktype='1'>\n";
		requestXML += "      <hostname>localhost2</hostname>\n";
		requestXML += "      <servicename>prout2</servicename>\n";
		requestXML += "      <state>1</state>\n";
		requestXML += "      <output>testPayload2</output>\n";
		requestXML += "    </checkresult>\n";
		requestXML += "  </checkresults>\n";

		Assert.assertEquals(requestXML, testServer.getXmlData());
		Assert.assertEquals("submitcheck", testServer.getCmd());
		Assert.assertEquals("sq", testServer.getToken());

	}

	@Test(expected = NRDPException.class)
	public void testSendWrongToken() throws NRDPException, IOException {

		// prepare client request
		NagiosCheckResult resultToSend = new NagiosCheckResult("localhost", "prout", State.CRITICAL, "testPayload");
		Collection<NagiosCheckResult> resultsToSend = new ArrayList<NagiosCheckResult>();
		resultsToSend.add(resultToSend);

		// prepare server response
		String response = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n";
		response += "<result>\n";
		response += "  <status>-1</status>\n";
		response += "  <message>BAD TOKEN</message>\n";
		response += "</result>\n";

		testServer.setMockResponseData(response);

		sender.send(resultsToSend);

	}

	@Test(expected = SocketTimeoutException.class)
	public void testSendTimeout() throws NRDPException, IOException {
		// prepare client request
		NagiosCheckResult resultToSend = new NagiosCheckResult("localhost", "prout", State.CRITICAL, "testPayload");
		Collection<NagiosCheckResult> resultsToSend = new ArrayList<NagiosCheckResult>();
		resultsToSend.add(resultToSend);

		NRDPServerConnectionSettings lowTimeoutSettings =
				new NRDPServerConnectionSettings("http://localhost:" + SERVER_PORT + "/nrdp/", "sq", 10);
		sender = new NagiosCheckSenderImpl(lowTimeoutSettings);

		// configure response delay (to trigger the timeout in the client) on mock nrdp server
		testServer.setDelay(20);

		sender.send(resultsToSend);

	}

}
