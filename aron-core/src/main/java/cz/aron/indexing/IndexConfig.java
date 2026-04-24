package cz.aron.indexing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;

@Configuration
public class IndexConfig /*extends ElasticsearchConfiguration*/ {
	
	public static final String FOLDING_AND_TOKENIZING = "folding_and_tokenizing";
	
	public static final String FOLDING_AND_TOKENIZING_STOP = "folding_and_tokenizing_stop";
	
	public static final String TEXT_LONG_KEYWORD = "text_long_keyword";
	
	public static final String TEXT_LONG_KEYWORD_CI = "text_long_keyword_ci";
	
	public static final String SORTING = "sorting";
	
	public static final String SUFFIX_SORT = "_sort";
	
	private final String connectionUrl;

	public IndexConfig(@Value("${spring.elasticsearch.rest.uris}") String connetionUrl) {
		this.connectionUrl = connetionUrl;
	}

	//@Override
	public ClientConfiguration clientConfiguration() {
		return ClientConfiguration.builder()           
				.connectedTo(connectionUrl)				
				.build();
	}

}
