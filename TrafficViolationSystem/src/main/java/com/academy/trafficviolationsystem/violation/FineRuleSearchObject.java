package com.academy.trafficviolationsystem.violation;

import com.academy.trafficviolationsystem.core.model.BaseSearchObject;
import lombok.Getter;
import lombok.Setter;

/**
 * Search/filter parameters for GET /api/fine-rules.
 */
@Getter
@Setter
public class FineRuleSearchObject extends BaseSearchObject<Integer> {

    private ViolationType violationType;

    /** null = all, true = only active rules, false = only inactive. */
    private Boolean isActive;
}
