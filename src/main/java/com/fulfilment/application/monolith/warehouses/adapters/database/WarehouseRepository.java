package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class WarehouseRepository implements WarehouseStore, PanacheRepository<DbWarehouse> {

  @Override
  public List<Warehouse> getAll() {
    return this.find("archivedAt IS NULL").stream().map(DbWarehouse::toWarehouse).toList();
  }

  @Override
  @Transactional
  public void create(Warehouse warehouse) {
    DbWarehouse dbWarehouse = new DbWarehouse();
    dbWarehouse.businessUnitCode = warehouse.businessUnitCode;
    dbWarehouse.location = warehouse.location;
    dbWarehouse.capacity = warehouse.capacity;
    dbWarehouse.stock = warehouse.stock;
    dbWarehouse.createdAt = warehouse.createdAt;
    dbWarehouse.archivedAt = warehouse.archivedAt;
    this.persist(dbWarehouse);
  }

  @Override
  @Transactional
  public void update(Warehouse warehouse) {
    DbWarehouse existing =
        this.find("businessUnitCode = ?1 and archivedAt IS NULL", warehouse.businessUnitCode)
            .firstResult();
    if (existing != null) {
      existing.location = warehouse.location;
      existing.capacity = warehouse.capacity;
      existing.stock = warehouse.stock;
      existing.archivedAt = warehouse.archivedAt;
      existing.createdAt = warehouse.createdAt;
    }
  }

  @Override
  @Transactional
  public void remove(Warehouse warehouse) {
    DbWarehouse existing =
        this.find("businessUnitCode = ?1 and archivedAt IS NULL", warehouse.businessUnitCode)
            .firstResult();
    if (existing != null) {
      this.delete(existing);
    }
  }

  @Override
  public Warehouse findByBusinessUnitCode(String buCode) {
    return this.find("businessUnitCode = ?1 and archivedAt IS NULL", buCode)
        .firstResultOptional()
        .map(DbWarehouse::toWarehouse)
        .orElse(null);
  }
}
