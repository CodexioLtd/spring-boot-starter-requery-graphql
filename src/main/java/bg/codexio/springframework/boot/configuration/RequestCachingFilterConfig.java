package bg.codexio.springframework.boot.configuration;

import bg.codexio.springframework.boot.requestcachingfilter.RequestCachingFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for registering a {@link RequestCachingFilter} bean,
 * which caches the HTTP request body to enable multiple reads of the body.
 *
 * <p>
 * The {@link RequestCachingFilter} is conditionally registered based on the
 * property {@code codexio.requery.adapters.graphql.supports.check-body} being
 * set to {@code true}. This filter is applied to all URL patterns (/*).
 * </p>
 */
@Configuration
public class RequestCachingFilterConfig {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Registers the {@link RequestCachingFilter} to cache the request body,
     * conditionally enabled by the property
     * {@code codexio.requery.adapters.graphql.supports.check-body}.
     *
     * @return a {@link FilterRegistrationBean} for the
     * {@link RequestCachingFilter}
     */
    @Bean
    @ConditionalOnProperty(
            prefix = "codexio.requery.adapters.graphql.supports",
            name = "check-body",
            havingValue = "true"
    )
    public FilterRegistrationBean<RequestCachingFilter> requestCachingFilter() {
        FilterRegistrationBean<RequestCachingFilter> registrationBean =
                new FilterRegistrationBean<>();
        registrationBean.setFilter(new RequestCachingFilter());
        registrationBean.addUrlPatterns("/*");
        this.logger.info(
                "{} has been registered successfully",
                this.getClass()
                    .getSimpleName()
        );
        return registrationBean;
    }
}
