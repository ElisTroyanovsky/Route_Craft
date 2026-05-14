# RouteCraft

**Multiple Traveling Salesman Problem (mTSP) Solver — Hybrid GA + ACO + 2-opt**

RouteCraft is a Java-based route optimization system that solves the Multiple Traveling Salesman Problem (mTSP) for fleets of delivery vehicles. It combines Genetic Algorithms (GA) with Ant Colony Optimization (ACO) in a unique cross-pollination loop, then applies 2-opt local search for final polish. Distances come from real road data via the Google Maps Distance Matrix API.

![Status](https://img.shields.io/badge/status-completed-success)
![License](https://img.shields.io/badge/license-Academic-blue)

### Built With

![Java](https://img.shields.io/badge/Java-SE_11%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Google Maps](https://img.shields.io/badge/Google_Maps-4285F4?style=for-the-badge&logo=googlemaps&logoColor=white)
![REST API](https://img.shields.io/badge/REST_API-009688?style=for-the-badge&logo=fastapi&logoColor=white)
![HTML5](https://img.shields.io/badge/HTML5-E34F26?style=for-the-badge&logo=html5&logoColor=white)
![JSON](https://img.shields.io/badge/JSON-000000?style=for-the-badge&logo=json&logoColor=white)
![IntelliJ IDEA](https://img.shields.io/badge/IntelliJ_IDEA-000000?style=for-the-badge&logo=intellij-idea&logoColor=white)
## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Algorithms](#algorithms)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Setup](#setup)
- [Configuration](#configuration)
- [Running](#running)
- [API Reference](#api-reference)
- [Results](#results)
- [Documentation](#documentation)
- [Author](#author)

---

## Overview

The Multiple Traveling Salesman Problem (mTSP) generalizes the classic TSP to a fleet of M vehicles departing from a shared depot (Hub) and visiting N delivery points exactly once between them. The objective is to minimize the total distance traveled by all vehicles combined.

mTSP is **NP-hard**: for 32 delivery points and 5 trucks the search space contains roughly **10³⁴ possible solutions** — exhaustive search is impossible. RouteCraft uses metaheuristics to find near-optimal solutions in seconds.

**Real-world use cases:**
- Last-mile parcel delivery
- Field-service technician dispatch
- Multi-vehicle waste collection
- Pharmacy / medical-equipment distribution

---

## Features

- **Hybrid optimization** — 15 cycles of cross-pollinated ACO ↔ GA, followed by 2-opt local polish
- **Real road distances** — integrates Google Maps Distance Matrix API (not Euclidean approximations)
- **Two-layer cache** — `MatrixCache` (in-memory, O(1)) + `PersistentCacheManager` (disk, `matrix_cache.json`) eliminates duplicate API calls across runs
- **Interactive UI** — pick points by clicking the map or searching addresses; live route visualization with Google Directions
- **Driver itineraries** — printable route sheets with deep links to Google Maps
- **Live Traffic toggle** — overlay real-time congestion on the map
- **Persistent state** — points are saved to browser `localStorage` between sessions
- **Layered architecture** — clean MVC separation with no external frameworks (only Gson for JSON parsing)

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    CLIENT (Browser)                         │
│   index.html + JavaScript + Google Maps JS API              │
└──────────────────────┬──────────────────────────────────────┘
                       │ HTTP POST /api/optimize (JSON)
                       │ JSON Response
┌──────────────────────▼──────────────────────────────────────┐
│              SERVER (Java) — Port 8080                      │
│ ┌──────────────────────────────────────────────────────────┐│
│ │ RouteServer.java                                         ││
│ │   ├─ OptimizeHandler   (POST /api/optimize)              ││
│ │   └─ StaticFileHandler (GET / → index.html)              ││
│ └──────────────────────────────────────────────────────────┘│
│ ┌────────────┐ ┌──────────────────┐ ┌──────────────────┐   │
│ │GreedySolver│ │GeneticAlgorithm  │ │AntColonyOptimizer│   │
│ │(Baseline)  │ │+ Population      │ │+ Ant             │   │
│ │            │ │+ RouteDNA        │ │                  │   │
│ └────────────┘ └────────┬─────────┘ └────────┬─────────┘   │
│                         │ cross-pollination  │             │
│                         └────────────────────┘             │
│ ┌──────────────────────────────────────────────────────────┐│
│ │ TwoOptOptimizer (final local search)                     ││
│ └──────────────────────────────────────────────────────────┘│
│ ┌──────────────────────┐  ┌────────────────────────────────┐│
│ │ MatrixCache          │  │ GoogleMapsRoutingService       ││
│ │ (RAM, O(1))          │  │ (chunks of 10, one-time)       ││
│ │ + PersistentCache    │  │                                ││
│ │   → matrix_cache.json│  │                                ││
│ └──────────────────────┘  └────────────────┬───────────────┘│
└─────────────────────────────────────────────┼───────────────┘
                                              │ HTTPS (one-time)
                              ┌───────────────▼───────────────┐
                              │ Google Maps Platform           │
                              │ Distance Matrix API            │
                              └────────────────────────────────┘
```

---

## Algorithms

RouteCraft runs a three-stage pipeline:

### Stage 1 — Greedy Baseline (Nearest Neighbor)

Each truck repeatedly picks the closest unvisited point. Fast, deterministic, used as the reference for measuring improvement.

- Complexity: `O(N² × M)`
- Quality: ~20–25% above optimum

### Stage 2 — Hybrid GA + ACO Loop (15 cycles)

The heart of the system. In each cycle:

```
a. ACO Phase    — 30 ants × 100 iterations
                  Build probabilistic tours, evaporate & deposit pheromones
b. Inject elite — 20 ACO solutions injected into GA population (slots 0–19)
                  globalBest preserved at slot 20
c. GA Phase     — 1000 generations of evolution
                  Tournament selection → OX crossover → swap mutation
d. Cross-Polli- — Best GA solution reinforces ACO pheromones (Q × 2)
   nation         so ants explore the same region next cycle
```

**Why hybrid?** ACO excels at exploration (finding promising regions); GA excels at exploitation (refining solutions). Cross-pollination lets each algorithm benefit from the other’s strengths.

### Stage 3 — 2-opt Polish

Eliminates remaining route crossings by reversing edge pairs whose swap reduces distance. Applied per truck. Used only if it actually improves the result.

### Fitness Function (mathematical model)

```
TotalDistance(R₁,…,Rₘ) = Σ RouteDistance(Rₘ)
RouteDistance(Rₘ)      = D[Hub][Rₘ[1]] + Σ D[Rₘ[i]][Rₘ[i+1]] + D[Rₘ[last]][Hub]
fitness(solution)      = 1 / TotalDistance(solution)
```

Distances `D[i][j]` come from Google Maps Distance Matrix (real road km).

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java SE 11+ |
| HTTP server | `com.sun.net.httpserver.HttpServer` (built-in) |
| JSON parsing | Gson 2.10.1 (only external dependency) |
| Frontend | Pure HTML5 + JavaScript (no frameworks) |
| Mapping | Google Maps JavaScript API + Distance Matrix API + Directions + Places + Geocoding |
| Persistence | `matrix_cache.json` on disk + `localStorage` in browser |
| IDE | IntelliJ IDEA |

---

## Project Structure

```
Route_Craft/
├── src/
│   ├── api/
│   │   └── RouteServer.java                # HTTP server, request routing
│   ├── app/
│   │   └── Main.java                       # Entry point
│   ├── domain/
│   │   ├── Location.java                   # Geographic point model
│   │   └── Truck.java                      # (legacy — early dev only)
│   ├── optimizer/
│   │   ├── GreedySolver.java               # Baseline (Nearest Neighbor)
│   │   ├── TwoOptOptimizer.java            # 2-opt local search
│   │   ├── ga/
│   │   │   ├── GeneticAlgorithm.java       # GA evolution loop
│   │   │   ├── Population.java             # Population of chromosomes
│   │   │   └── RouteDNA.java               # Chromosome representation
│   │   └── aco/
│   │       ├── Ant.java                    # Single artificial ant
│   │       └── AntColonyOptimizer.java     # Colony management + cross-pollination
│   └── routing/
│       ├── DistanceMatrix.java             # Distance provider interface
│       ├── MatrixCache.java                # In-memory distance cache
│       ├── PersistentCacheManager.java     # Disk-backed cache
│       └── GoogleMapsRoutingService.java   # Google API integration
├── index.html                              # Web UI
├── config.example.js                       # Config template (commit-safe)
├── config.js                               # Real API key (gitignored)
├── matrix_cache.json                       # Persistent distance cache
└── README.md
```

---

## Setup

### Prerequisites

- **Java SE 11 or higher** (verified on Java 17)
- **IntelliJ IDEA 2023+** (Community Edition is fine) or any Java IDE
- **Google Cloud account** with billing enabled and these APIs enabled:
    - Distance Matrix API
    - Maps JavaScript API
    - Places API
    - Geocoding API
    - Directions API
- A modern browser (Chrome 90+ / Firefox 88+)

### Clone & Open

```bash
git clone https://github.com/ElisTroyanovsky/Route_Craft
cd Route_Craft
```

Open the project in IntelliJ IDEA: `File → Open → Route_Craft`.

### Add Gson Library

The project uses Gson 2.10.1 (single external JAR, ~250 KB):

```
File → Project Structure → Libraries → + → Java
   → select gson-2.10.1.jar → OK → Apply
```

---

## Configuration

Copy `config.example.js` to `config.js` and replace the placeholder with your real Google Maps API key:

```javascript
// config.js
const CONFIG = {
    GOOGLE_MAPS_API_KEY: "AIza...YOUR_REAL_KEY_HERE"
};
```

> **Important:** `config.js` is listed in `.gitignore` — never commit it to GitHub.

---

## Running

1. Run `src/app/Main.java` in IntelliJ (or `Shift+F10`).
2. Console output:
   ```
   === Route Craft System Starting ===
   Server is running -> http://localhost:8080
   Waiting for data from the frontend...
   ```
3. Open `http://localhost:8080` in your browser.
4. Set a Hub (red pin) and add delivery points (blue pins) by clicking on the map or using the search bar.
5. Set vehicle count (default: 3).
6. Click **START OPTIMIZATION**.
7. Routes appear on the map within seconds; the log panel shows convergence progress.

---

## API Reference

### `POST /api/optimize`

**Request body:**

```json
{
  "trucks": 5,
  "hub": { "lat": 32.0853, "lng": 34.7818 },
  "locations": [
    { "id": "P0", "lat": 32.1234, "lng": 34.8765 },
    { "id": "P1", "lat": 32.0567, "lng": 34.7123 }
  ]
}
```

**Success response (HTTP 200):**

```json
{
  "status": "success",
  "greedyDistance": 402.17,
  "gaDistance": 349.46,
  "log": [
    "Greedy Baseline: 402.17 km",
    "Cycle 5 | ACO: 404.53 -> GA: 378.36",
    "Cycle 10 | ACO: 424.59 -> GA: 368.64",
    "Cycle 15 | ACO: 406.85 -> GA: 354.06",
    "FINAL 2-opt: 349.46 km (Improved!)"
  ],
  "routes": [
    [{"lat": 32.12, "lng": 34.87}, {"lat": 32.05, "lng": 34.71}],
    [{"lat": 32.33, "lng": 34.95}, ...]
  ]
}
```

**Error response (HTTP 500):**

```json
{
  "status": "error",
  "message": "No valid route found"
}
```

---

## Results

Reference benchmark — 32 delivery points across the Gush Dan region (Israel), 5 trucks:

| Stage | Distance | Improvement |
|-------|---------:|------------:|
| Greedy Baseline | 402.17 km | — |
| Hybrid GA + ACO | 354.06 km | 11.9% |
| GA + ACO + 2-opt | **349.46 km** | **13.11%** |

Convergence over 15 hybrid cycles (excerpt):

```
Cycle  5 | ACO: 404.53 -> GA: 378.36
Cycle 10 | ACO: 424.59 -> GA: 368.64
Cycle 15 | ACO: 406.85 -> GA: 354.06
FINAL 2-opt: 349.46 km (Improved!)
```

**Runtime:**
- First run (Google API populates cache): ~10 seconds
- Subsequent runs (cached): ~3 seconds

**Scaling:** verified for 32+ points × 5 trucks; reaches ~13–23% savings vs. greedy depending on geometry.

---

## Algorithm Parameters

These constants are tuned in code (not exposed in the UI):

### Genetic Algorithm

| Parameter | Value | Where |
|-----------|-------|-------|
| Population size | 100 | RouteServer.java |
| Generations / cycle | 1000 | RouteServer.java |
| Mutation rate | 0.10 | GeneticAlgorithm.java |
| Tournament size | 3 | GeneticAlgorithm.java |
| Elitism | true | GeneticAlgorithm.java |

### Ant Colony Optimization

| Parameter | Value | Meaning |
|-----------|-------|---------|
| α (alpha) | 1.0 | Pheromone weight |
| β (beta) | 2.0 | Heuristic (distance) weight |
| ρ (evaporation) | 0.5 | Pheromone decay per iteration |
| Q | 500 | Pheromone deposit constant |
| antCount | 30 | Ants per iteration |
| Iterations / cycle | 100 | ACO inner loop |

### Hybrid Loop

| Parameter | Value |
|-----------|-------|
| Total cycles | 15 |
| Elite injection size | 20 |
| `globalBest` slot | 20 |
| Pheromone reinforcement (cross-poll.) | Q × 2 = 1000 |

---

## Documentation

The full project book (in Hebrew + English, ~140 pages) includes:

- Theoretical background of TSP / mTSP
- Survey of 6 alternative algorithms (Brute Force, Held-Karp, Greedy, GA, ACO, Simulated Annealing)
- SWOT analysis for the chosen algorithms
- UML class & use-case diagrams
- Top-down architecture breakdown
- Pseudocode for every key function
- Personal reflection
- APA-style bibliography

See `Route Craft.docx` / `Route Craft.pdf` in the parent directory.

---

## Limitations & Future Work

- Capacity / time-window constraints (extension to VRP) not implemented
- Single depot only (no multi-depot mTSP)
- No load balancing constraint between vehicles (only total distance is minimized)
- Single-threaded execution (could be parallelized at the ant level)
- Real-time traffic data shown visually but not used in optimization (distances are static after first fetch)

---

## Author

**Elis Troyanovsky** — Bari WAN TEC, Class of 13 (Software Engineering)
Final project, 2025–2026.

Supervisor: Yuda Or.

GitHub: [@ElisTroyanovsky](https://github.com/ElisTroyanovsky)

---

## License

Academic project. Code provided for educational reference. Google Maps Platform usage is subject to Google’s terms of service. The author retains all rights to the original implementation.