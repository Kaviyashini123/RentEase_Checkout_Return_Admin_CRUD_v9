package com.rentease.renteasesystem.reports.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfPTable;
import com.rentease.renteasesystem.payment.model.Payment;
import com.rentease.renteasesystem.reports.model.ReportExportConfig;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** Business-oriented monthly finance report built exclusively from recorded payment data. */
@Component
public class MonthlyRevenueReportStrategy implements ReportGenerationStrategy {
    @Override public ReportExportConfig.ReportType reportType() { return ReportExportConfig.ReportType.MONTHLY_REVENUE; }
    @Override public String title() { return "Monthly Revenue & Payments Report"; }
    @Override public String description() { return "Revenue performance, payment activity and reconciliation details for the selected month."; }

    @Override
    public void generate(Document document, ReportGenerationContext context) throws DocumentException {
        YearMonth month = context.period();
        YearMonth previousMonth = month.minusMonths(1);
        var allPayments = context.reportData().getOverviewPayments();
        var monthPayments = allPayments.stream()
                .filter(payment -> payment.getPaidAt() != null && YearMonth.from(payment.getPaidAt()).equals(month))
                .toList();
        BigDecimal revenue = context.reportData().getMonthlyRevenue(month);
        BigDecimal previousRevenue = context.reportData().getMonthlyRevenue(previousMonth);
        BigDecimal change = revenue.subtract(previousRevenue);
        String changeLabel;
        if (previousRevenue.compareTo(BigDecimal.ZERO) == 0) {
            changeLabel = revenue.compareTo(BigDecimal.ZERO) == 0 ? "No change" : "No prior baseline";
        } else {
            double percent = change.multiply(BigDecimal.valueOf(100))
                    .divide(previousRevenue, 2, java.math.RoundingMode.HALF_UP).doubleValue();
            changeLabel = String.format(Locale.ENGLISH, "%+.1f%%", percent);
        }
        long successfulCount = monthPayments.stream()
                .filter(payment -> payment.getStatus() == Payment.PaymentStatus.SUCCESS).count();

        context.addSummaryMetrics(document, new String[][] {
                {"Successful revenue", "LKR " + context.money(revenue)},
                {"Vs. previous month", changeLabel},
                {"Successful payments", String.valueOf(successfulCount)},
                {"Transactions dated this month", String.valueOf(monthPayments.size())}
        });

        context.addHeading(document, "Executive Summary");
        context.addKeyValueTable(document, new String[][] {
                {"Reporting period", month.getMonth().getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH) + " " + month.getYear()},
                {"Successful revenue", "LKR " + context.money(revenue)},
                {"Previous month revenue", "LKR " + context.money(previousRevenue)},
                {"Revenue movement", "LKR " + (change.signum() > 0 ? "+" : "") + context.money(change) + " (" + changeLabel + ")"},
                {"Reporting rule", "Revenue includes payments with SUCCESS status and a paid date within the selected month."}
        });

        context.addHeading(document, "Payment Method Mix");
        if (monthPayments.isEmpty()) {
            context.addEmptyNotice(document, "No payment transactions with a recorded paid date were found for this month.");
        } else {
            Map<String, BigDecimal> methodAmounts = monthPayments.stream()
                    .filter(payment -> payment.getMethod() != null)
                    .collect(Collectors.groupingBy(payment -> payment.getMethod().name(), Collectors.mapping(
                            payment -> payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount(),
                            Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
            Map<String, Long> methodCounts = monthPayments.stream().filter(payment -> payment.getMethod() != null)
                    .collect(Collectors.groupingBy(payment -> payment.getMethod().name(), Collectors.counting()));
            PdfPTable methods = context.table(3);
            context.setWidths(methods, 2.0f, 1.0f, 1.5f);
            context.addHeader(methods, "Payment method", "Transactions", "Recorded amount (LKR)");
            methodAmounts.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> context.addRow(methods,
                    entry.getKey().replace('_', ' '), String.valueOf(methodCounts.getOrDefault(entry.getKey(), 0L)), context.money(entry.getValue())));
            document.add(methods);
        }

        context.addHeading(document, "Current Payment Status Snapshot");
        context.addEmptyNotice(document,
                "This snapshot covers all recorded payment records, regardless of reporting month. Pending or failed payments without a paid date cannot be assigned reliably to a specific month because the payment record does not store a creation timestamp.");
        PdfPTable statuses = context.table(3);
        context.setWidths(statuses, 1.8f, 1.0f, 1.6f);
        context.addHeader(statuses, "Current status", "Records", "Recorded amount (LKR)");
        for (Payment.PaymentStatus status : Payment.PaymentStatus.values()) {
            var matching = allPayments.stream().filter(payment -> payment.getStatus() == status).toList();
            BigDecimal amount = matching.stream().map(Payment::getAmount).filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            context.addRow(statuses, status.name().replace('_', ' '), String.valueOf(matching.size()), context.money(amount));
        }
        document.add(statuses);

        context.addHeading(document, "Monthly Transaction Register");
        if (monthPayments.isEmpty()) context.addEmptyNotice(document, "No transactions are available for the selected month.");
        PdfPTable table = context.table(7);
        context.setWidths(table, 0.62f, 0.72f, 1.0f, 1.15f, 0.9f, 1.2f, 0.95f);
        context.addHeader(table, "Payment ID", "Reservation", "Customer ID", "Amount (LKR)", "Method", "Status", "Paid at");
        monthPayments.stream().sorted(java.util.Comparator.comparing(Payment::getPaidAt).reversed()).forEach(payment ->
                context.addRow(table, String.valueOf(payment.getId()), String.valueOf(payment.getReservationId()),
                        String.valueOf(payment.getCustomerId()), context.money(payment.getAmount()),
                        payment.getMethod() == null ? "—" : payment.getMethod().name().replace('_', ' '),
                        payment.getStatus() == null ? "—" : payment.getStatus().name(), context.humanDateTime(payment.getPaidAt())));
        document.add(table);

        context.addHeading(document, "Reconciliation Notes");
        context.addKeyValueTable(document, new String[][] {
                {"Revenue basis", "Successful payments only; amount is not treated as revenue if the payment is pending, failed or refunded."},
                {"Transaction register", "Includes records with a paid timestamp in the selected month, regardless of current status."},
                {"Refund treatment", "Refunded records are shown in the status snapshot; this report does not subtract refunds from successful revenue without a separately defined refund accounting policy."},
                {"Data integrity", "Values are generated from the current database records at export time; no missing dates or amounts are inferred."}
        });
    }
}
