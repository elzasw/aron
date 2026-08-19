package cz.aron.ws;

import java.io.IOException;

import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Keeps the two surfaces on their own ports once {@code soap.port} is configured:
 * the internal SOAP interfaces answer only on the SOAP listener, everything else
 * (portal, both REST APIs, actuator) only on the main one. The mismatching
 * combination gets a plain 404 - the surface simply does not exist there.
 * <p>
 * Both connectors serve the same servlet context, so the split has to be made per
 * request; {@code getServletPath()} is the mapping of the CXF servlet
 * ({@code /cxf/*}) and is therefore independent of the deployment's context path.
 */
public class SoapPortFilter extends OncePerRequestFilter {

	/** Servlet path of the CXF servlet mapping (cz.aron.ft.server.CxfWsConfig). */
	private static final String SOAP_SERVLET_PATH = "/cxf";

	private final SoapListener soapListener;

	SoapPortFilter(SoapListener soapListener) {
		this.soapListener = soapListener;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		boolean soapRequest = SOAP_SERVLET_PATH.equals(request.getServletPath());
		boolean soapPort = request.getLocalPort() == soapListener.getPort();
		if (soapRequest != soapPort) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		chain.doFilter(request, response);
	}

}
