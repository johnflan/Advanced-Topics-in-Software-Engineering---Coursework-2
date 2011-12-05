package com.acmetelecom;

import java.math.BigDecimal;

public class LineItem {
    private Call call;
    private BigDecimal callCost;

    public LineItem(Call call, BigDecimal callCost) {
        this.call = call;
        this.callCost = callCost;
    }

    public String date() {
        return call.date();
    }

    public String callee() {
        return call.callee();
    }

    public String durationMinutes() {
        return "" + call.durationSeconds() / 60 + ":" + String.format("%02d", call.durationSeconds() % 60);
    }

    public BigDecimal cost() {
        return callCost;
    }
}
