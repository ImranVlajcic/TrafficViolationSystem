package com.academy.trafficviolationsystem.fine;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class LateSurchargeDecorator extends FineAmountDecorator {

    private final BigDecimal surchargePct;

    public LateSurchargeDecorator(FineAmountComponent component, BigDecimal surchargePct) {
        super(component);
        this.surchargePct = surchargePct;
    }

    @Override
    public BigDecimal getSurchargeAmount() {
        return component.getBaseAmount()
                .multiply(surchargePct)
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal getTotalDue() {
        return component.getBaseAmount()
                .subtract(component.getDiscountAmount())
                .add(getSurchargeAmount())
                .setScale(2, RoundingMode.HALF_UP);
    }
}