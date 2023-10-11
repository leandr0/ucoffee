/**
 * 
 */
package com.m7s.ucoffee.uorder.uitem;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lrgoncalves.coffee.model.Item;
import com.lrgoncalves.coffee.model.Order;
import com.lrgoncalves.coffee.model.exception.ModelNotFoundException;
import com.lrgoncalves.coffee.model.hateos.ItemHATEOS;
import com.lrgoncalves.coffee.model.hateos.OrderHATEOS;
import com.lrgoncalves.coffee.model.type.OrderStatusType;
import io.micronaut.http.annotation.*;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.web.router.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import static com.m7s.ucoffee.uorder.OrderApplicationConfig.CONTROLLER_PATH;

/**
 * @author lrgoncalves
 *
 */
@Controller("/order/{UUDI}/item")
public class ItemService {

	/**
	 *
	 */
	private static final Logger LOG = LoggerFactory.getLogger(ItemService.class);

	/**
	 *
	 */
	private static ObjectMapper mapper = new ObjectMapper();

	/**
	 *
	 */
	private final ItemHATEOS hateos;

	private OrderHATEOS orderHATEOS;
	/**
	 *
	 */
	private final URI uriInfo;

	public ItemService(EmbeddedServer embeddedServer, RouteBuilder.UriNamingStrategy uns) throws URISyntaxException {
		this.uriInfo = embeddedServer.getURI();
		hateos = new ItemHATEOS(this.uriInfo, CONTROLLER_PATH);
		orderHATEOS = new OrderHATEOS(this.uriInfo, CONTROLLER_PATH);
	}

	@Post
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response createOrderItem(final @PathParam(value = "UUDI") String uudi, @Body final String itemRequestJson) {

		LOG.info("Receiving Order Request Payload in method updateOrder");

		Order order = new Order();

		try {

			LOG.info("Finding order by UUDI");
			order = order.findByUUDI(uudi);

			if (order.getStatus().getType() == OrderStatusType.UNPAID) {

				List<Item> items = null;

				LOG.info("Converting JSON Payload to Object");
				items = mapper.readValue(itemRequestJson, new TypeReference<List<Item>>() {
				});


				for (Item item : items) {
					order.getItems().add(item);
				}

				order = order.save(order);

				LOG.info("Generating HATEOS");
				final String HATEOS = hateos.generateHATEOS(order);
				LOG.debug(HATEOS);

				LOG.info("Returning HTTP Status " + Status.OK);
				return Response.status(Status.OK).entity(HATEOS).build();

			} else {

				LOG.info("Generating HATEOS");
				//final String HATEOS = OrderHATEOS.generateHATEOS(order,uriInfo.getBaseUri()).toJson();
				//LOG.debug(HATEOS);

				LOG.info("Returning HTTP Status" + Status.NOT_ACCEPTABLE);
				return Response.status(Status.NOT_ACCEPTABLE).entity("{\"messsage\": \"Invalid Status\" }").build();
			}

		} catch (JsonParseException e) {
			LOG.error(e.getMessage());
			return Response.status(Status.BAD_REQUEST).build();
		} catch (JsonMappingException e) {
			LOG.error(e.getMessage());
			return Response.status(Status.BAD_REQUEST).build();
		} catch (IOException e) {
			LOG.error(e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		} catch (ModelNotFoundException mnfe) {
			LOG.warn(mnfe.getMessage());
			return Response.status(Status.NOT_FOUND).entity(mnfe.getMessage()).build();
		} catch (Throwable t) {
			LOG.error(t.getMessage());
			return Response.serverError().build();
		}
	}

	@Patch
	@Path("/{ITEM_ID}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateOrderItem(final @PathParam(value = "UUDI") String uudi, final @PathParam(value = "ITEM_ID") String itemId, @Body final String itemRequestJson) {

		LOG.info("Receiving Order Request Payload in method updateOrder");

		Order order = new Order();

		try {

			LOG.info("Saving order request");
			order = order.findByUUDI(uudi);

			if (order.getStatus().getType() == OrderStatusType.UNPAID) {


				LOG.info("Generating HATEOS");
				//final String HATEOS = OrderHATEOS.generateHATEOS(order,uriInfo.getBaseUri()).toJson();
				//LOG.debug(HATEOS);

				Item item = new Item();

				LOG.info("Converting JSON Payload to Object");
				item = mapper.readValue(itemRequestJson, Item.class);

				try {
					item.setId(Long.parseLong(itemId.trim()));
				} catch (Throwable t) {
					throw new IllegalArgumentException("Item Identifier is not valid.");
				}

				if (!order.getItems().removeIf(i -> i.getId() == Long.parseLong(itemId.trim()))) {
					throw new ModelNotFoundException("Item of Order not found.");
				}
				order.getItems().add(item.save(item));

				order.updatePrice();
				order.save(order);

				LOG.info("Returning HTTP Status " + Status.OK);
				return Response.status(Status.OK).entity(order).build();

			} else {

				LOG.info("Generating HATEOS");
				//final String HATEOS = OrderHATEOS.generateHATEOS(order,uriInfo.getBaseUri()).toJson();
				//LOG.debug(HATEOS);

				LOG.info("Returning HTTP Status" + Status.NOT_ACCEPTABLE);
				return Response.status(Status.NOT_ACCEPTABLE).entity("{\"messsage\": \"Invalid Status\" }").build();
			}

		} catch (JsonParseException e) {
			LOG.error(e.getMessage());
			return Response.status(Status.BAD_REQUEST).build();
		} catch (JsonMappingException e) {
			LOG.error(e.getMessage());
			return Response.status(Status.BAD_REQUEST).build();
		} catch (IOException e) {
			LOG.error(e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		} catch (ModelNotFoundException mnfe) {
			LOG.warn(mnfe.getMessage());
			return Response.status(Status.NOT_FOUND).entity(mnfe.getMessage()).build();
		} catch (IllegalArgumentException iae) {
			LOG.warn(iae.getMessage());
			return Response.status(Status.BAD_REQUEST).entity(iae.getMessage()).build();
		} catch (Throwable t) {
			LOG.error(t.getMessage());
			return Response.serverError().build();
		}
	}

	@Delete
	@Path("/{ITEM_ID}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response deleteOrderItem(final @PathParam(value = "UUDI") String uudi, final @PathParam(value = "ITEM_ID") String itemId) {

		LOG.info("Receiving Order Request Payload in method updateOrder");

		Order order = new Order();

		try {

			LOG.info("Fiding order request");
			order = order.findByUUDI(uudi);

			if (order.getStatus().getType() == OrderStatusType.UNPAID) {

				try{
					if (!order.getItems().removeIf(i -> i.getId() == Long.parseLong(itemId.trim()))) {
						throw new ModelNotFoundException("Item of Order not found.");
					}
				}catch (ModelNotFoundException mnfe){
					throw mnfe;
				}catch (Throwable t) {
					throw new IllegalArgumentException("Item Identifier is not valid.");
				}

				Item item = new Item();

				LOG.info("Deletind item : " + itemId);
				item.deleteItemFull(Long.parseLong(itemId.trim()));

				LOG.info("Update order price");
				order.updatePrice();
				LOG.info("Updating order");
				order.save(order);

				LOG.info("Returning HTTP Status " + Status.OK);
				return Response.status(Status.OK).entity(order).build();

			} else {

				LOG.info("Returning HTTP Status" + Status.NOT_ACCEPTABLE);
				return Response.status(Status.NOT_ACCEPTABLE).entity("{\"messsage\": \"Invalid Status\" }").build();
			}

		} catch (ModelNotFoundException mnfe) {
			LOG.error(mnfe.getMessage());
			return Response.status(Status.NOT_FOUND).entity(mnfe.getMessage()).build();
		}catch (IllegalArgumentException iae) {
			LOG.error(iae.getMessage());
			return Response.status(Status.NOT_FOUND).entity(iae.getMessage()).build();
		} catch (Throwable t) {
			LOG.error(t.getMessage());
			return Response.serverError().build();
		}
	}
}