package app;

import api.RouteServer;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Route Craft System Starting ===");

        // Start the HTTP server to listen for requests from the web interface
        RouteServer.startServer();

        System.out.println("Waiting for data from the frontend...");

        // Note: We no longer generate random points or run the algorithm here.
        // The algorithm is triggered dynamically when data is received via the API.
    }
}