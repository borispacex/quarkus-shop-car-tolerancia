package org.redeasy.shop.resource;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.redeasy.shop.entity.Car;
import org.redeasy.shop.entity.Shop;
import org.redeasy.shop.proxy.CarProxy;
import org.redeasy.shop.repository.CarRepository;
import org.redeasy.shop.repository.ShopRepository;

import java.net.URI;
import java.util.List;

@Path("/shops")
public class ShopResource {

    @RestClient
    CarProxy carProxy;

    @Inject
    ShopRepository shopRepository;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        List<Shop> shops = shopRepository.listAll();
        return Response.ok(shops).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id}")
    public Response getById(@PathParam("id") Long id) {
        Shop shop = shopRepository.findById(id);
        if (shopRepository.isPersistent(shop)) {
            return Response.ok(shop).build();
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/state/{state}")
    public Response getByState(@PathParam("state") String state) {
        List<Shop> shops = shopRepository.list("SELECT s FROM Shop s WHERE s.state = ?1 ORDER BY id", state);
        return Response.ok(shops).build();
    }

    @POST
    @Transactional
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(Shop shop) {
        shopRepository.persist(shop);
        if (shopRepository.isPersistent(shop)) {
            return Response.created(URI.create("shops" + shop.getId())).build();
        }
        return Response.status(Response.Status.BAD_REQUEST).build();
    }

    @DELETE
    @Transactional
    @Path("{id}")
    public Response deleteById(@PathParam("id") Long id) {
        boolean deleted = shopRepository.deleteById(id);
        if (deleted) {
            return Response.noContent().build();
        }
        return Response.status(Response.Status.BAD_REQUEST).build();
    }

    // Metodo obtenido desde Car
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id}/cars")
    public Response getCars(@PathParam("id") Long id) {
        List<Car> cars = carProxy.getCarsByShopId(id);
        return Response.ok(cars).build();
    }
}
