package com.nigahae.user.domain;

import com.nigahae.user.domain.preference.PreferredTravelTheme;
import com.nigahae.user.domain.preference.TransportationMode;
import com.nigahae.user.domain.preference.TravelCompanion;
import com.nigahae.user.domain.preference.TravelTime;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "\"User\"", uniqueConstraints =
        @UniqueConstraint(name = "uk_user_provider_subject", columnNames = {"provider", "provider_user_id"}))
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Builder.Default
    @Column(name = "has_travel_plan", nullable = false)
    private Boolean hasTravelPlan = false;

    @Column(name = "travel_time")
    private Long travelTime;

    @Column(name = "travel_companion")
    private Long travelCompanion;

    @Column(name = "preferred_travel_theme")
    private Long preferredTravelTheme;

    @Column(name = "transportation_mode")
    private Long transportationMode;

    @Column(name = "language")
    private Long language;

    @Column(length = 30)
    private String provider;

    @Column(name = "provider_user_id", length = 100)
    private String providerUserId;

    @Column(length = 100)
    private String nickname;

    @Column(length = 320)
    private String email;

    @Column(name = "profile_image_url", length = 2048)
    private String profileImageUrl;

    public void updateProfile(String nickname, String email, String profileImageUrl) {
        this.nickname = nickname;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
    }

    @PrePersist
    private void applyDefaults() {
        if (hasTravelPlan == null) {
            hasTravelPlan = false;
        }
    }

    public void updatePreferences(
            boolean hasTravelPlan,
            TravelTime travelTime,
            TravelCompanion travelCompanion,
            PreferredTravelTheme preferredTravelTheme,
            TransportationMode transportationMode
    ) {
        this.hasTravelPlan = hasTravelPlan;
        this.travelTime = travelTime.code();
        this.travelCompanion = travelCompanion.code();
        this.preferredTravelTheme = preferredTravelTheme == null ? null : preferredTravelTheme.code();
        this.transportationMode = transportationMode.code();
    }
}
