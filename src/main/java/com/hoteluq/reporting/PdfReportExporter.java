package com.hoteluq.reporting;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * PdfReportExporter (exportador de informes PDF).
 *
 * Implementación del patrón Strategy.
 *
 * Escribe una estructura básica del PDF.
 *
 * Esta clase concentra la parte técnica del PDF
 * para mantener sencillas las demás clases.
 */
public final class PdfReportExporter implements ReportExporter {

    // Codificación de los caracteres del documento.
    private static final Charset CP1252 =
            Charset.forName("windows-1252");

    // Formato visible de las fechas.
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * format (formato).
     *
     * Indica que esta implementación genera PDF.
     */
    @Override
    public ReportFormat format() {
        return ReportFormat.PDF;
    }

    /**
     * export (exportar).
     *
     * Organiza las líneas, crea las páginas
     * y escribe el documento PDF.
     */
    @Override
    public void export(
            ReportData report,
            OutputStream outputStream) throws IOException {

        // Guardamos las líneas que aparecerán en el PDF.
        List<String> lines = new ArrayList<>();

        lines.add(
                "Generado: " + DATE.format(report.getGeneratedAt())
        );

        lines.add(
                "Periodo: "
                        + DATE.format(report.getFrom())
                        + " al "
                        + DATE.format(report.getTo())
        );

        lines.add(
                "Registros (filas): " + report.getRecordCount()
        );

        lines.add(
                "--------------------------------------------------------------------------"
        );

        // Agregamos los registros del informe.
        for (String row : report.getRows()) {

            // wrap (ajustar línea): divide textos demasiado largos.
            wrap(lines, "  - " + row, 93);
        }

        lines.add(
                "--------------------------------------------------------------------------"
        );

        lines.add(
                "TOTAL: "
                        + report.getTotal().toPlainString()
                        + " "
                        + report.getUnit()
        );

        // contents (contenidos): almacena las páginas del PDF.
        List<byte[]> contents = new ArrayList<>();

        // offset (posición): indica la próxima línea por escribir.
        int offset = 0;

        // page (página): contador de páginas.
        int page = 1;

        // Generamos páginas hasta escribir todas las líneas.
        do {

            StringBuilder content = new StringBuilder();

            // Encabezado institucional.
            writeLine(
                    content,
                    "HOTELUQ  |  INFORME ADMINISTRATIVO",
                    42, 750, 15, true
            );

            // Título del informe.
            writeLine(
                    content,
                    report.getTitle(),
                    42, 720, 13, true
            );

            // Línea horizontal de separación.
            content.append(
                    "0.75 0.78 0.80 RG 42 704 m 570 704 l S 0 0 0 RG\n"
            );

            // Posición vertical inicial.
            int y = 678;

            // Escribimos las líneas que caben en la página.
            while (offset < lines.size() && y > 57) {

                writeLine(
                        content,
                        lines.get(offset++),
                        42, y, 10, false
                );

                // Bajamos para escribir la siguiente línea.
                y -= 17;
            }

            // Numeración de páginas.
            writeLine(
                    content,
                    "HotelUQ | Pagina " + page++,
                    42, 35, 9, false
            );

            // Guardamos los caracteres correspondientes.
            contents.add(
                    content.toString().getBytes(CP1252)
            );

        } while (offset < lines.size());

        // Cantidad de páginas generadas.
        int pageCount = contents.size();

        // Identificadores internos de las fuentes PDF.
        int fontId = 3 + pageCount * 2;
        int boldFontId = fontId + 1;
        int objectCount = boldFontId;

        /**
         * ByteArrayOutputStream:
         * almacena temporalmente el documento en memoria.
         *
         * Es necesario porque PDF utiliza referencias
         * a las posiciones de sus objetos.
         */
        ByteArrayOutputStream pdf = new ByteArrayOutputStream();

        // Encabezado obligatorio del formato PDF.
        ascii(pdf, "%PDF-1.4\n");

        // Posiciones internas de los objetos.
        List<Integer> byteOffsets = new ArrayList<>();

        byteOffsets.add(0);

        // Creamos el objeto principal del documento.
        putObject(
                pdf,
                byteOffsets,
                1,
                "<< /Type /Catalog /Pages 2 0 R >>"
        );

        // Guardamos las referencias de todas las páginas.
        StringBuilder kids = new StringBuilder();

        for (int i = 0; i < pageCount; i++) {

            kids.append(3 + i * 2)
                    .append(" 0 R ");
        }

        // Creamos el conjunto de páginas.
        putObject(
                pdf,
                byteOffsets,
                2,
                "<< /Type /Pages /Count "
                        + pageCount
                        + " /Kids [ "
                        + kids
                        + "] >>"
        );

        // Escribimos cada página del documento.
        for (int i = 0; i < pageCount; i++) {

            int pageId = 3 + i * 2;
            int streamId = pageId + 1;

            // Información de tamaño, fuentes y contenido.
            putObject(
                    pdf,
                    byteOffsets,
                    pageId,
                    "<< /Type /Page /Parent 2 0 R "
                            + "/MediaBox [0 0 612 792] "
                            + "/Resources << /Font << /F1 "
                            + fontId
                            + " 0 R /F2 "
                            + boldFontId
                            + " 0 R >> >> /Contents "
                            + streamId
                            + " 0 R >>"
            );

            byte[] data = contents.get(i);

            // Guardamos la posición del contenido.
            byteOffsets.add(pdf.size());

            ascii(
                    pdf,
                    streamId
                            + " 0 obj\n<< /Length "
                            + data.length
                            + " >>\nstream\n"
            );

            // Escribimos el texto de la página.
            pdf.write(data);

            ascii(pdf, "\nendstream\nendobj\n");
        }

        // Fuente normal.
        putObject(
                pdf,
                byteOffsets,
                fontId,
                "<< /Type /Font /Subtype /Type1 "
                        + "/BaseFont /Helvetica "
                        + "/Encoding /WinAnsiEncoding >>"
        );

        // Fuente en negrita.
        putObject(
                pdf,
                byteOffsets,
                boldFontId,
                "<< /Type /Font /Subtype /Type1 "
                        + "/BaseFont /Helvetica-Bold "
                        + "/Encoding /WinAnsiEncoding >>"
        );

        // Posición donde comienza la tabla de referencias.
        int xref = pdf.size();

        ascii(
                pdf,
                "xref\n0 "
                        + (objectCount + 1)
                        + "\n0000000000 65535 f \n"
        );

        // Registramos la posición de cada objeto.
        for (int id = 1; id <= objectCount; id++) {

            ascii(
                    pdf,
                    String.format(
                            java.util.Locale.ROOT,
                            "%010d 00000 n \n",
                            byteOffsets.get(id)
                    )
            );
        }

        // Información final del archivo PDF.
        ascii(
                pdf,
                "trailer\n<< /Size "
                        + (objectCount + 1)
                        + " /Root 1 0 R >>\n"
        );

        ascii(
                pdf,
                "startxref\n"
                        + xref
                        + "\n%%EOF\n"
        );

        // Guardamos el documento en el destino seleccionado.
        pdf.writeTo(outputStream);

        outputStream.flush();
    }

    /**
     * writeLine (escribir línea).
     *
     * Coloca una línea de texto en una posición del PDF.
     *
     * x: posición horizontal.
     * y: posición vertical.
     * size: tamaño de la fuente.
     * bold: indica si se escribe en negrita.
     */
    private static void writeLine(
            StringBuilder content,
            String text,
            int x,
            int y,
            int size,
            boolean bold) {

        // Elegimos el color de la letra.
        content.append(
                bold ? "0.08 0.34 0.27 rg\n" : "0 0 0 rg\n"
        );

        // Agregamos el comando de escritura PDF.
        content.append("BT /")
                .append(bold ? "F2" : "F1")
                .append(' ')
                .append(size)
                .append(" Tf ")
                .append(x)
                .append(' ')
                .append(y)
                .append(" Td (")
                .append(escape(text))
                .append(") Tj ET\n");
    }

    /**
     * escape (escapar texto).
     *
     * Prepara los caracteres especiales para que
     * no interfieran con la estructura del PDF.
     */
    private static String escape(String value) {

        StringBuilder escaped = new StringBuilder();

        // Recorremos los caracteres del texto.
        for (int i = 0; i < value.length(); i++) {

            char c = value.charAt(i);

            // Protegemos paréntesis y barras.
            if (c == '(' || c == ')' || c == '\\') {

                escaped.append('\\');
            }

            // Reemplazamos caracteres no admitidos.
            if (c < 32 ||
                    (c > 126 && !CP1252.newEncoder().canEncode(c))) {

                c = '?';
            }

            escaped.append(c);
        }

        return escaped.toString();
    }

    /**
     * wrap (ajustar línea).
     *
     * Divide los textos largos para evitar
     * que salgan del ancho de la página.
     */
    private static void wrap(
            List<String> target,
            String line,
            int max) {

        String text = line;

        while (text.length() > max) {

            // Buscamos el espacio más próximo al límite.
            int boundary = text.lastIndexOf(' ', max);

            if (boundary < 15) {

                boundary = max;
            }

            target.add(text.substring(0, boundary));

            text = "    " + text.substring(boundary).trim();
        }

        target.add(text);
    }

    /**
     * putObject (escribir objeto).
     *
     * Guarda un objeto interno del PDF
     * y registra la posición donde comienza.
     */
    private static void putObject(
            ByteArrayOutputStream output,
            List<Integer> offsets,
            int id,
            String value) throws IOException {

        offsets.add(output.size());

        ascii(
                output,
                id + " 0 obj\n" + value + "\nendobj\n"
        );
    }

    /**
     * ascii (escribir caracteres básicos).
     *
     * Convierte los comandos PDF en bytes.
     */
    private static void ascii(
            ByteArrayOutputStream output,
            String text) throws IOException {

        output.write(
                text.getBytes(StandardCharsets.ISO_8859_1)
        );
    }
}