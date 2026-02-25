package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import java.time.LocalDateTime;

@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public CreateWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  @Transactional
  public void create(Warehouse warehouse) {
    // 1. Business Unit Code must be unique among active warehouses.
    if (warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode) != null) {
      throw new WebApplicationException(
          "Business unit code already exists: " + warehouse.businessUnitCode, 400);
    }

    // 2. Location must be a known, valid location.
    Location location = locationResolver.resolveByIdentifier(warehouse.location);
    if (location == null) {
      throw new WebApplicationException("Invalid location: " + warehouse.location, 400);
    }

    // 3. The location must not have reached its maximum number of warehouses.
    long warehousesAtLocation =
        warehouseStore.getAll().stream()
            .filter(w -> w.location.equals(location.identification))
            .count();
    if (warehousesAtLocation >= location.maxNumberOfWarehouses) {
      throw new WebApplicationException(
          "Maximum number of warehouses reached for location: " + warehouse.location, 400);
    }

    // 4. The sum of warehouse capacities at this location must not exceed the location maximum.
    int totalCapacityAtLocation =
        warehouseStore.getAll().stream()
            .filter(w -> w.location.equals(location.identification))
            .mapToInt(w -> w.capacity)
            .sum();
    if (totalCapacityAtLocation + warehouse.capacity > location.maxCapacity) {
      throw new WebApplicationException(
          "Warehouse capacity exceeds the maximum total capacity for location: "
              + warehouse.location,
          400);
    }

    // 5. Stock cannot exceed the warehouse's own capacity.
    if (warehouse.stock != null && warehouse.capacity != null
        && warehouse.stock > warehouse.capacity) {
      throw new WebApplicationException(
          "Warehouse stock cannot exceed its capacity.", 400);
    }

    warehouse.createdAt = LocalDateTime.now();
    warehouseStore.create(warehouse);
  }
}
