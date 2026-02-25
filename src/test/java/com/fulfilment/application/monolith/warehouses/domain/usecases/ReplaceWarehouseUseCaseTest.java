package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReplaceWarehouseUseCaseTest {

  @Mock private WarehouseStore warehouseStore;
  @Mock private LocationResolver locationResolver;

  private ReplaceWarehouseUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase = new ReplaceWarehouseUseCase(warehouseStore, locationResolver);
  }

  @Test
  void testReplace_Success() {
    Warehouse oldWarehouse = existingWarehouse();
    Warehouse newWarehouse = replacementWarehouse();
    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(oldWarehouse);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));
    doNothing().when(warehouseStore).update(any());
    doNothing().when(warehouseStore).create(any());

    useCase.replace(newWarehouse);

    // Old warehouse must be archived.
    assertNotNull(oldWarehouse.archivedAt);
    verify(warehouseStore).update(oldWarehouse);
    // New warehouse must be created.
    assertNotNull(newWarehouse.createdAt);
    verify(warehouseStore).create(newWarehouse);
  }

  @Test
  void testReplace_WarehouseNotFound_ShouldThrow404() {
    Warehouse newWarehouse = replacementWarehouse();
    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(null);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> useCase.replace(newWarehouse));
    assert ex.getResponse().getStatus() == 404;
    verify(warehouseStore, never()).update(any());
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void testReplace_InvalidLocation_ShouldThrow400() {
    Warehouse oldWarehouse = existingWarehouse();
    Warehouse newWarehouse = replacementWarehouse();
    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(oldWarehouse);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(null);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> useCase.replace(newWarehouse));
    assert ex.getResponse().getStatus() == 400;
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void testReplace_NewCapacityTooSmallForOldStock_ShouldThrow400() {
    Warehouse oldWarehouse = existingWarehouse();
    oldWarehouse.stock = 50;
    Warehouse newWarehouse = replacementWarehouse();
    newWarehouse.capacity = 30; // less than oldWarehouse.stock = 50
    newWarehouse.stock = 50;
    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(oldWarehouse);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> useCase.replace(newWarehouse));
    assert ex.getResponse().getStatus() == 400;
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void testReplace_StockMismatch_ShouldThrow400() {
    Warehouse oldWarehouse = existingWarehouse(); // stock = 10
    Warehouse newWarehouse = replacementWarehouse();
    newWarehouse.stock = 99; // doesn't match old stock of 10
    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(oldWarehouse);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> useCase.replace(newWarehouse));
    assert ex.getResponse().getStatus() == 400;
    verify(warehouseStore, never()).create(any());
  }

  private Warehouse existingWarehouse() {
    Warehouse w = new Warehouse();
    w.businessUnitCode = "MWH.001";
    w.location = "ZWOLLE-001";
    w.capacity = 100;
    w.stock = 10;
    return w;
  }

  private Warehouse replacementWarehouse() {
    Warehouse w = new Warehouse();
    w.businessUnitCode = "MWH.001";
    w.location = "AMSTERDAM-001";
    w.capacity = 80;
    w.stock = 10; // matches old warehouse stock
    return w;
  }
}
