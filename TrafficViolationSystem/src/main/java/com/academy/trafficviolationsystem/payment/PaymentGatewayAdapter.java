package com.academy.trafficviolationsystem.payment;

import java.math.BigDecimal;

public interface PaymentGatewayAdapter {

    SimulationResult processPayment(PaymentRequest request, BigDecimal amount);

}