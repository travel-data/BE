package com.nigahae.user.controller.dto;

import com.nigahae.user.domain.preference.PreferredTravelTheme;
import com.nigahae.user.domain.preference.TransportationMode;
import com.nigahae.user.domain.preference.TravelCompanion;
import com.nigahae.user.domain.preference.TravelTime;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserPreferencesRequestTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void acceptsCompleteRequestWithOrWithoutOptionalValues() {
        UserPreferencesRequest withOptionalValues = validRequest();
        UserPreferencesRequest withoutOptionalValues = new UserPreferencesRequest(
                true,
                TravelTime.ONE_DAY,
                TravelCompanion.PARTNER,
                null,
                TransportationMode.WALK_PUBLIC_TRANSIT
        );

        assertThat(validator.validate(withOptionalValues)).isEmpty();
        assertThat(validator.validate(withoutOptionalValues)).isEmpty();
    }

    @Test
    void rejectsMissingRequiredSelections() {
        UserPreferencesRequest request = new UserPreferencesRequest(
                null,
                null,
                null,
                null,
                null
        );

        Set<String> invalidProperties = validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());

        assertThat(invalidProperties)
                .contains("hasTravelPlan", "travelTime", "travelCompanion", "transportationMode");
    }

    private UserPreferencesRequest validRequest() {
        return new UserPreferencesRequest(
                true,
                TravelTime.ONE_DAY,
                TravelCompanion.PARTNER,
                PreferredTravelTheme.HISTORY_CULTURE,
                TransportationMode.WALK_PUBLIC_TRANSIT
        );
    }
}
