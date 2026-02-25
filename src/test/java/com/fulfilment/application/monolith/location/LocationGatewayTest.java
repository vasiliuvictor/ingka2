package com.fulfilment.application.monolith.location;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import org.junit.jupiter.api.Test;

public class LocationGatewayTest {

  @Test
  public void testWhenResolveExistingLocationShouldReturn() {
    LocationGateway locationGateway = new LocationGateway();
    Location location = locationGateway.resolveByIdentifier("ZWOLLE-001");
    assertEquals("ZWOLLE-001", location.identification);
    assertEquals(1, location.maxNumberOfWarehouses);
    assertEquals(40, location.maxCapacity);
  }

  @Test
  public void testWhenResolveAnotherExistingLocationShouldReturn() {
    LocationGateway locationGateway = new LocationGateway();
    Location location = locationGateway.resolveByIdentifier("AMSTERDAM-001");
    assertEquals("AMSTERDAM-001", location.identification);
    assertEquals(5, location.maxNumberOfWarehouses);
    assertEquals(100, location.maxCapacity);
  }

  @Test
  public void testWhenResolveNonExistingLocationShouldReturnNull() {
    LocationGateway locationGateway = new LocationGateway();
    Location location = locationGateway.resolveByIdentifier("DOES-NOT-EXIST");
    assertNull(location);
  }
}
