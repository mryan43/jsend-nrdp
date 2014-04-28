package ch.shamu.jsendnrdp.impl;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import ch.shamu.jsendnrdp.NRDPException;
import ch.shamu.jsendnrdp.NRDPServerConnectionSettings;
import ch.shamu.jsendnrdp.NagiosCheckSender;
import ch.shamu.jsendnrdp.domain.CheckSubmissionResult;
import ch.shamu.jsendnrdp.domain.NagiosCheckResult;

/**
 * {@inheritDoc}
 * 
 * <pre>
 * In this implementation, calls to the send method waits for the NRDP server's response before returning.
 * However, concurrent calls to the send method do send http requests in parallel.
 * </pre>
 */
public class NagiosCheckSenderImpl implements NagiosCheckSender {

	private final static Logger logger = LoggerFactory.getLogger(NagiosCheckSenderImpl.class);

	private final NRDPServerConnectionSettings server;
	private HttpClient httpClient;

	public NagiosCheckSenderImpl(NRDPServerConnectionSettings server) {
		this.server = server;
		// initialize HTTP client 
		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
		RequestConfig requestConfig = RequestConfig.custom()//
				.setSocketTimeout(server.getTimeout())//
				.setConnectTimeout(server.getTimeout()).build();
		httpClient = HttpClients.custom()//
				.setDefaultRequestConfig(requestConfig)//
				.setConnectionManager(connectionManager).build();
	}

	public void send(Collection<NagiosCheckResult> results) throws NRDPException, IOException {

		// build XML
		StringBuilder b = new StringBuilder();

		b.append("<?xml version='1.0'?>\n");
		b.append("  <checkresults>\n");
		for (NagiosCheckResult r : results) {
			b.append("    <checkresult type='service' checktype='");
			b.append(NagiosCheckResult.PASSIVE_CHECK_TYPE);
			b.append("'>\n");
			b.append("      <hostname>");
			b.append(StringEscapeUtils.escapeXml(r.getHost()));
			b.append("</hostname>\n");
			b.append("      <servicename>");
			b.append(StringEscapeUtils.escapeXml(r.getService()));
			b.append("</servicename>\n");
			b.append("      <state>");
			b.append(r.getState().getCode());
			b.append("</state>\n");
			b.append("      <output>");
			b.append(StringEscapeUtils.escapeXml(r.getMessage()));
			b.append("</output>\n");
			b.append("    </checkresult>\n");
			logger.info("Nagios check results to be sent {hostname:" + r.getHost() + ",servicename:" + r.getService() + ",state:" + r.getState()
					+ ",message:" + r.getMessage() + "}");
		}
		b.append("  </checkresults>\n");

		String xml = b.toString();

		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new BasicNameValuePair("token", server.getToken()));
		postParams.add(new BasicNameValuePair("cmd", "submitcheck"));
		postParams.add(new BasicNameValuePair("XMLDATA", xml));

		// attempt to POST the message to NRDP, using the HTTPClient
		HttpPost request = new HttpPost(server.getUrl());
		request.setEntity(new UrlEncodedFormEntity(postParams));
		HttpResponse response = httpClient.execute(request); // eventual IO exceptions are allowed to bubble up from here
		HttpEntity entity = response.getEntity();
		String responseString = EntityUtils.toString(entity, "UTF-8");

		// Treat the response
		CheckSubmissionResult result;

		try {
			result = parseResponseXML(responseString);
		}
		catch (Exception e) {
			throw new NRDPException("Failed to parse http response body from NRDP server (should be XML) : " + responseString, e);
		}

		if (!result.getStatus().equals("0")) {
			throw new NRDPException("NRDP server returned with code " + result.getStatus() + " and message " + result.getMessage());
		}

		logger.info(results.size() + " check results succesfully sent to Nagios");
	}

	private CheckSubmissionResult parseResponseXML(String xml) throws ParserConfigurationException, SAXException, IOException {
		final CheckSubmissionResult res = new CheckSubmissionResult();

		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser saxParser = factory.newSAXParser();

		DefaultHandler handler = new DefaultHandler() {

			boolean statusTag = false;
			boolean messageTag = false;

			@Override
			public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
				if (qName.equalsIgnoreCase("status")) {
					statusTag = true;
				}
				if (qName.equalsIgnoreCase("message")) {
					messageTag = true;
				}
			}

			@Override
			public void characters(char[] ch, int start, int length) throws SAXException {
				if (statusTag) {
					res.setStatus(new String(ch, start, length));
					statusTag = false;
				}
				if (messageTag) {
					res.setMessage(new String(ch, start, length));
					messageTag = false;
				}
			}

		};

		saxParser.parse(new InputSource(new StringReader(xml)), handler);
		if (res.getStatus() == null || res.getMessage() == null) {
			throw new SAXException("Failed to get response status and message");
		}
		return res;
	}
}
