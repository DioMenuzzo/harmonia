package com.hospital.harmonia.service;

import com.hospital.harmonia.dao.MealDao;
import com.hospital.harmonia.dao.impl.MealDaoImpl;
import com.hospital.harmonia.model.Meal;
import com.hospital.harmonia.util.ReportGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        log.debug("Registering meal: employeeId={} date={} time={}", meal.getEmployeeId(), meal.getDate(), meal.getTime());
        return mealDao.save(meal);
    }

    public void update(Meal meal) {
        validate(meal);
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
            throw new IllegalArgumentException("Data inicial nao pode ser depois da data final.");
        }
        return mealDao.findByPeriod(start, end);
    }

    /** Only the active ones -- for the PDF report and the total spent in the period. */
    public List<Meal> findActiveByPeriod(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Data inicial nao pode ser depois da data final.");
        }
        return mealDao.findActiveByPeriod(start, end);
    }

    /** Total spent in the period -- used in the report header. */
    public BigDecimal totalForPeriod(List<Meal> meals) {
        return meals.stream()
                .map(Meal::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Generates the PDF report for the given period, using JasperReports.
     * The .jrxml file lives at src/main/resources/reports/refeitorio_report.jrxml
     */
    public File generatePeriodReport(LocalDate start, LocalDate end, File outputFile) throws Exception {
        // Only active meals go into the report/total -- a deactivated
        // (canceled) meal stays visible on screen as a log, but shouldn't be billed.
        List<Meal> meals = findActiveByPeriod(start, end);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("startDate", start.format(DATE_FORMAT));
        parameters.put("endDate", end.format(DATE_FORMAT));
        parameters.put("total", totalForPeriod(meals));
        parameters.put("quantity", meals.size());

        return ReportGenerator.generatePdf("/reports/refeitorio_report.jrxml", parameters, meals, outputFile);
    }

    private void validate(Meal m) {
        if (m.getEmployeeId() == null) {
            throw new IllegalArgumentException("Selecione um colaborador.");
        }
        if (m.getDate() == null) {
            throw new IllegalArgumentException("Informe a data da refeicao.");
        }
        if (m.getTime() == null) {
            throw new IllegalArgumentException("Informe o horario da refeicao.");
        }
        if (m.getPrice() == null || m.getPrice().signum() < 0) {
            throw new IllegalArgumentException("Preco invalido.");
        }
        if (m.getType() == null) {
            throw new IllegalArgumentException("Selecione o tipo de refeicao.");
        }
    }
}
