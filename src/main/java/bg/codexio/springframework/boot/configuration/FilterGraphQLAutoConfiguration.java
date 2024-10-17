package bg.codexio.springframework.boot.configuration;

import bg.codexio.springframework.boot.adapter.GraphQLComplexFilterAdapter;
import bg.codexio.springframework.boot.adapter.GraphQLDefaultComplexFilterAdapter;
import bg.codexio.springframework.boot.adapter.GraphQLHttpFilterAdapter;
import bg.codexio.springframework.data.jpa.requery.adapter.HttpFilterAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
public class FilterGraphQLAutoConfiguration {

    @Bean
    public GraphQLComplexFilterAdapter graphQLComplexFilterAdapter(ObjectMapper objectMapper) {
        return new GraphQLDefaultComplexFilterAdapter(objectMapper);
    }

    @Bean
    public HttpFilterAdapter graphQLHttpFilterAdapter(ObjectMapper objectMapper) {
        return new GraphQLHttpFilterAdapter(
                objectMapper,
                graphQLComplexFilterAdapter(objectMapper),
                supportsProperties()
        );
    }

    @Bean
    public SupportsProperties supportsProperties() {
        return new SupportsProperties();
    }
}