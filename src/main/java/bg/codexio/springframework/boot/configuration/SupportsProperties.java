package bg.codexio.springframework.boot.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "codexio.requery.adapters.graphql.supports")
public class SupportsProperties {
    private boolean shouldCheckBody;
    private boolean inclusive;
    private String urlPattern;

    public boolean shouldCheckBody() {
        return shouldCheckBody;
    }

    public void setShouldCheckBody(boolean shouldCheckBody) {
        this.shouldCheckBody = shouldCheckBody;
    }

    public String getUrlPattern() {
        return urlPattern;
    }

    public void setUrlPattern(String urlPattern) {
        this.urlPattern = urlPattern;
    }

    public boolean isInclusive() {
        return inclusive;
    }

    public void setInclusive(boolean inclusive) {
        this.inclusive = inclusive;
    }
}
