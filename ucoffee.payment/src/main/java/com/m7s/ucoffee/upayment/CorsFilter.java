package com.m7s.ucoffee.upayment;

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

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.logging.Logger;

import static com.m7s.ucoffee.upayment.ServicesProperties.*;

@Filter("/**")
public class CorsFilter implements HttpServerFilter {

	private static final Logger LOGGER = Logger.getLogger(CorsFilter.class.getName());


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
