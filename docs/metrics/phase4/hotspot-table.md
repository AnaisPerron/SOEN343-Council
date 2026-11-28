Cells: NLOC / cyclomatic complexity / cognitive complexity / SonarQube code smells. Source: `docs/metrics/<snapshot>/sonarqube-metrics.csv`.

| Class | phase3 | phase4-refactored | phase4 |
| --- | --- | --- | --- |
| **Step 1 hotspots** | | | |
| `OrderService` | 403 / 68 / 37 / 0 | 295 / 35 / 11 / 0 | 295 / 35 / 11 / 0 |
| `CustomerOrderService` | 194 / 42 / 38 / 0 | 134 / 16 / 9 / 0 | 134 / 16 / 9 / 0 |
| `Order` | 186 / 60 / 25 / 0 | 186 / 60 / 25 / 0 | 186 / 60 / 25 / 0 |
| `AssignmentService` | 132 / 52 / 40 / 0 | 114 / 41 / 28 / 0 | 114 / 41 / 28 / 0 |
| `CustomerOrderHistoryService` | 74 / 14 / 6 / 0 | 74 / 14 / 6 / 0 | 74 / 14 / 6 / 0 |
| **Added by the refactorings (R1-R4)** | | | |
| `OrderPreparation` | - | 104 / 27 / 21 / 0 | 104 / 27 / 21 / 0 |
| `OrderResponseAssembler` | - | 128 / 33 / 25 / 0 | 128 / 33 / 25 / 0 |
| `AssignmentAdoption` | - | 44 / 13 / 12 / 0 | 44 / 13 / 12 / 0 |
| `OrderAccessPolicy` | - | 15 / 5 / 1 / 0 | 15 / 5 / 1 / 0 |
| **Changed by R4 (ownership checks)** | | | |
| `OrderCancellationService` | 65 / 11 / 5 / 0 | 65 / 10 / 4 / 0 | 65 / 10 / 4 / 0 |
| `OrderBelongingService` | 39 / 10 / 6 / 0 | 34 / 8 / 4 / 0 | 34 / 8 / 4 / 0 |
| **RVW module (added)** | | | |
| `OrderRatingController` | - | - | 41 / 4 / 1 / 0 |
| `OrderRatingResponseDto` | - | - | 11 / 1 / 0 / 0 |
| `OrderRatingViewDto` | - | - | 4 / 0 / 0 / 0 |
| `RestaurantRatingSummary` | - | - | 4 / 0 / 0 / 0 |
| `SubmitRatingRequestDto` | - | - | 17 / 0 / 0 / 0 |
| `OrderRating` | - | - | 68 / 16 / 4 / 0 |
| `InvalidRatingException` | - | - | 6 / 1 / 0 / 0 |
| `OrderAlreadyRatedException` | - | - | 6 / 1 / 0 / 0 |
| `OrderRatingRepository` | - | - | 20 / 0 / 0 / 0 |
| `OrderRatingService` | - | - | 101 / 24 / 16 / 0 |
| `RestaurantRatingService` | - | - | 38 / 8 / 4 / 0 |
| **Existing classes modified by RVW** | | | |
| `BrowseService` | 75 / 10 / 2 / 0 | 75 / 10 / 2 / 0 | 87 / 10 / 2 / 0 |
| `RestaurantResponseDTO` | 9 / 0 / 0 / 0 | 9 / 0 / 0 / 0 | 11 / 0 / 0 / 0 |
| `GlobalExceptionHandler` | 168 / 26 / 9 / 0 | 168 / 26 / 9 / 0 | 180 / 28 / 9 / 0 |
