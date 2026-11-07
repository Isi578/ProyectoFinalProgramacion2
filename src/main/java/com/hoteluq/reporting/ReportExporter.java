package com.hoteluq.reporting;

import java.io.IOException;
import java.io.OutputStream;

public interface ReportExporter {
    ReportFormat format();
    void export(ReportData report, OutputStream outputStream) throws IOException;
}