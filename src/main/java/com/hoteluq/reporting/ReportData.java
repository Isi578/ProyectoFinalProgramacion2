package com.hoteluq.reporting;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class ReportData {
    private final String title;
    private final LocalDateTime generatedAt;
    private final LocalDateTime from;
    private final LocalDateTime to;
    private final List<String> rows;
    private final BigDecimal total;

    public ReportData(String title, LocalDateTime generatedAt,
                      LocalDateTime from, LocalDateTime to,
                      List<String> rows, BigDecimal total) {
        this.title = title;
        this.generatedAt = generatedAt;
        this.from = from;
        this.to = to;
        this.rows = List.copyOf(rows);
        this.total = total;
    }

    public String getTitle() { return title; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public LocalDateTime getFrom() { return from; }
    public LocalDateTime getTo() { return to; }
    public List<String> getRows() { return rows; }
    public BigDecimal getTotal() { return total; }
}