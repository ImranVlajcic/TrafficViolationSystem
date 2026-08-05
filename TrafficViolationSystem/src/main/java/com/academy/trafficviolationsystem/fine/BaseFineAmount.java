package com.academy.trafficviolationsystem.fine;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class BaseFineAmount implements FineAmountComponent {

    private final BigDecimal baseAmount;
    private final BigDecimal discountAmount;
    private final BigDecimal surchargeAmount;

    public BaseFineAmount(BigDecimal baseAmount) {
        this(baseAmount, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public BaseFineAmount(BigDecimal baseAmount, BigDecimal discountAmount, BigDecimal surchargeAmount) {
        this.baseAmount      = baseAmount;
        this.discountAmount  = discountAmount == null ? BigDecimal.ZERO : discountAmount;
        this.surchargeAmount = surchargeAmount == null ? BigDecimal.ZERO : surchargeAmount;
    }

    @Override public BigDecimal getBaseAmount()      { return baseAmount; }
    @Override public BigDecimal getDiscountAmount()  { return discountAmount; }
    @Override public BigDecimal getSurchargeAmount() { return surchargeAmount; }
    @Override public BigDecimal getTotalDue() {
        return baseAmount.subtract(discountAmount).add(surchargeAmount).setScale(2, RoundingMode.HALF_UP);
    }
}