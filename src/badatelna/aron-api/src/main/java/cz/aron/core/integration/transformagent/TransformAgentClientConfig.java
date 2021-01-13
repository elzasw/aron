package cz.aron.core.integration.transformagent;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

/**
 * @author Lukas Jane (inQool) 06.11.2020.
 */
@Configuration
public class TransformAgentClientConfig {

    @Value("${transformation-agent-url}")
    private String transformationAgentUrl;

    @Bean
    public Jaxb2Marshaller marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath("cz.aron.apux._2020");
        return marshaller;
    }
    @Bean
    public TransformAgentClient transformAgentClient(Jaxb2Marshaller marshaller) {
        TransformAgentClient client = new TransformAgentClient();
        client.setDefaultUri(transformationAgentUrl);
        client.setMarshaller(marshaller);
        client.setUnmarshaller(marshaller);
        return client;
    }
}
