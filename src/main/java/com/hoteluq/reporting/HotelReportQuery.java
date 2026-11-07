package com.hoteluq.reporting;

import java.time.LocalDateTime;

public interface HotelReportQuery {
    ReportData generate(ReportType type, LocalDateTime from, LocalDateTime to);
}