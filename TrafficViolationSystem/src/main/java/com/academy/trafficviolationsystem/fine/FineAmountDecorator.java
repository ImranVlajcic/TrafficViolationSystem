package com.academy.trafficviolationsystem.fine;

import java.math.BigDecimal;

public abstract class FineAmountDecorator implements FineAmountComponent {

    protected final FineAmountComponent component;

    protected FineAmountDecorator(FineAmountComponent component) {
        this.component = component;
    }

    @Override public BigDecimal getBaseAmount()      { return component.getBaseAmount(); }
    @Override public BigDecimal getDiscountAmount()  { return component.getDiscountAmount(); }
    @Override public BigDecimal getSurchargeAmount() { return component.getSurchargeAmount(); }
}