package com.nigahae.location.controller;

import com.nigahae.location.domain.RouteTransportType;
import com.nigahae.location.dto.CoordinateRequest;
import com.nigahae.location.dto.RouteCalculationRequest;
import com.nigahae.location.service.LocationService;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class LocationControllerValidationTest {

    @Test
    void routeRequestConstraintsDoNotConflictWithControllerMethod() throws Exception {
        var controller = new LocationController(mock(LocationService.class));
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var executable = factory.getValidator().forExecutables();
            var point = new CoordinateRequest("불국사", 129.3319, 35.7900);

            assertThat(executable.validateParameters(controller,
                    method("calculateRoute", RouteCalculationRequest.class),
                    new Object[]{new RouteCalculationRequest(point, point, RouteTransportType.WALK)})).isEmpty();
        }
    }

    private Method method(String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        return LocationController.class.getMethod(name, parameterTypes);
    }
}
