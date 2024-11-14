package bg.codexio.springframework.boot.requestcachingfilter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;

/**
 * A servlet filter that wraps incoming {@link HttpServletRequest} instances
 * with a {@link ContentCachingRequestWrapper} to enable multiple reads of
 * the request body.
 *
 * <p>
 * This filter is useful when the request body needs to be accessed multiple
 * times within the processing chain, as the
 * {@link ContentCachingRequestWrapper}
 * caches the content for repeated reads.
 * </p>
 */
public class RequestCachingFilter
        implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    /**
     * Wraps the incoming {@link HttpServletRequest} with a
     * {@link ContentCachingRequestWrapper} to enable caching of the request
     * body, allowing it to be read multiple times.
     *
     * @param request  the incoming {@link ServletRequest}, cast to
     *                 {@link HttpServletRequest}
     * @param response the outgoing {@link ServletResponse}
     * @param chain    the {@link FilterChain} to pass the wrapped request
     *                 and response along
     * @throws IOException      if an I/O error occurs during filtering
     * @throws ServletException if a servlet error occurs during filtering
     */
    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {
        ContentCachingRequestWrapper cachedRequest =
                new ContentCachingRequestWrapper((HttpServletRequest) request);

        chain.doFilter(
                cachedRequest,
                response
        );
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
