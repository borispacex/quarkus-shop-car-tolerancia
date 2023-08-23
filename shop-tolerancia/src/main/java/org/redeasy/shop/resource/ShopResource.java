package org.redeasy.shop.resource;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.faulttolerance.*;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.redeasy.shop.entity.Car;
import org.redeasy.shop.entity.Shop;
import org.redeasy.shop.proxy.CarProxy;
import org.redeasy.shop.repository.CarRepository;
import org.redeasy.shop.repository.ShopRepository;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Path("/shops")
public class ShopResource {

    @RestClient
    CarProxy carProxy;

    @Inject
    ShopRepository shopRepository;

    // Metodo con Tolerancia a fallos, Tiempo de espera
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Timeout(5000)
    public Response getAll() {
        pausa(3000L); // si es mayor al timeout falla
        List<Shop> shops = shopRepository.listAll();
        return Response.ok(shops).build();
    }
    private void pausa(Long milisegundos) {
        try {
            TimeUnit.MILLISECONDS.sleep(milisegundos);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Metodo get Cars con Tolerancia a fallos
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id}")
    @Retry(maxRetries = 4)
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

    // Metodo obtenido desde Car, usando Tolerancia a fallos
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id}/cars")
    @Fallback(fallbackMethod = "fallbackGetCars")
    @CircuitBreaker(requestVolumeThreshold = 4, failureRatio = 0.5, delay = 4000, successThreshold = 2)
    @Bulkhead(value = 2, waitingTaskQueue = 10)
    public Response getCars(@PathParam("id") Long id) {
        List<Car> cars = carProxy.getCarsByShopId(id);
        System.out.println("[INFO] Consultando el {id: " + id + "} desde el metodo getCars");
        return Response.ok(cars).build();
    }

    // Metodo si ocurre un fallo, nos vamos a ejecuta este metodo
    private Response fallbackGetCars(Long id) {
        List<Car> cars = new ArrayList<Car>();
        System.out.println("[INFO] Consultando el {id: " + id + "} desde el fallback method fallbackGetCars");
        return Response.ok(cars).build();
    }

}
