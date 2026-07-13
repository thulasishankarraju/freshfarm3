package com.example.freshfarm3.service;

import com.example.freshfarm3.entity.Address;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * DeliveryChargeService — computes the delivery charge for an order.
 *
 * All stock ships from the farm's single collection point in Tirupati.
 * Charge = straight-line distance (km) from Tirupati to the buyer's
 * delivery address × ₹10/km, rounded up to the next whole km so a
 * fraction of a km still counts as a full km (matches how local delivery
 * pricing is normally quoted), with a minimum charge floor so very close
 * addresses aren't delivered for free.
 *
 * Distance requires the address to have latitude/longitude saved on it
 * (captured client-side via the browser's geolocation API when the buyer
 * adds an address). If an address has no coordinates yet — e.g. it was
 * added before this feature, or the buyer skipped geolocation — we fall
 * back to a flat charge instead of guessing a distance.
 *
 * This is the single source of truth for delivery pricing: both the
 * checkout preview (AddressService) and the actual order (OrderService)
 * call this same method, so what the buyer is shown always matches what
 * they're charged.
 */
@Service
public class DeliveryChargeService {

    // Tirupati, Andhra Pradesh — where all farm stock is held.
    public static final double FARM_LATITUDE = 13.6288;
    public static final double FARM_LONGITUDE = 79.4192;

    public static final BigDecimal RATE_PER_KM = BigDecimal.valueOf(10);
    public static final BigDecimal MIN_CHARGE = BigDecimal.valueOf(20);

    // Used only when the address has no saved coordinates.
    private static final BigDecimal FALLBACK_FLAT_CHARGE = BigDecimal.valueOf(50);

    private static final int EARTH_RADIUS_KM = 6371;

    /** Straight-line distance in km, or null if the address has no saved coordinates. */
    public Double distanceKmTo(Address address) {
        if (address == null || address.getLatitude() == null || address.getLongitude() == null) {
            return null;
        }
        return haversineKm(FARM_LATITUDE, FARM_LONGITUDE, address.getLatitude(), address.getLongitude());
    }

    /** Delivery charge (₹) for shipping to this address. */
    public BigDecimal calculateCharge(Address address) {
        Double distanceKm = distanceKmTo(address);
        if (distanceKm == null) {
            return FALLBACK_FLAT_CHARGE;
        }
        // Round distance up to the next whole km before pricing.
        BigDecimal billableKm = BigDecimal.valueOf(Math.ceil(distanceKm));
        BigDecimal charge = billableKm.multiply(RATE_PER_KM);
        return charge.max(MIN_CHARGE).setScale(0, RoundingMode.HALF_UP);
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
