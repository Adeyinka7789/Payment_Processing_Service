package com.example.pps.gateway;

import com.example.pps.entity.Transaction;
import org.springframework.stereotype.Component;

@Component
public class GatewayFactory {
    private final PaystackGateway paystackGateway;
    private final FlutterwaveGateway flutterwaveGateway;

    public GatewayFactory(PaystackGateway paystackGateway, FlutterwaveGateway flutterwaveGateway) {
        this.paystackGateway = paystackGateway;
        this.flutterwaveGateway = flutterwaveGateway;
    }

    public GatewayProvider getGateway(Transaction.PaymentGateway gateway) {
        return switch (gateway) {
            case PAYSTACK -> paystackGateway;
            case FLUTTERWAVE -> flutterwaveGateway;
        };
    }
}