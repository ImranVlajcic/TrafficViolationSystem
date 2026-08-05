package com.academy.trafficviolationsystem.fine;

import java.math.BigDecimal;

public interface FineAmountComponent {
    BigDecimal getBaseAmount();
    BigDecimal getDiscountAmount();
    BigDecimal getSurchargeAmount();
    BigDecimal getTotalDue();
}