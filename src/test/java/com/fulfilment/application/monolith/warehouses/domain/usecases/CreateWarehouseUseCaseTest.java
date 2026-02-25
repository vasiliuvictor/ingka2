package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.ws.rs.WebApplicationException;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CreateWarehouseUseCaseTest {

  @Mock private WarehouseStore warehouseStore;
  @Mock private LocationResolver locationResolver;

  private CreateWarehouseUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase = new CreateWarehouseUseCase(warehouseStore, locationResolver);
  }

  @Test
  void testCreate_Success() {
    Warehouse warehouse = validWarehouse();
    when(warehouseStore.findByBusinessUnitCode("MWH.NEW")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("TILBURG-001"))
        .thenReturn(new Location("TILBURG-001", 1, 40));
    when(warehouseStore.getAll()).thenReturn(Collections.emptyList());
    doNothing().when(warehouseStore).create(any());

    useCase.create(warehouse);

    verify(warehouseStore).create(warehouse);
    assertNotNull(warehouse.createdAt);
  }

  @Test
  void testCreate_DuplicateBusinessUnitCode_ShouldThrow400() {
    Warehouse warehouse = validWarehouse();
    when(warehouseStore.findByBusinessUnitCode("MWH.NEW")).thenReturn(new Warehouse());

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> useCase.create(warehouse));
    assert ex.getResponse().getStatus() == 400;
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void testCreate_InvalidLocation_ShouldThrow400() {
    Warehouse warehouse = validWarehouse();
    when(warehouseStore.findByBusinessUnitCode("MWH.NEW")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("TILBURG-001")).thenReturn(null);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> useCase.create(warehouse));
    assert ex.getResponse().getStatus() == 400;
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void testCreate_MaxWarehousesExceeded_ShouldThrow400() {
    Warehouse warehouse = validWarehouse();
    when(warehouseStore.findByBusinessUnitCode("MWH.NEW")).thenReturn(null);
    // Location allows only 1 warehouse
    when(locationResolver.resolveByIdentifier("TILBURG-001"))
        .thenReturn(new Location("TILBURG-001", 1, 40));
    // One warehouse already exists at this location
    Warehouse existing = new Warehouse();
    existing.location = "TILBURG-001";
    existing.capacity = 10;
    when(warehouseStore.getAll()).thenReturn(java.util.List.of(existing));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> useCase.create(warehouse));
    assert ex.getResponse().getStatus() == 400;
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void testCreate_CapacityExceedsLocationMax_ShouldThrow400() {
    Warehouse warehouse = validWarehouse();
    warehouse.capacity = 40; // same as maxCapacity, no room for existing
    when(warehouseStore.findByBusinessUnitCode("MWH.NEW")).thenReturn(null);
    // Location capacity max = 40, but 30 already used
    when(locationResolver.resolveByIdentifier("TILBURG-001"))
        .thenReturn(new Location("TILBURG-001", 2, 40));
    Warehouse existing = new Warehouse();
    existing.location = "TILBURG-001";
    existing.capacity = 30;
    when(warehouseStore.getAll()).thenReturn(java.util.List.of(existing));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> useCase.create(warehouse));
    assert ex.getResponse().getStatus() == 400;
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void testCreate_StockExceedsCapacity_ShouldThrow400() {
    Warehouse warehouse = validWarehouse();
    warehouse.capacity = 5;
    warehouse.stock = 10; // stock > capacity
    when(warehouseStore.findByBusinessUnitCode("MWH.NEW")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("TILBURG-001"))
        .thenReturn(new Location("TILBURG-001", 2, 40));
    when(warehouseStore.getAll()).thenReturn(Collections.emptyList());

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> useCase.create(warehouse));
    assert ex.getResponse().getStatus() == 400;
    verify(warehouseStore, never()).create(any());
  }

  private Warehouse validWarehouse() {
    Warehouse w = new Warehouse();
    w.businessUnitCode = "MWH.NEW";
    w.location = "TILBURG-001";
    w.capacity = 20;
    w.stock = 5;
    return w;
  }
}
