package com.m7s.ucoffee.uorder;

import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.FilterChain;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import io.reactivex.Flowable;
import org.reactivestreams.Publisher;

import static com.m7s.ucoffee.uorder.ServicesProperties.*;
import java.io.IOException;
import java.util.logging.Logger;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Filter("/**")
public class CorsFilter implements HttpServerFilter {

	private static final Logger LOGGER = Logger.getLogger(CorsFilter.class.getName());

	private final OrderService orderService;

	public CorsFilter(OrderService orderService){
		this.orderService = orderService;
	}




	public void filter(ContainerRequestContext requestContext) throws IOException {
		try {

			if ("OPTIONS" != requestContext.getMethod()) {

				String mediaType = requestContext.getHeaderString("Content-Type");
				
				if (mediaType == null || mediaType.isEmpty()
						|| !mediaType.equals(APPLICATION_VND_INTODATA_DAP_JSON)) {
					requestContext.abortWith(Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE).build());
					return;
				}

				String apiToken = requestContext.getHeaderString("api-token");

				if (apiToken == null || apiToken.isEmpty() || apiToken.trim().length() != 16) {
					requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
					return;
				}

			}
		} catch (Throwable t) {
			LOGGER.severe(t.getMessage());
			throw new IOException(t.getMessage());
		}

	}

	@Override
	public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {


		return Flowable.fromPublisher(chain.proceed(request)).flatMap(response -> {

			if(!request.getMethod().equals(HttpMethod.OPTIONS)) {
				response.getHeaders().add(ACCESS_CONTROL_ALLOW_ORIGIN, ACCESS_CONTROL_ALLOW_ORIGIN_VALUE);
				response.getHeaders().add(ACCESS_CONTROL_ALLOW_CREDENTIALS, ACCESS_CONTROL_ALLOW_CREDENTIALS_VALUE);
				response.getHeaders().add(ACCESS_CONTROL_ALLOW_HEADERS, ACCESS_CONTROL_ALLOW_HEADERS_VALUE);
				response.getHeaders().add(ACCESS_CONTROL_ALLOW_METHODS, ACCESS_CONTROL_ALLOW_METHODS_VALUE);
			}
			return Flowable.just(response);
		});
	}

	@Override
	public Publisher<? extends HttpResponse<?>> doFilter(HttpRequest<?> request, FilterChain chain) {
		return HttpServerFilter.super.doFilter(request, chain);
	}
}
