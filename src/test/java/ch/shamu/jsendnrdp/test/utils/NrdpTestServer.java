package ch.shamu.jsendnrdp.test.utils;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.Ignore;

@Ignore
public class NrdpTestServer {

	private int httpPort;
	private String target;
	private Server server;
	private String responseBody;
	private String requestBody;
	private String xmlData;
	private String token;
	private String cmd;
	private String mockResponseData;
	private int delay = 0;
	private boolean responseReceived = false;

	public NrdpTestServer(int httpPort) {
		this.httpPort = httpPort;
	}

	public void start() throws Exception {
		configureServer();
		startServer();
	}

	private void startServer() throws Exception {
		server.start();
	}

	protected void configureServer() {
		server = new Server(httpPort);
		server.setHandler(getMockHandler());
	}

	/**
	 * Creates an {@link AbstractHandler handler} returning an arbitrary String as a response.
	 * @return never <code>null</code>.
	 */
	public Handler getMockHandler() {
		Handler handler = new AbstractHandler() {

			public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException,
					ServletException {
				try {
					Thread.sleep(getDelay());
				}
				catch (InterruptedException e) {
					e.printStackTrace();
				}
				setTarget(target);
				setXmlData(request.getParameter("XMLDATA"));
				setCmd(request.getParameter("cmd"));
				setToken(request.getParameter("token"));
				setResponseBody(getMockResponseData());
				setRequestBody(IOUtils.toString(request.getInputStream()));
				response.setStatus(HttpStatus.SC_OK);
				response.setContentType("text/xml;charset=utf-8");
				response.getWriter().write(getResponseBody());
				baseRequest.setHandled(true);
				setResponseReceived(true);
			}
		};

		return handler;
	}

	public void stop() throws Exception {
		server.stop();
	}

	public void setResponseBody(String responseBody) {
		this.responseBody = responseBody;
	}

	public String getResponseBody() {
		return responseBody;
	}

	public void setRequestBody(String requestBody) {
		this.requestBody = requestBody;
	}

	public String getRequestBody() {
		return requestBody;
	}

	public void setMockResponseData(String mockResponseData) {
		this.mockResponseData = mockResponseData;
	}

	public String getMockResponseData() {
		return mockResponseData;
	}

	protected Server getServer() {
		return server;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public Object getXmlData() {
		return xmlData;
	}

	public void setXmlData(String xmlData) {
		this.xmlData = xmlData;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getCmd() {
		return cmd;
	}

	public void setCmd(String cmd) {
		this.cmd = cmd;
	}

	public int getDelay() {
		return delay;
	}

	public void setDelay(int _delay) {
		this.delay = _delay;
	}

	public boolean isResponseReceived() {
		return responseReceived;
	}

	public void setResponseReceived(boolean responseReceived) {
		this.responseReceived = responseReceived;
	}

}
