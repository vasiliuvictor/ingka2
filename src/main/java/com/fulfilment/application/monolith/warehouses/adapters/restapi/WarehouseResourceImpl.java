package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.warehouse.api.WarehouseResource;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.WebApplicationException;
import java.util.List;

@RequestScoped
public class WarehouseResourceImpl implements WarehouseResource {

  @Inject private WarehouseRepository warehouseRepository;
  @Inject private CreateWarehouseOperation createWarehouseOperation;
  @Inject private ArchiveWarehouseOperation archiveWarehouseOperation;
  @Inject private ReplaceWarehouseOperation replaceWarehouseOperation;

  @Override
  public List<com.warehouse.api.beans.Warehouse> listAllWarehousesUnits() {
    return warehouseRepository.getAll().stream().map(this::toWarehouseResponse).toList();
  }

  @Override
  public com.warehouse.api.beans.Warehouse createANewWarehouseUnit(
      @NotNull com.warehouse.api.beans.Warehouse data) {
    Warehouse warehouse = toDomain(data);
    createWarehouseOperation.create(warehouse);
    return toWarehouseResponse(warehouse);
  }

  @Override
  public com.warehouse.api.beans.Warehouse getAWarehouseUnitByID(String id) {
    DbWarehouse dbWarehouse = findActiveDbWarehouseById(id);
    return toWarehouseResponse(dbWarehouse.toWarehouse());
  }

  @Override
  public void archiveAWarehouseUnitByID(String id) {
    DbWarehouse dbWarehouse = findActiveDbWarehouseById(id);
    archiveWarehouseOperation.archive(dbWarehouse.toWarehouse());
  }

  @Override
  public com.warehouse.api.beans.Warehouse replaceTheCurrentActiveWarehouse(
      String businessUnitCode, @NotNull com.warehouse.api.beans.Warehouse data) {
    Warehouse newWarehouse = toDomain(data);
    newWarehouse.businessUnitCode = businessUnitCode;
    replaceWarehouseOperation.replace(newWarehouse);
    return toWarehouseResponse(newWarehouse);
  }

  private DbWarehouse findActiveDbWarehouseById(String id) {
    long longId;
    try {
      longId = Long.parseLong(id);
    } catch (NumberFormatException e) {
      throw new WebApplicationException("Invalid warehouse ID: " + id, 400);
    }
    DbWarehouse dbWarehouse = warehouseRepository.findById(longId);
    if (dbWarehouse == null || dbWarehouse.archivedAt != null) {
      throw new WebApplicationException("Warehouse not found with id: " + id, 404);
    }
    return dbWarehouse;
  }

  private Warehouse toDomain(com.warehouse.api.beans.Warehouse data) {
    var warehouse = new Warehouse();
    warehouse.businessUnitCode = data.getBusinessUnitCode();
    warehouse.location = data.getLocation();
    warehouse.capacity = data.getCapacity();
    warehouse.stock = data.getStock();
    return warehouse;
  }

  private com.warehouse.api.beans.Warehouse toWarehouseResponse(Warehouse warehouse) {
    var response = new com.warehouse.api.beans.Warehouse();
    response.setBusinessUnitCode(warehouse.businessUnitCode);
    response.setLocation(warehouse.location);
    response.setCapacity(warehouse.capacity);
    response.setStock(warehouse.stock);
    return response;
  }
}
