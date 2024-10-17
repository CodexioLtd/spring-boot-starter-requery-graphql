package bg.codexio.springframework.boot.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "codexio.requery.adapters.graphql.supports")
public class SupportsProperties {
    private boolean checkBody;
    private boolean inclusive;
    private String urlPattern;

    public boolean isCheckBody() {
        return checkBody;
    }

    public void setCheckBody(boolean checkBody) {
        this.checkBody = checkBody;
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
