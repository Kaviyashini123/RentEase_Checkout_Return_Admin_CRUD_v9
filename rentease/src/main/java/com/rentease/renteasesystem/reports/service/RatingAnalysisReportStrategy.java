package com.rentease.renteasesystem.reports.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfPTable;
import com.rentease.renteasesystem.reports.model.ReportExportConfig;
import com.rentease.renteasesystem.reports.model.Review;
import org.springframework.stereotype.Component;

import java.util.List;

/** Generates summary and detail tables from the current review records. */
@Component
public class RatingAnalysisReportStrategy implements ReportGenerationStrategy {
    @Override public ReportExportConfig.ReportType reportType() { return ReportExportConfig.ReportType.RATING_ANALYSIS; }
    @Override public String title() { return "Customer Rating Analysis Report"; }
    @Override public String description() { return "All submitted customer ratings and feedback across available records; the selected month is retained as export metadata."; }

    @Override
    public void generate(Document document, ReportGenerationContext context) throws DocumentException {
        List<Review> reviews = context.reportData().getAllReviews();
        double overall = reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
        long fiveStar = reviews.stream().filter(review -> review.getRating() == 5).count();
        context.addSummaryMetrics(document, new String[][] {
                {"Total reviews", String.valueOf(reviews.size())},
                {"Average rating", String.format(java.util.Locale.ENGLISH, "%.2f / 5", overall)},
                {"5-star reviews", String.valueOf(fiveStar)},
                {"Flagged reviews", String.valueOf(reviews.stream().filter(Review::isFlagged).count())}
        });
        context.addHeading(document, "Rating Summary");
        context.addKeyValueTable(document, new String[][] {
                {"Total reviews", String.valueOf(reviews.size())},
                {"Overall average rating", String.format("%.2f / 5", overall)},
                {"Flagged reviews", String.valueOf(reviews.stream().filter(Review::isFlagged).count())}
        });
        context.addHeading(document, "Rating Distribution");
        PdfPTable distribution = context.table(3);
        context.setWidths(distribution, 1.0f, 1.0f, 1.0f);
        context.addHeader(distribution, "Rating", "Reviews", "Share");
        for (int rating = 5; rating >= 1; rating--) {
            final int score = rating;
            long count = reviews.stream().filter(review -> review.getRating() == score).count();
            String share = reviews.isEmpty() ? "0.0%" : String.format(java.util.Locale.ENGLISH, "%.1f%%", count * 100.0 / reviews.size());
            context.addRow(distribution, rating + " / 5", String.valueOf(count), share);
        }
        document.add(distribution);
        context.addHeading(document, "Review Details");
        if (reviews.isEmpty()) context.addEmptyNotice(document, "No customer reviews are available for rating analysis.");
        PdfPTable table = context.table(5);
        context.setWidths(table, 0.65f, 0.75f, 0.75f, 0.65f, 3.2f);
        context.addHeader(table, "Review ID", "Vehicle", "Customer", "Rating", "Comment");
        reviews.forEach(review -> context.addRow(table, String.valueOf(review.getId()),
                String.valueOf(review.getVehicleId()), String.valueOf(review.getCustomerId()),
                review.getRating() + " / 5", context.safe(review.getComment())));
        document.add(table);
    }
}
