package com.hospital.harmonia.util;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.HtmlExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleHtmlExporterOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

/**
 * Wraps report generation with JasperReports.
 *
 * Flow: compiles the .jrxml (or uses an already-compiled .jasper), fills it
 * with the data (a collection of Java objects, via JRBeanCollectionDataSource)
 * and exports it to PDF.
 *
 * Performance tip: in production, compile the .jrxml files once (during the
 * build, generating .jasper files) instead of compiling on every report generation.
 */
public final class ReportGenerator {

    private static final Logger log = LoggerFactory.getLogger(ReportGenerator.class);

    private ReportGenerator() {
    }

    public static <T> File generatePdf(String jrxmlResourcePath, Map<String, Object> parameters,
                                        Collection<T> data, File outputFile) throws Exception {
        log.info("Generating PDF report from {} ({} record(s)) -> {}",
                jrxmlResourcePath, data.size(), outputFile.getAbsolutePath());
        try (InputStream jrxmlStream = ReportGenerator.class.getResourceAsStream(jrxmlResourcePath)) {
            if (jrxmlStream == null) {
                throw new IllegalArgumentException("Relatório não encontrado no classpath: " + jrxmlResourcePath);
            }
            JasperReport jasperReport = JasperCompileManager.compileReport(jrxmlStream);
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(data);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
            JasperExportManager.exportReportToPdfFile(jasperPrint, outputFile.getAbsolutePath());
            log.info("PDF report generated successfully: {}", outputFile.getAbsolutePath());
            return outputFile;
        } catch (Exception e) {
            log.error("Failed to generate PDF report from {}", jrxmlResourcePath, e);
            throw e;
        }
    }

    public static <T> void generateHtml(String jrxmlResourcePath, Map<String, Object> parameters,
                                         Collection<T> data, File outputFile) throws Exception {
        log.info("Generating HTML report from {} ({} record(s)) -> {}",
                jrxmlResourcePath, data.size(), outputFile.getAbsolutePath());
        try (InputStream jrxmlStream = ReportGenerator.class.getResourceAsStream(jrxmlResourcePath)) {
            JasperReport jasperReport = JasperCompileManager.compileReport(jrxmlStream);
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(data);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);

            HtmlExporter exporter = new HtmlExporter();
            exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
            exporter.setExporterOutput(new SimpleHtmlExporterOutput(outputFile));
            exporter.exportReport();
            log.info("HTML report generated successfully: {}", outputFile.getAbsolutePath());
        } catch (Exception e) {
            log.error("Failed to generate HTML report from {}", jrxmlResourcePath, e);
            throw e;
        }
    }
}
