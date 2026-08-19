package cz.aron.ws;

import org.apache.catalina.connector.Connector;

/**
 * The optional separate listener of the internal SOAP interfaces, configured by
 * {@code soap.port} (see {@link SoapListenerConfig}). Exists as a bean only when
 * that port is configured.
 */
public class SoapListener {

	private final Connector connector;

	SoapListener(Connector connector) {
		this.connector = connector;
	}

	/**
	 * The port the listener is really bound to - resolves a configured {@code 0}
	 * (ephemeral port, used by tests) to the port assigned at startup.
	 */
	public int getPort() {
		return connector.getLocalPort();
	}

}
