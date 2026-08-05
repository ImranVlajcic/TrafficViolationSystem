package com.academy.trafficviolationsystem.fine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class EarlyPaymentDiscountDecorator extends FineAmountDecorator {

    private final BigDecimal discountPct;
    private final boolean withinWindow;

    public EarlyPaymentDiscountDecorator(FineAmountComponent component,
                                         BigDecimal discountPct,
                                         LocalDate issuedDate,
                                         int windowDays) {
        super(component);
        this.discountPct  = discountPct;
        this.withinWindow = discountPct != null
                && discountPct.compareTo(BigDecimal.ZERO) != 0
                && !LocalDate.now().isAfter(issuedDate.plusDays(windowDays));
    }

    @Override
    public BigDecimal getDiscountAmount() {
        if (!withinWindow) {
            return component.getDiscountAmount();
        }
        return component.getBaseAmount()
                .multiply(discountPct)
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal getTotalDue() {
        return component.getBaseAmount()
                .subtract(getDiscountAmount())
                .add(component.getSurchargeAmount())
                .setScale(2, RoundingMode.HALF_UP);
    }
}