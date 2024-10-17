package bg.codexio.springframework.boot.requestcachingfilter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;

public class RequestCachingFilter
        implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

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
        //        System.out.println(request.getReader().lines().collect
        //        (Collectors.joining(System.lineSeparator())));

    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
