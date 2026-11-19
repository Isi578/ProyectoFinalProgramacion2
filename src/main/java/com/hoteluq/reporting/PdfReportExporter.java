package com.hoteluq.reporting;

import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.HeaderFooter;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.time.format.DateTimeFormatter;

/**
 * PdfReportExporter (exportador de informes PDF).
 *
 * Se implementa patrón Strategy.
 *
 * Permite exportar los informes administrativos
 * del hotel en formato PDF.
 *
 * Utiliza la biblioteca OpenPDF para crear
 * el documento y organizar su contenido.
 *
 * La biblioteca se encarga automáticamente
 * de las páginas, los textos y las fuentes.
 *
 * Así se evita líos de gestión de texto.
 *
 * Esta clase solamente presenta los datos.
 * No calcula estadísticas, pagos ni reservas.
 */
public final class PdfReportExporter implements ReportExporter {

    // Formato utilizado para mostrar las fechas del informe.
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * format (formato).
     *
     * Indica el formato de archivo
     * que genera esta implementación.
     *
     * Forma parte del patrón Strategy.
     */
    @Override
    public ReportFormat format() {

        return ReportFormat.PDF;
    }

    /**
     * export (exportar).
     *
     * Recibe los datos de un informe
     * y genera el documento PDF.
     *
     * report: contiene los resultados del informe.
     *
     * outputStream: indica dónde se guardará
     * el documento generado.
     *
     * No es necesario modificar el controlador,
     * porque conservamos el contrato de ReportExporter.
     */
    @Override
    public void export(
            ReportData report,
            OutputStream outputStream) throws IOException {

        /*
         * Document (documento).
         *
         * Representa el archivo PDF que vamos a crear.
         *
         * Utilizamos una hoja tamaño A4
         * y establecemos sus márgenes.
         */
        Document document = new Document(
                PageSize.A4,
                42, 42, 65, 50
        );

        /*
         * Font (fuente).
         *
         * Definimos tres estilos de texto:
         *
         * normal: información general.
         * title: título principal del informe.
         * bold: encabezados y valores importantes.
         */
        Font normal = new Font(
                Font.HELVETICA,
                10,
                Font.NORMAL
        );

        Font title = new Font(
                Font.HELVETICA,
                14,
                Font.BOLD
        );

        Font bold = new Font(
                Font.HELVETICA,
                10,
                Font.BOLD
        );

        /*
         * HeaderFooter (encabezado y pie).
         *
         * Generamos el encabezado institucional
         * que aparecerá en las páginas del PDF.
         */
        HeaderFooter header = new HeaderFooter(
                new Phrase(
                        "HOTELUQ | INFORME ADMINISTRATIVO",
                        bold
                ),
                false
        );

        // Centramos el encabezado.
        header.setAlignment(Element.ALIGN_CENTER);

        /*
         * Creamos un pie de página.
         *
         * El valor true indica que OpenPDF
         * agregará automáticamente el número de página.
         */
        HeaderFooter footer = new HeaderFooter(
                new Phrase(
                        "HotelUQ | Página ",
                        normal
                ),
                true
        );

        // Alineamos el pie de página a la izquierda.
        footer.setAlignment(Element.ALIGN_LEFT);

        try {

            /*
             * PdfWriter (escritor PDF).
             *
             * Conecta el documento con el archivo
             * seleccionado desde la interfaz gráfica.
             */
            PdfWriter writer = PdfWriter.getInstance(
                    document,
                    outputStream
            );

            /*
             * El controlador administrativo se encarga
             * de cerrar el archivo cuando termina.
             *
             * Por eso evitamos que OpenPDF cierre
             * directamente el OutputStream recibido.
             */
            writer.setCloseStream(false);

            /*
             * Configuramos el encabezado y el pie
             * antes de abrir el documento.
             *
             * Así también aparecerán en la primera página.
             */
            document.setHeader(header);
            document.setFooter(footer);

            /*
             * open (abrir).
             *
             * Prepara el documento para comenzar
             * a escribir su contenido.
             */
            document.open();

            /*
             * Paragraph (párrafo).
             *
             * Creamos el título principal del informe
             * utilizando los datos recibidos.
             */
            Paragraph heading = new Paragraph(
                    report.getTitle(),
                    title
            );

            // Dejamos un espacio después del título.
            heading.setSpacingAfter(12);

            // Agregamos el título al documento.
            document.add(heading);

            /*
             * Agregamos las fechas del informe.
             *
             * Estos valores ya fueron establecidos
             * por el servicio de reportes.
             */
            document.add(new Paragraph(
                    "Generado: "
                            + DATE.format(report.getGeneratedAt()),
                    normal
            ));

            document.add(new Paragraph(
                    "Periodo: "
                            + DATE.format(report.getFrom())
                            + " al "
                            + DATE.format(report.getTo()),
                    normal
            ));

            /*
             * Mostramos la cantidad real de registros.
             *
             * Este valor se conserva incluso cuando
             * el informe utiliza el patrón Decorator.
             */
            document.add(new Paragraph(
                    "Registros (filas): "
                            + report.getRecordCount(),
                    normal
            ));

            // Separamos el encabezado de los resultados.
            document.add(new Paragraph(
                    "------------------------------------------------------------",
                    normal
            ));

            /*
             * Recorremos los registros del informe.
             *
             * Utilizamos un ciclo for, igual que
             * en nuestro exportador de texto.
             *
             * OpenPDF organiza los párrafos
             * y genera nuevas páginas cuando se necesitan.
             */
            for (String row : report.getRows()) {

                // Creamos un párrafo para cada registro.
                Paragraph line = new Paragraph(
                        "  - " + row,
                        normal
                );

                // Dejamos un pequeño espacio entre registros.
                line.setSpacingAfter(3);

                // Agregamos el registro al documento.
                document.add(line);
            }

            // Separamos los registros del total.
            document.add(new Paragraph(
                    "------------------------------------------------------------",
                    normal
            ));

            /*
             * Agregamos el total del informe.
             *
             * El valor ya está calculado
             * por HotelReportService.
             *
             * Esta clase solamente lo muestra.
             */
            document.add(new Paragraph(
                    "TOTAL: "
                            + report.getTotal().toPlainString()
                            + " "
                            + report.getUnit(),
                    bold
            ));

        } catch (DocumentException error) {

            /*
             * DocumentException (error del documento).
             *
             * Si OpenPDF presenta un problema,
             * lo convertimos en IOException.
             *
             * Este es el tipo de excepción
             * establecido por ReportExporter.
             */
            throw new IOException(
                    "No se pudo generar el informe PDF.",
                    error
            );

        } finally {

            /*
             * close (cerrar).
             *
             * Finalizamos el documento PDF.
             *
             * OpenPDF completa automáticamente
             * las páginas y la estructura interna.
             */
            document.close();
        }

        /*
         * flush (vaciar).
         *
         * Terminamos de enviar los datos
         * al destino seleccionado por el administrador.
         */
        outputStream.flush();
    }
}