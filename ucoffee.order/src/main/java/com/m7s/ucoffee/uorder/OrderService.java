package com.m7s.ucoffee.uorder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lrgoncalves.coffee.model.Order;
import com.lrgoncalves.coffee.model.OrderStatus;
import com.lrgoncalves.coffee.model.exception.ModelNotFoundException;
import com.lrgoncalves.coffee.model.hateos.OrderHATEOS;
import io.micronaut.http.annotation.*;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.web.router.RouteBuilder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.net.URI;
import java.net.URISyntaxException;

import static com.lrgoncalves.coffee.model.mongodb.trace.OrderBusinessOperationType.*;
import static com.lrgoncalves.coffee.model.mongodb.trace.OrderBusinessOperationType.CREATE_ORDER;
import static com.lrgoncalves.coffee.model.mongodb.trace.OrderBusinessOperationType.FIND_ORDER;
import static com.lrgoncalves.coffee.model.mongodb.trace.OrderTrace.*;
import static com.lrgoncalves.coffee.model.mongodb.trace.OrderTrace.storePayloadAsync;
import static com.lrgoncalves.coffee.model.mongodb.trace.TraceType.*;
import static com.lrgoncalves.coffee.model.mongodb.trace.TraceType.REQUEST;
import static com.lrgoncalves.coffee.model.mongodb.trace.TraceType.RESPONSE;
import static com.lrgoncalves.coffee.model.mongodb.trace.TraceType.RESPONSE_ERROR;
import static com.lrgoncalves.coffee.model.type.OrderStatusType.*;
import static com.lrgoncalves.coffee.model.type.OrderStatusType.CANCELLED;
import static com.lrgoncalves.coffee.model.type.OrderStatusType.UNPAID;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.*;
import static com.m7s.ucoffee.uorder.OrderApplicationConfig.*;

@Controller("/order")
public class OrderService {

	/**
	 * 
	 */
	private static final Logger LOG = LoggerFactory.getLogger(OrderService.class);

	/**
	 * 
	 */

	private final URI uriInfo;

	/**
	 * 
	 */
	private static final ObjectMapper mapper = new ObjectMapper();

	/**
	 * 
	 */
	private final OrderHATEOS hateos;

	public OrderService(EmbeddedServer embeddedServer, RouteBuilder.UriNamingStrategy uns) throws URISyntaxException {
		this.uriInfo = embeddedServer.getURI();
		hateos =  new OrderHATEOS(this.uriInfo,CONTROLLER_PATH);
    }
	
	@Get
	@Path("/{UUDI}")
	@Produces(APPLICATION_JSON)
	@Consumes(APPLICATION_JSON)
	public Response getOrder(@PathParam(value="UUDI") String uudi) {

		try {

			
			String pathParam = "{ \"path_parameters\" :  [ { \"UUDI\" : \""+uudi+"\" }] }";
			
			LOG.info("Call Store Payload Async ....");
			storePayloadAsync(REQUEST, FIND_ORDER , pathParam);
			LOG.info("After Call Store Payload Async ....");
			
			if(uudi == null && StringUtils.isEmpty(uudi)) {
				LOG.debug("UUDI request is null or empty.");
				throw new IllegalArgumentException("Bad Request");
			}

			LOG.info("Receiving Order Request Payload in method getOrder");
			LOG.debug(uudi);

			Order order = new Order();

			LOG.info("Fiding order request");
			order = order.findByUUDI(uudi);

			LOG.info("Generating HATEOS");
			String result = hateos.generateHATEOS(order);
			LOG.debug(result);

			//storePayloadAsync(RESPONSE, FIND_ORDER , result);
			
			LOG.info("Returning HTTP Status "+ OK);
			return Response.status(OK)
					/**.header("Access-Control-Allow-Origin","*")
					.header("Access-Control-Allow-Headers","*")
					.header("Access-Control-Allow-Methods","*")
					.header("Content-Type","*")**/
					.entity(result).build();
		}catch (IllegalArgumentException i) {
			
			LOG.error(i.getMessage());
			
			try {				
				storePayloadAsync(RESPONSE_ERROR,FIND_ORDER,buildErrorResponse(Status.NOT_FOUND));
			} catch (InterruptedException e) {
				LOG.error(e.getMessage());
			}
			
			return Response.status(NOT_FOUND).entity(i.getMessage()).build();
		} 
		catch (Throwable t) {
			
			LOG.error(t.getMessage());
			
			try {				
				storePayloadAsync(RESPONSE_ERROR,FIND_ORDER,buildErrorResponse(INTERNAL_SERVER_ERROR));
			} catch (InterruptedException e) {
				LOG.error(e.getMessage());
			}
			
			return Response.serverError().entity(t.getMessage()).build();
		}
	}


	@Post
	@Produces(APPLICATION_JSON)
	@Consumes(APPLICATION_JSON)
	public Response createOrder(@Body final String orderRequestJson) {

		LOG.info("Receiving Order Request Payload in method createOrder");
		LOG.debug(orderRequestJson);

		Order order = null;

		try {

			storePayloadAsync(REQUEST, CREATE_ORDER ,orderRequestJson);

			if(StringUtils.isBlank(orderRequestJson)) {
				LOG.debug("Payload request is null or empty.");
				throw new IllegalArgumentException("Bad Request");
			}

			LOG.info("Converting JSON Payload to Object");
			order = mapper.readValue(orderRequestJson, Order.class);

			LOG.info("Saving order request");
			order.save(order);

			LOG.info("Generating HATEOS");
			String result = hateos.generateHATEOS(order);
			LOG.debug(result);

			storePayloadAsync(RESPONSE, CREATE_ORDER ,result);

			LOG.info("Returning HTTP Status "+ CREATED);
			return Response.status(CREATED).entity(result).build();

		}catch (IllegalArgumentException i) {
			
			LOG.error(i.getMessage());

			Response response = Response.status(BAD_REQUEST).build();
			try {				
				storePayloadAsync(RESPONSE_ERROR,CREATE_ORDER,buildErrorResponse(BAD_REQUEST));
			} catch (InterruptedException e) {
				LOG.error(e.getMessage());
			}
			return response;
		} 
		catch (Throwable t) {
			
			LOG.error(t.getMessage());

			Response response = Response.status(INTERNAL_SERVER_ERROR).build();
			try {
				storePayloadAsync(RESPONSE_ERROR,CREATE_ORDER,buildErrorResponse(INTERNAL_SERVER_ERROR));
			} catch (InterruptedException e) {
				LOG.error(e.getMessage());
			}

			return response;
		}
	}

	@Delete
	@Path("/{UUDI}")
	@Produces(APPLICATION_JSON)
	@Consumes(APPLICATION_JSON)
	public Response cancelOrder(final @PathParam(value="UUDI") String uudi ) {

		LOG.info("Receiving Order Request Payload in method deleteOrder");

		Order order = new Order();

		try {

			LOG.info("Fiding order request");
			order = order.findByUUDI(uudi);

			if(order == null) {
				LOG.info("Returning HTTP Status 404");
				return Response.status(NOT_FOUND).entity("{\"messsage\": \"Resource Not Found.\" }").build();
			}

			if(order.getStatus().getType() == UNPAID) {

				OrderStatus status = order.getStatus();

				status.setType(CANCELLED);
				LOG.info("Updating Status to CANCELLED");
				order.setStatus(status.save(status));

				LOG.info("Generating HATEOS");
				String result = hateos.generateHATEOS(order);
				LOG.debug(result);

				LOG.info("Returning HTTP Status "+ OK);
				return Response.status(OK).entity(result).build();

				
			}else {

				LOG.info("Generating HATEOS");
				String result = hateos.generateHATEOS(order);
				LOG.debug(result);

				LOG.info("Returning HTTP Status 403");
				return Response.status(FORBIDDEN).entity(result).build();
			}

		}catch (ModelNotFoundException mnfe){
			LOG.warn(mnfe.getMessage());
			return Response.status(NOT_FOUND).entity(mnfe.getMessage()).build();
		} catch (Throwable t) {
			LOG.error(t.getMessage());
			return Response.serverError().build();
		}
	}

	/**
	 * Just the location could be updated
	 * @param uudi
	 * @return Response
	 */
	@Patch
	@Path("/{UUDI}")
	@Produces(APPLICATION_JSON)
	@Consumes(APPLICATION_JSON)
	public Response updateOrder(final @PathParam(value="UUDI") String uudi ) {

		LOG.info("Receiving Order Request Payload in method updateOrder");

		Order order = new Order();

		try {

			LOG.info("Saving order request");
			order = order.findByUUDI(uudi);

			if(order.getStatus().getType() == UNPAID) {
				LOG.info("Generating HATEOS");
				//final String HATEOS = OrderHATEOS.generateHATEOS(order,uriInfo.getBaseUri()).toJson();
				//LOG.debug(HATEOS);

				LOG.info("Returning HTTP Status "+ OK);
				return Response.status(OK).entity("").build();

			}else {

				LOG.info("Generating HATEOS");
				//final String HATEOS = OrderHATEOS.generateHATEOS(order,uriInfo.getBaseUri()).toJson();
				//LOG.debug(HATEOS);

				LOG.info("Returning HTTP Status"+ NOT_ACCEPTABLE);
				return Response.status(NOT_ACCEPTABLE).entity("{\"messsage\": \"Invalid Status\" }").build();
			}

		}catch (ModelNotFoundException mnfe){
			LOG.warn(mnfe.getMessage());
			return Response.status(NOT_FOUND).entity(mnfe.getMessage()).build();
		}
		catch (Throwable t) {
			LOG.error(t.getMessage());
			return Response.serverError().build();
		}
	}	

	private String buildErrorResponse(final Status httpStatus) {
		return "{\"Status Code\" : \""+httpStatus.getStatusCode()+" "+httpStatus.getReasonPhrase()+"\"}";
	}
}