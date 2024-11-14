package bg.codexio.springframework.boot.configuration;

import bg.codexio.springframework.boot.adapter.GraphQLComplexFilterAdapter;
import bg.codexio.springframework.boot.adapter.GraphQLDefaultComplexFilterAdapter;
import bg.codexio.springframework.boot.adapter.GraphQLHttpFilterAdapter;
import bg.codexio.springframework.data.jpa.requery.adapter.HttpFilterAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration class for setting up GraphQL filter adapters, including
 * complex filter handling and HTTP request filtering.
 *
 * <p>
 * This configuration class defines beans for the
 * {@link GraphQLComplexFilterAdapter},
 * {@link HttpFilterAdapter}, and {@link SupportsProperties}. These beans are
 * auto-configured to facilitate advanced GraphQL filtering capabilities,
 * such as
 * adapting complex filters and processing HTTP requests.
 * </p>
 */
@Configuration
@EnableConfigurationProperties
public class FilterGraphQLAutoConfiguration {

    /**
     * Defines a {@link GraphQLComplexFilterAdapter} bean using the default
     * implementation {@link GraphQLDefaultComplexFilterAdapter}.
     *
     * @param objectMapper the {@link ObjectMapper} for JSON deserialization
     * @return a {@link GraphQLComplexFilterAdapter} for handling complex
     * filters
     */
    @Bean
    public GraphQLComplexFilterAdapter graphQLComplexFilterAdapter(ObjectMapper objectMapper) {
        return new GraphQLDefaultComplexFilterAdapter(objectMapper);
    }

    /**
     * Defines an {@link HttpFilterAdapter} bean for handling HTTP requests with
     * GraphQL filtering capabilities.
     *
     * @param objectMapper the {@link ObjectMapper} for JSON deserialization
     * @return a {@link HttpFilterAdapter} configured with the GraphQL
     * complex filter
     * adapter and support properties
     */
    @Bean
    public HttpFilterAdapter graphQLHttpFilterAdapter(ObjectMapper objectMapper) {
        return new GraphQLHttpFilterAdapter(
                objectMapper,
                graphQLComplexFilterAdapter(objectMapper),
                supportsProperties()
        );
    }

    /**
     * Defines a {@link SupportsProperties} bean to provide configuration
     * properties
     * for GraphQL support within HTTP requests.
     *
     * @return a new {@link SupportsProperties} instance
     */
    @Bean
    public SupportsProperties supportsProperties() {
        return new SupportsProperties();
    }
}