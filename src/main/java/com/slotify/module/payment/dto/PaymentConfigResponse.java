package com.slotify.module.payment.dto;

/**
 * Public payment configuration for the apps.
 *
 * @param stripeEnabled whether Stripe is configured on the server
 * @param stripePublishableKey key for the Stripe PaymentSheet
 * @param paypalEnabled whether PayPal is configured
 * @param paypalClientId client id for the PayPal button/web view
 * @param paypalMode sandbox or live
 */
public record PaymentConfigResponse(
    boolean stripeEnabled,
    String stripePublishableKey,
    boolean paypalEnabled,
    String paypalClientId,
    String paypalMode) {}
