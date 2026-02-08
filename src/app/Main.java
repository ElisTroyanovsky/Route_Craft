package app;
import routing.GoogleMapsRoutingService;
import routing.RoutingService;
import app.AppConfig;


/**
 * Application entry point.
 * Wires together services (auth, storage, routing, optimizers) and runs a single optimization flow.
 *
 * TODO:
 * - Parse UI input (depot, stops, trucks, constraints).
 * - Authenticate user.
 * - Load/save Problem/Solution/Run via storage layer.
 * - Choose algorithm (GA/ACO) via config/args.
 * - Print/export results (routes, total distance, links).
 */

public class Main {
    public static void main(String[] args) {

        RoutingService routingService =
                new GoogleMapsRoutingService(
                        AppConfig.googleMapsApiKey()
                );

        System.out.println("Routing service initialized");
    }
}