package org.redeasy.shop.proxy;

import java.util.List;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.redeasy.shop.entity.Car;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/cars")
@Produces(MediaType.APPLICATION_JSON)
@RegisterRestClient(baseUri = "stork://car-service")
public interface CarProxy {

    @GET
    @Path("shop/{idShop}")
    List<Car> getCarsByShopId(@PathParam("idShop") Long idShop);

}
