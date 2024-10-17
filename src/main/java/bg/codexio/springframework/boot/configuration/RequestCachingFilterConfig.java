package bg.codexio.springframework.boot.configuration;

import bg.codexio.springframework.boot.requestcachingfilter.RequestCachingFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RequestCachingFilterConfig {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

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
