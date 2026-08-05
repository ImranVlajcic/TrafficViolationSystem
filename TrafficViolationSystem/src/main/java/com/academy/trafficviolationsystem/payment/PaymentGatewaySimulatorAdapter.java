package com.academy.trafficviolationsystem.payment;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PaymentGatewaySimulatorAdapter implements PaymentGatewayAdapter {

    private final PaymentGatewaySimulator simulator;

    public PaymentGatewaySimulatorAdapter(PaymentGatewaySimulator simulator) {
        this.simulator = simulator;
    }

    @Override
    public SimulationResult processPayment(PaymentRequest request,
                                           BigDecimal amount) {
        return simulator.simulate(request, amount);
    }
}
