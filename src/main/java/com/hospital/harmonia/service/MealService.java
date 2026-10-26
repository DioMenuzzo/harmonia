package com.hospital.harmonia.service;

import com.hospital.harmonia.dao.MealDao;
import com.hospital.harmonia.dao.impl.MealDaoImpl;
import com.hospital.harmonia.model.Employee;
import com.hospital.harmonia.model.Meal;
import com.hospital.harmonia.util.ReportGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hospital.harmonia.model.MealType;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.time.format.DateTimeFormatter;

public class MealService {

    private static final Logger log = LoggerFactory.getLogger(MealService.class);

    // Same dia/mes/ano format used on screen -- without this, the PDF header
    // showed the "raw" date (LocalDate.toString(), ISO yyyy-MM-dd format).
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final MealDao mealDao;

    public MealService() {
        this(new MealDaoImpl());
    }

    /** Package-visible constructor allowing a test double to be injected (see MealServiceTest). */
    MealService(MealDao mealDao) {
        this.mealDao = mealDao;
    }

    public Meal register(Meal meal) {
        validate(meal);
        checkNoDuplicateMealType(meal);
        log.debug("Registering meal: employeeId={} date={} time={}", meal.getEmployeeId(), meal.getDate(), meal.getTime());
        return mealDao.save(meal);
    }

    public void update(Meal meal) {
        validate(meal);
        checkNoDuplicateMealType(meal);
        mealDao.update(meal);
    }

    public void remove(int id) {
        mealDao.remove(id);
    }

    public void deactivate(int id) {
        mealDao.deactivate(id);
    }

    public void activate(int id) {
        mealDao.activate(id);
    }

    /** Active and inactive -- for the screen's table/audit log. */
    public List<Meal> findByPeriod(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Data inicial não pode ser depois da data final.");
        }
        return mealDao.findByPeriod(start, end);
    }

    /** Only the active ones -- for the PDF report and the total spent in the period. */
    public List<Meal> findActiveByPeriod(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Data inicial não pode ser depois da data final.");
        }
        return mealDao.findActiveByPeriod(start, end);
    }

    /**
     * Only the active ones, additionally filtered by the given ReportFilters
     * (category/type can each be a set of values, plain or inverted/"except",
     * plus a single optional employee) -- used by the report formats (PDF
     * and XLSX). The filtering itself happens in Java, on top of
     * findActiveByPeriod(start, end), instead of adding more WHERE clauses
     * to the SQL: the cafeteria's data volume is small, and keeping the
     * filtering here (rather than spread across DAO query variants) keeps it
     * in one place, easy to extend later.
     */
    public List<Meal> findActiveByPeriod(LocalDate start, LocalDate end, ReportFilters filters) {
        List<Meal> meals = findActiveByPeriod(start, end);
        return meals.stream()
                .filter(filters::matches)
                .collect(Collectors.toList());
    }

    /** Total spent in the period -- used in the report header. */
    public BigDecimal totalForPeriod(List<Meal> meals) {
        return meals.stream()
                .map(Meal::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Generates the PDF report for the given period, using JasperReports.
     * The .jrxml file lives at src/main/resources/reports/refeitorio_report.jrxml.
     * filters is optional (ReportFilters.none() applies no restriction at
     * all) and is applied on top of the date range -- see ReportFilters for
     * how category/type each support multiple values and an "except" mode.
     *
     * The report is organized in blocks: one block per employee category,
     * each with a sub-section per employee listing their meals (date/time),
     * and a total (plus a per-type breakdown) for the category -- driven by
     * two nested groups in the .jrxml (category, then employee). Nested
     * JasperReports groups require the underlying data pre-sorted the same
     * way they're nested, hence reportOrder() below before handing the list
     * to JasperReports.
     */
    public File generatePeriodReport(LocalDate start, LocalDate end, ReportFilters filters,
                                      File outputFile) throws Exception {
        // Only active meals go into the report/total -- a deactivated
        // (canceled) meal stays visible on screen as a log, but shouldn't be billed.
        List<Meal> meals = findActiveByPeriod(start, end, filters);

        List<Meal> sortedMeals = meals.stream()
                .sorted(reportOrder())
                .collect(Collectors.toList());

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("startDate", start.format(DATE_FORMAT));
        parameters.put("endDate", end.format(DATE_FORMAT));
        parameters.put("total", totalForPeriod(meals));
        parameters.put("quantity", meals.size());
        parameters.put("typeBreakdown", formatTypeBreakdown(meals));
        parameters.put("categoryBreakdown", formatCategoryBreakdown(meals));
        parameters.put("filters", describeFilters(filters));

        return ReportGenerator.generatePdf("/reports/refeitorio_report.jrxml", parameters, sortedMeals, outputFile);
    }

    /**
     * Canonical key that decides which category block a meal lands in on the
     * PDF report. Categories are free text typed by hand on the employee
     * record, so the same category shows up as "Alunos" on one record and
     * "ALUNOS" on another -- they mean the same group of people and must end
     * up in the same block.
     *
     * The groupExpression in refeitorio_report.jrxml applies this exact same
     * rule (blank -> "Sem categoria", then uppercase). The two MUST stay in
     * sync: JasperReports starts a new block whenever its expression changes,
     * so if the template grouped by the raw value while this sorted
     * case-insensitively, "Alunos" and "ALUNOS" would be interleaved and the
     * template would emit two blocks with the same title and split totals --
     * while the period summary at the end (formatCategoryBreakdown, which
     * merges case-insensitively) reported a single combined number. Same PDF,
     * two different answers.
     */
    private static String categoryGroupKey(String category) {
        return normalizeCategory(category).toUpperCase();
    }

    /**
     * The order the PDF report's data must be in, matching how the .jrxml
     * nests its groups: category block, then one sub-section per employee,
     * then that employee's meals chronologically. JasperReports only closes a
     * group when the group expression changes, so every record belonging to
     * the same block has to be adjacent here -- and these sort keys have to be
     * the SAME keys the template groups by, or a block gets split in two.
     *
     * Employees are ordered by name but separated by id: the database only
     * enforces a unique matrícula, not a unique name, so two different people
     * can legitimately both be "Maria Silva". Without the id in the sort (and
     * in the template's group expression) their meals would be merged into a
     * single block under a single total, making it impossible to tell who
     * consumed what -- on the document used to bill the outsourced company.
     *
     * Package-visible so MealServiceTest can verify the ordering without
     * having to run JasperReports.
     */
    static Comparator<Meal> reportOrder() {
        return Comparator
                .comparing((Meal m) -> categoryGroupKey(m.getEmployeeCategory()))
                .thenComparing(Meal::getEmployeeName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Meal::getEmployeeId, Comparator.nullsLast(Comparator.<Integer>naturalOrder()))
                .thenComparing(Meal::getDate)
                .thenComparing(Meal::getTime);
    }

    // Meal types shown in the calendar-grid Excel report (and in its summary),
    // in this display order. LANCHE is intentionally left out -- same as the
    // meal-price screen -- since it's no longer assigned automatically (see
    // MealType.byTime): if some old record still has it, it simply won't
    // appear in this report's grid or its totals.
    private static final MealType[] GRID_TYPES = {
            MealType.CAFE_DA_MANHA, MealType.ALMOCO, MealType.JANTAR, MealType.CEIA
    };

    // Number of date-blocks placed side by side before wrapping to a new
    // "row" of blocks -- matches the layout of the calendar-style spreadsheet
    // the cafeteria used to fill by hand (4 dates across, then down).
    private static final int GRID_COLUMNS = 4;

    // Columns used by a single date-block: one for the meal-type label, one
    // for its quantity.
    private static final int BLOCK_WIDTH = 2;

    // Rows used by a single date-block: date header, 4 meal types, a blank
    // row, the "TOTAL GERAL" row, and one trailing blank row that separates
    // it from the next block stacked below it in the same column.
    private static final int BLOCK_HEIGHT = 8;

    /**
     * Generates a calendar-style .xlsx report: one small table per day in
     * the period (date, quantity per meal type, and a "TOTAL GERAL" for that
     * day), arranged GRID_COLUMNS-across and wrapping downward -- the same
     * layout the cafeteria used to fill by hand in Excel -- with a summary
     * table (quantity and total R$ for the whole period, per meal type)
     * placed beside the grid, to its right, instead of below it. Days with
     * no meals still show up in the grid, with zeros. Uses Apache POI;
     * filters is the same ReportFilters as the PDF report (ReportFilters.none()
     * means "don't filter by anything").
     */
    public File generatePeriodReportXlsx(LocalDate start, LocalDate end, ReportFilters filters,
                                          File outputFile) throws IOException {
        List<Meal> meals = findActiveByPeriod(start, end, filters);

        // Every day in the period starts at zero, so a day with no meals
        // still shows up in the grid (instead of just being skipped).
        Map<LocalDate, Map<MealType, Long>> countsByDate = new TreeMap<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            countsByDate.put(d, new EnumMap<>(MealType.class));
        }
        for (Meal m : meals) {
            countsByDate.computeIfAbsent(m.getDate(), d -> new EnumMap<>(MealType.class))
                    .merge(m.getType(), 1L, Long::sum);
        }
        List<LocalDate> dates = new ArrayList<>(countsByDate.keySet());

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Relatório");
            XlsxStyles styles = new XlsxStyles(workbook);

            int rowIdx = 0;
            Row titleRow = sheet.createRow(rowIdx++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("RELATÓRIO DO REFEITÓRIO");
            titleCell.setCellStyle(styles.title);

            Row periodRow = sheet.createRow(rowIdx++);
            periodRow.createCell(0).setCellValue(
                    "Período: " + start.format(DATE_FORMAT) + " a " + end.format(DATE_FORMAT));

            String filtersText = describeFilters(filters);
            if (!filtersText.isEmpty()) {
                Row filtersRow = sheet.createRow(rowIdx++);
                filtersRow.createCell(0).setCellValue("Filtros: " + filtersText);
            }
            rowIdx++; // blank separator row before the grid

            int gridStartRow = rowIdx;
            int rowsPerColumn = Math.max(1, (int) Math.ceil(dates.size() / (double) GRID_COLUMNS));

            for (int i = 0; i < dates.size(); i++) {
                int blockColumn = i / rowsPerColumn;
                int rowWithinColumn = i % rowsPerColumn;
                int baseRow = gridStartRow + rowWithinColumn * BLOCK_HEIGHT;
                int baseCol = blockColumn * (BLOCK_WIDTH + 1); // +1 for the gap column between blocks

                LocalDate date = dates.get(i);
                writeDateBlock(sheet, styles, baseRow, baseCol, date, countsByDate.get(date));
            }

            int usedColumns = GRID_COLUMNS * (BLOCK_WIDTH + 1) - 1; // last block has no trailing gap
            int summaryColumn = usedColumns + 2; // one gap column to the right of the grid

            // Placed beside the grid (to the right, at the same row it starts)
            // instead of below it -- keeps the summary visible without having
            // to scroll past every date block first.
            writeSummary(sheet, styles, gridStartRow, summaryColumn, meals);

            for (int c = 0; c <= usedColumns; c++) {
                boolean gapColumn = (c + 1) % (BLOCK_WIDTH + 1) == 0;
                sheet.setColumnWidth(c, gapColumn ? 500 : 4200);
            }
            sheet.setColumnWidth(summaryColumn, 4500);
            sheet.setColumnWidth(summaryColumn + 1, 2500);
            sheet.setColumnWidth(summaryColumn + 2, 3200);

            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                workbook.write(fos);
            }
        }
        log.info("XLSX report generated successfully: {}", outputFile.getAbsolutePath());
        return outputFile;
    }

    /** Writes one date's mini-table (date header, the 4 meal types, and its "TOTAL GERAL") at the given position. */
    private static void writeDateBlock(Sheet sheet, XlsxStyles styles, int baseRow, int baseCol,
                                        LocalDate date, Map<MealType, Long> counts) {
        Row headerRow = getOrCreateRow(sheet, baseRow);
        Cell dateCell = headerRow.createCell(baseCol);
        dateCell.setCellValue(date.format(DATE_FORMAT));
        dateCell.setCellStyle(styles.dateHeader);
        Cell dateSpacerCell = headerRow.createCell(baseCol + 1);
        dateSpacerCell.setCellStyle(styles.dateHeader);
        sheet.addMergedRegion(new CellRangeAddress(baseRow, baseRow, baseCol, baseCol + 1));

        long total = 0;
        int r = 1;
        for (MealType gridType : GRID_TYPES) {
            long count = counts.getOrDefault(gridType, 0L);
            total += count;

            Row row = getOrCreateRow(sheet, baseRow + r);
            Cell labelCell = row.createCell(baseCol);
            labelCell.setCellValue(gridType.getDescription().toUpperCase());
            labelCell.setCellStyle(styles.label);

            Cell valueCell = row.createCell(baseCol + 1);
            valueCell.setCellValue(count);
            valueCell.setCellStyle(styles.value);
            r++;
        }
        r++; // blank row, same as the original spreadsheet layout

        Row totalRow = getOrCreateRow(sheet, baseRow + r);
        Cell totalLabelCell = totalRow.createCell(baseCol);
        totalLabelCell.setCellValue("TOTAL GERAL");
        totalLabelCell.setCellStyle(styles.totalLabel);
        Cell totalValueCell = totalRow.createCell(baseCol + 1);
        totalValueCell.setCellValue(total);
        totalValueCell.setCellStyle(styles.totalValue);
    }

    /**
     * Writes the period's summary (quantity and R$ per meal type, plus the
     * grand total) starting at (startRow, startColumn) -- placed beside the
     * date-block grid (see the call site in generatePeriodReportXlsx), using
     * getOrCreateRow since some of those rows are already shared with the
     * grid's own date blocks at lower columns.
     */
    private static void writeSummary(Sheet sheet, XlsxStyles styles, int startRow, int startColumn, List<Meal> meals) {
        Map<MealType, Long> quantityByType = new EnumMap<>(MealType.class);
        Map<MealType, BigDecimal> totalByType = new EnumMap<>(MealType.class);
        for (Meal m : meals) {
            if (m.getType() == MealType.LANCHE) {
                continue; // not part of this report's grid, so not part of its summary either
            }
            quantityByType.merge(m.getType(), 1L, Long::sum);
            totalByType.merge(m.getType(), m.getPrice(), BigDecimal::add);
        }

        DecimalFormat money = new DecimalFormat("#,##0.00");
        int r = startRow;

        Row titleRow = getOrCreateRow(sheet, r++);
        titleRow.createCell(startColumn).setCellValue("TOTAL DO PERÍODO");

        Row headerRow = getOrCreateRow(sheet, r++);
        writeCell(headerRow, startColumn, "TIPO DE REFEIÇÃO", styles.summaryHeader);
        writeCell(headerRow, startColumn + 1, "QTDE.", styles.summaryHeader);
        writeCell(headerRow, startColumn + 2, "R$", styles.summaryHeader);

        long grandTotalQty = 0;
        BigDecimal grandTotalMoney = BigDecimal.ZERO;
        for (MealType gridType : GRID_TYPES) {
            long qty = quantityByType.getOrDefault(gridType, 0L);
            BigDecimal value = totalByType.getOrDefault(gridType, BigDecimal.ZERO);
            grandTotalQty += qty;
            grandTotalMoney = grandTotalMoney.add(value);

            Row row = getOrCreateRow(sheet, r++);
            writeCell(row, startColumn, gridType.getDescription().toUpperCase(), styles.summaryLabel);
            writeCell(row, startColumn + 1, String.valueOf(qty), styles.summaryValue);
            writeCell(row, startColumn + 2, "R$ " + money.format(value), styles.summaryValue);
        }

        Row totalRow = getOrCreateRow(sheet, r);
        writeCell(totalRow, startColumn, "TOTAL DO PERÍODO", styles.summaryLabel);
        writeCell(totalRow, startColumn + 1, String.valueOf(grandTotalQty), styles.summaryValue);
        writeCell(totalRow, startColumn + 2, "R$ " + money.format(grandTotalMoney), styles.summaryValue);
    }

    private static void writeCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    /**
     * Rows are shared between the block-columns placed side by side (each
     * column stacks its own dates using the same row numbers, just at a
     * different column offset) -- so a row already created by an earlier
     * block-column must be reused, never recreated (that would wipe out
     * whatever was already written in it).
     */
    private static Row getOrCreateRow(Sheet sheet, int rowIndex) {
        Row row = sheet.getRow(rowIndex);
        return row != null ? row : sheet.createRow(rowIndex);
    }

    /** Groups every reusable cell style for the .xlsx report in one place, built once per workbook. */
    private static final class XlsxStyles {
        final CellStyle title;
        final CellStyle dateHeader;
        final CellStyle label;
        final CellStyle value;
        final CellStyle totalLabel;
        final CellStyle totalValue;
        final CellStyle summaryHeader;
        final CellStyle summaryLabel;
        final CellStyle summaryValue;

        XlsxStyles(Workbook wb) {
            Font boldFont = wb.createFont();
            boldFont.setBold(true);

            Font titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);

            Font blueBoldFont = wb.createFont();
            blueBoldFont.setBold(true);
            blueBoldFont.setColor(IndexedColors.DARK_BLUE.getIndex());

            title = wb.createCellStyle();
            title.setFont(titleFont);

            dateHeader = wb.createCellStyle();
            dateHeader.setFont(boldFont);
            dateHeader.setAlignment(HorizontalAlignment.CENTER);
            border(dateHeader);

            label = wb.createCellStyle();
            border(label);

            value = wb.createCellStyle();
            value.setAlignment(HorizontalAlignment.CENTER);
            border(value);

            totalLabel = wb.createCellStyle();
            totalLabel.setFont(boldFont);
            border(totalLabel);

            totalValue = wb.createCellStyle();
            totalValue.setFont(boldFont);
            totalValue.setAlignment(HorizontalAlignment.CENTER);
            border(totalValue);

            summaryHeader = wb.createCellStyle();
            summaryHeader.setFont(blueBoldFont);
            summaryHeader.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            summaryHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            border(summaryHeader);

            summaryLabel = wb.createCellStyle();
            summaryLabel.setFont(blueBoldFont);
            summaryLabel.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            summaryLabel.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            border(summaryLabel);

            summaryValue = wb.createCellStyle();
            summaryValue.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            summaryValue.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            summaryValue.setAlignment(HorizontalAlignment.CENTER);
            border(summaryValue);
        }

        private static void border(CellStyle style) {
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
        }
    }


    /** Human-readable description of the filters actually applied -- shown in the report header, or omitted if none. */
    private static String describeFilters(ReportFilters filters) {
        List<String> parts = new ArrayList<>();
        if (!filters.categories().isEmpty()) {
            String label = filters.excludeCategories() ? "Categoria (exceto)" : "Categoria";
            String values = filters.categories().stream()
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .collect(Collectors.joining(", "));
            parts.add(label + ": " + values);
        }
        if (!filters.types().isEmpty()) {
            String label = filters.excludeTypes() ? "Tipo (exceto)" : "Tipo";
            List<MealType> orderedTypes = new ArrayList<>(filters.types());
            orderedTypes.sort(Comparator.comparingInt(t -> java.util.Arrays.asList(TYPE_DISPLAY_ORDER).indexOf(t)));
            String values = orderedTypes.stream()
                    .map(MealType::getDescription)
                    .collect(Collectors.joining(", "));
            parts.add(label + ": " + values);
        }
        if (filters.employee() != null) {
            parts.add("Colaborador: " + filters.employee().getName());
        }
        return String.join("   ", parts);
    }

    // Fixed display order (same order used on screen for the configurable
    // prices: Breakfast, Lunch, Dinner, Supper -- Snack last, since it's no
    // longer assigned automatically, see MealType.byTime).
    private static final MealType[] TYPE_DISPLAY_ORDER = {
            MealType.CAFE_DA_MANHA, MealType.ALMOCO, MealType.JANTAR, MealType.CEIA, MealType.LANCHE
    };

    /**
     * Builds a single formatted line with the quantity of meals per type in
     * the period (e.g. "Dejejum: 12   Almoço: 8   Jantar: 5"), used in
     * the report's summary. Types with zero meals in the period are omitted.
     */
    private static String formatTypeBreakdown(List<Meal> meals) {
        Map<MealType, Long> countByType = new EnumMap<>(MealType.class);
        for (Meal m : meals) {
            countByType.merge(m.getType(), 1L, Long::sum);
        }

        StringBuilder result = new StringBuilder();
        for (MealType type : TYPE_DISPLAY_ORDER) {
            long count = countByType.getOrDefault(type, 0L);
            if (count > 0) {
                if (result.length() > 0) {
                    result.append("   ");
                }
                result.append(type.getDescription()).append(": ").append(count);
            }
        }
        return result.length() > 0 ? result.toString() : "Nenhuma refeição no período.";
    }

    /**
     * Builds a single formatted line with the quantity of meals per employee
     * category in the period (e.g. "ALUNOS: 45   FUNCIONÁRIOS: 30"), used in
     * the report's summary alongside the per-type breakdown. Unlike meal
     * type, categories aren't a fixed set (they're free text on the employee
     * record), so they're sorted alphabetically instead of a fixed order. A
     * meal whose employee has no category set is grouped under "Sem categoria".
     */
    /**
     * Normalizes an employee's category for display/grouping purposes: a
     * null/blank category (employee record with no category set) is grouped
     * under "Sem categoria" instead of showing up blank. Shared by the
     * category breakdown line below and by the PDF report's category
     * grouping (generatePeriodReport), so both treat "no category" the same way.
     */
    private static String normalizeCategory(String category) {
        return (category == null || category.isBlank()) ? "Sem categoria" : category;
    }

    private static String formatCategoryBreakdown(List<Meal> meals) {
        Map<String, Long> countByCategory = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Meal m : meals) {
            countByCategory.merge(normalizeCategory(m.getEmployeeCategory()), 1L, Long::sum);
        }

        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, Long> entry : countByCategory.entrySet()) {
            if (result.length() > 0) {
                result.append("   ");
            }
            result.append(entry.getKey()).append(": ").append(entry.getValue());
        }
        return result.length() > 0 ? result.toString() : "Nenhuma refeição no período.";
    }

    private void validate(Meal m) {
        if (m.getEmployeeId() == null) {
            throw new IllegalArgumentException("Selecione um colaborador.");
        }
        if (m.getDate() == null) {
            throw new IllegalArgumentException("Informe a data da refeição.");
        }
        if (m.getTime() == null) {
            throw new IllegalArgumentException("Informe o horário da refeição.");
        }
        if (m.getPrice() == null || m.getPrice().signum() < 0) {
            throw new IllegalArgumentException("Preço inválido.");
        }
        if (m.getType() == null) {
            throw new IllegalArgumentException("Selecione o tipo de refeição.");
        }
    }

    /**
     * Enforces one meal per type per employee per day (agreed with RH): an
     * employee can't have, say, two lunches or three dinners registered on
     * the same date -- regardless of whether the meals came from manual
     * registration or bulk import (.txt), since both paths call
     * register()/update() and therefore both go through this check.
     * Deactivated (canceled) meals don't count as a conflict -- a
     * deactivated lunch no longer represents an actual use of the
     * cafeteria that day, so a new one can still be registered in its
     * place. When updating an existing meal, that meal's own record
     * (matched by id) is excluded, so saving an edit that doesn't change
     * date/type doesn't flag itself as a duplicate of itself.
     */
    private void checkNoDuplicateMealType(Meal meal) {
        if (meal.isEmployeeSharedUsage()) {
            // This matrícula is intentionally shared by more than one person
            // (see Employee.sharedUsage, e.g. a "plantão médico" badge) --
            // the rule assumes one matrícula == one person, so it doesn't
            // apply here: several different people legitimately eating under
            // the same registration on the same day is expected, not a
            // duplicate.
            return;
        }
        List<Meal> sameDay = mealDao.findByEmployeeAndPeriod(meal.getEmployeeId(), meal.getDate(), meal.getDate());
        boolean conflict = sameDay.stream()
                .filter(Meal::isActive)
                .filter(m -> m.getType() == meal.getType())
                // Objects.equals (not m.getId().equals(...)) so this can't NPE
                // if either id happens to be null -- meal.getId() IS null on
                // every new register() (not assigned until mealDao.save()
                // returns), which must never be mistaken for "this is the
                // same record as an existing one" just because both are null.
                .anyMatch(m -> !Objects.equals(m.getId(), meal.getId()));
        if (conflict) {
            throw new DuplicateMealException(
                    "Este colaborador já tem uma refeição do tipo " + meal.getType().getDescription()
                            + " registrada em " + meal.getDate().format(DATE_FORMAT)
                            + ". Só é permitida uma refeição de cada tipo por dia.");
        }
    }
}
