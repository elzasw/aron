package cz.inqool.eas.common.tstutil;

import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import static cz.inqool.eas.common.tstutil.TestBase.ELASTICSEARCH_CONTAINER;

@TestConfiguration
@EnableTransactionManagement
public class TestConfig {

    @Bean
    public RestHighLevelClient restHighLevelClient() {
        ClientConfiguration configuration = ClientConfiguration.builder()
                .connectedTo("localhost:" + ELASTICSEARCH_CONTAINER.getMappedPort(9200))
                .build();
        return RestClients.create(configuration).rest();
    }
}
