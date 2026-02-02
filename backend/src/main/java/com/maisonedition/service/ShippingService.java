package com.maisonedition.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class ShippingService {

    // Shipping costs by country code (ISO 3166-1 alpha-2)
    private static final Map<String, BigDecimal> SHIPPING_COSTS = Map.ofEntries(
        // Middle East
        Map.entry("SA", new BigDecimal("15.00")),  // Saudi Arabia - local
        Map.entry("AE", new BigDecimal("20.00")),  // UAE
        Map.entry("KW", new BigDecimal("20.00")),  // Kuwait
        Map.entry("BH", new BigDecimal("20.00")),  // Bahrain
        Map.entry("QA", new BigDecimal("20.00")),  // Qatar
        Map.entry("OM", new BigDecimal("20.00")),  // Oman
        Map.entry("JO", new BigDecimal("25.00")),  // Jordan
        Map.entry("LB", new BigDecimal("25.00")),  // Lebanon
        Map.entry("EG", new BigDecimal("25.00")),  // Egypt
        Map.entry("IQ", new BigDecimal("30.00")),  // Iraq
        Map.entry("YE", new BigDecimal("30.00")),  // Yemen
        Map.entry("SY", new BigDecimal("30.00")),  // Syria
        Map.entry("PS", new BigDecimal("30.00")),  // Palestine

        // North Africa
        Map.entry("MA", new BigDecimal("30.00")),  // Morocco
        Map.entry("DZ", new BigDecimal("30.00")),  // Algeria
        Map.entry("TN", new BigDecimal("30.00")),  // Tunisia
        Map.entry("LY", new BigDecimal("30.00")),  // Libya
        Map.entry("SD", new BigDecimal("30.00")),  // Sudan

        // Europe
        Map.entry("FR", new BigDecimal("35.00")),  // France
        Map.entry("DE", new BigDecimal("35.00")),  // Germany
        Map.entry("GB", new BigDecimal("35.00")),  // UK
        Map.entry("ES", new BigDecimal("35.00")),  // Spain
        Map.entry("IT", new BigDecimal("35.00")),  // Italy
        Map.entry("NL", new BigDecimal("35.00")),  // Netherlands
        Map.entry("BE", new BigDecimal("35.00")),  // Belgium
        Map.entry("CH", new BigDecimal("40.00")),  // Switzerland
        Map.entry("AT", new BigDecimal("35.00")),  // Austria
        Map.entry("SE", new BigDecimal("40.00")),  // Sweden
        Map.entry("TR", new BigDecimal("30.00")),  // Turkey

        // Americas
        Map.entry("US", new BigDecimal("45.00")),  // USA
        Map.entry("CA", new BigDecimal("45.00")),  // Canada

        // Asia
        Map.entry("PK", new BigDecimal("35.00")),  // Pakistan
        Map.entry("IN", new BigDecimal("35.00")),  // India
        Map.entry("MY", new BigDecimal("40.00")),  // Malaysia
        Map.entry("ID", new BigDecimal("40.00"))   // Indonesia
    );

    // Default shipping cost for countries not in the list
    private static final BigDecimal DEFAULT_SHIPPING_COST = new BigDecimal("50.00");

    // Free shipping threshold
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("200.00");

    /**
     * Calculate shipping cost based on country code
     * @param countryCode ISO 3166-1 alpha-2 country code
     * @param orderTotal Total order amount before shipping
     * @return Shipping cost (0 if free shipping applies)
     */
    public BigDecimal calculateShippingCost(String countryCode, BigDecimal orderTotal) {
        // Free shipping for orders above threshold
        if (orderTotal != null && orderTotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0) {
            return BigDecimal.ZERO;
        }

        if (countryCode == null || countryCode.isEmpty()) {
            return DEFAULT_SHIPPING_COST;
        }

        return SHIPPING_COSTS.getOrDefault(countryCode.toUpperCase(), DEFAULT_SHIPPING_COST);
    }

    /**
     * Get shipping cost for a specific country (without considering order total)
     */
    public BigDecimal getShippingCostForCountry(String countryCode) {
        if (countryCode == null || countryCode.isEmpty()) {
            return DEFAULT_SHIPPING_COST;
        }
        return SHIPPING_COSTS.getOrDefault(countryCode.toUpperCase(), DEFAULT_SHIPPING_COST);
    }

    /**
     * Get the free shipping threshold
     */
    public BigDecimal getFreeShippingThreshold() {
        return FREE_SHIPPING_THRESHOLD;
    }

    /**
     * Check if an order qualifies for free shipping
     */
    public boolean qualifiesForFreeShipping(BigDecimal orderTotal) {
        return orderTotal != null && orderTotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0;
    }
}
