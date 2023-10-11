package com.m7s.ucoffee.upayment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lrgoncalves.coffee.model.Order;
import com.lrgoncalves.coffee.model.Payment;
import com.lrgoncalves.coffee.model.hateos.OrderHATEOS;
import com.lrgoncalves.coffee.model.type.OrderStatusType;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.web.router.RouteBuilder;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;

/**
 * @author lrgoncalves
 *
 */
@Controller("/payment")
public class PaymentService {

	/**
	 * 
	 */
	private static final Logger LOG = LoggerFactory.getLogger(PaymentService.class);
	/**
	 * 
	 */
	private static ObjectMapper mapper = new ObjectMapper();

	private OrderHATEOS hateos;

	private URI uriInfo;

	public PaymentService(EmbeddedServer embeddedServer, RouteBuilder.UriNamingStrategy uns) throws URISyntaxException {
		this.uriInfo = embeddedServer.getURI();
		hateos =  new OrderHATEOS(this.uriInfo,"api");
	}

	@Post
	@Path("/order/{UUDI}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response creditCardPayment( @HeaderParam("Referrer") String orderLink  ,final @PathParam("UUDI") String  uudi , @Body final String paymentRequestJson) {

		final String badRequestOrderLinkMessage = """
													{\"messsage\": \"Invalid Request\" }
													""";

		final String notAcceptableInvalidRequestMessage = """
           						{\"messsage\": \"Invalid Request\" }
    							""";

		final String notAcceptableSatusPayMessage = """
           						{ \"messsage\": \"Invalid Status\" }
    							""";

		try {

			/**
			 * @implNote Technical validation
			 * It`s a "double-check" for @code( HTTP Header parameter "Referer" )
			 */
			if(StringUtils.isBlank(orderLink)) {
				throw new BadRequestException(badRequestOrderLinkMessage);
				//return Response.status(Status.BAD_REQUEST).entity(badRequestOrderLinkMessage).build();
			}

			if( !uudi.equals(orderLink.substring(orderLink.lastIndexOf("/") + 1))){
				throw new BadRequestException(notAcceptableInvalidRequestMessage);
			}

			if(StringUtils.isBlank(paymentRequestJson)) {
				throw new NotAcceptableException(notAcceptableInvalidRequestMessage);
			}

			HttpResponse<JsonNode> jsonOrder = Unirest.get(orderLink)
					.header("Content-Type", "application/json").asJson();

			Payment payment = mapper.readValue(paymentRequestJson, Payment.class);
			LOG.info(jsonOrder.getBody().toString());
			payment.setOrder(getOrder(jsonOrder));

			/**
			 * @implNote
			 * Only UNPAID order status can be paid
			 * */
			/**
			if(payment.getOrder().getStatus().getType() != OrderStatusType.UNPAID) {
				LOG.info("Returning HTTP Status 406");
				throw new NotAcceptableException(notAcceptableSatusPayMessage);
				//return Response.status(Status.NOT_ACCEPTABLE).entity(notAcceptableSatusPayMessage).build();
			}
**/
			//double price = getPriceFromOrder(jsonOrder);

			final String notAcceptableInvalidPaymentValueMessage = "{"+
					" \"orderPrice\": " + payment.getOrder().getPrice() +
					" , \"sentAmount\":" + payment.getAmount() +
					" }";

			if(payment.getAmount() != payment.getOrder().getPrice()) {
				LOG.info("Returning HTTP Status 406");
				throw new NotAcceptableException(notAcceptableInvalidPaymentValueMessage);
			}

			payment.getOrder().getStatus().setType(OrderStatusType.PREPARING);

			payment.save(payment);

			LOG.info("Returning HTTP Status 201");
			return Response.status(Status.CREATED).entity(hateos.generateHATEOS(payment.getOrder())).build();

		} catch (BadRequestException bre){
			LOG.info("Message",bre.getMessage());
			LOG.info("HTTP Status Code",Status.BAD_REQUEST.getStatusCode());
			return Response.status(Status.BAD_REQUEST).entity(bre.getMessage()).build();
		} catch (NotAcceptableException nae){
			LOG.info("Message",nae.getMessage());
			LOG.info("HTTP Status Code",Status.BAD_REQUEST.getStatusCode());
			return Response.status(Status.NOT_ACCEPTABLE).entity(nae.getMessage()).build();
		} catch (Throwable t) {
			LOG.error(t.getMessage());
			return Response.serverError().build();
		}
	}

	private double getPriceFromOrder(HttpResponse<JsonNode> jsonOrder) {

		Iterator<Object> jsonIterator = jsonOrder.getBody().getArray().iterator();

		while(jsonIterator.hasNext()) {
			JSONObject jsonObject = (JSONObject) jsonIterator.next();
			return (double) jsonObject.get("price");
		}

		return 0.0;
	}


	private Order getOrder(HttpResponse<JsonNode> jsonOrder) {

		Iterator<Object> jsonIterator = jsonOrder.getBody().getArray().iterator();

		while(jsonIterator.hasNext()) {
			JSONObject jsonObject = (JSONObject) jsonIterator.next();
			Order order = new Order();
			order.setId((Long) jsonObject.getLong("id"));
			return order.find(order.getId(),3);
		}

		return null;
	}
}
