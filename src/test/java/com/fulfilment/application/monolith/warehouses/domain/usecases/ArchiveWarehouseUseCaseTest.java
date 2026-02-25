package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ArchiveWarehouseUseCaseTest {

  @Mock private WarehouseStore warehouseStore;

  private ArchiveWarehouseUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase = new ArchiveWarehouseUseCase(warehouseStore);
  }

  @Test
  void testArchive_SetsArchivedAtAndUpdates() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.001";
    warehouse.location = "ZWOLLE-001";
    warehouse.capacity = 100;
    warehouse.stock = 10;
    doNothing().when(warehouseStore).update(any());

    useCase.archive(warehouse);

    assertNotNull(warehouse.archivedAt);
    verify(warehouseStore).update(warehouse);
  }
}
