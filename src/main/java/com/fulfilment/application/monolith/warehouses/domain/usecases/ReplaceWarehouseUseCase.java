package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import java.time.LocalDateTime;

@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public ReplaceWarehouseUseCase(
      WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  @Transactional
  public void replace(Warehouse newWarehouse) {
    // 1. The warehouse being replaced must exist and be active.
    Warehouse oldWarehouse =
        warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);
    if (oldWarehouse == null) {
      throw new WebApplicationException(
          "Active warehouse not found for business unit code: " + newWarehouse.businessUnitCode,
          404);
    }

    // 2. New warehouse location must be valid.
    Location location = locationResolver.resolveByIdentifier(newWarehouse.location);
    if (location == null) {
      throw new WebApplicationException("Invalid location: " + newWarehouse.location, 400);
    }

    // 3. New warehouse capacity must be able to accommodate the stock from the old warehouse.
    if (newWarehouse.capacity < oldWarehouse.stock) {
      throw new WebApplicationException(
          "New warehouse capacity is insufficient to accommodate the current stock of "
              + oldWarehouse.stock,
          400);
    }

    // 4. New warehouse stock must match the stock of the warehouse being replaced.
    if (!newWarehouse.stock.equals(oldWarehouse.stock)) {
      throw new WebApplicationException(
          "New warehouse stock must match the replaced warehouse stock of " + oldWarehouse.stock,
          400);
    }

    // 5. Archive the old warehouse.
    oldWarehouse.archivedAt = LocalDateTime.now();
    warehouseStore.update(oldWarehouse);

    // 6. Create the new warehouse under the same business unit code.
    newWarehouse.createdAt = LocalDateTime.now();
    warehouseStore.create(newWarehouse);
  }
}
