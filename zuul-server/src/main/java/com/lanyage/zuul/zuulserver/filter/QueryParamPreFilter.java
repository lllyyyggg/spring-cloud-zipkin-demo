package com.lanyage.zuul.zuulserver.filter;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;

import javax.servlet.http.HttpServletRequest;

public class QueryParamPreFilter extends ZuulFilter {
    @Override
    public String filterType() {
        return "PRE_TYPE";
    }

    @Override
    public int filterOrder() {
        return 0;
    }

    @Override
    public boolean shouldFilter() {
        RequestContext requestContext = RequestContext.getCurrentContext();
        return !requestContext.containsKey("FORWARD_TO_KEY")
                && !requestContext.containsKey("SERVICE_ID_KEY");
    }

    @Override
    public Object run() throws ZuulException {
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();
        if (request.getParameter("foo") != null) {
            ctx.put("SERVICE_ID_KEY", request.getParameter("foo"));
        }
        return null;
    }
}
