package com.bm.wschat.feature.wiki.service;

import com.bm.wschat.feature.wiki.model.WikiArticle;
import com.bm.wschat.feature.wiki.repository.WikiArticleRepository;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class WikiDownloadService {

    private final WikiArticleRepository wikiArticleRepository;

    private static final DateTimeFormatter PDF_DATE_FORMAT =
            DateTimeFormatter
                    .ofPattern("dd.MM.yyyy HH:mm")
                    .withZone(ZoneId.systemDefault());

//    Получает данные вики статьи из БД и формирует PDF файл

    public void generatePdf(HttpServletResponse response, String slug) throws IOException {

        WikiArticle article = wikiArticleRepository.findBySlug(slug)
                .orElseThrow(() -> new EntityNotFoundException("Вики статья не найдена"));

        response.setContentType("application/pdf");
        response.setHeader(
                "Content-Disposition",
                "attachment; filename=\"" + article.getSlug() + ".pdf\""
        );

        PdfWriter writer = new PdfWriter(response.getOutputStream());
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A4);
        document.setMargins(50, 50, 50, 50);

        PdfFont regular = loadFontFromResources("font/Roboto/static/Roboto-Regular.ttf");
        PdfFont semiBold = loadFontFromResources("font/Roboto/static/Roboto-SemiBold.ttf");
        PdfFont bold = loadFontFromResources("font/Roboto/static/Roboto-Bold.ttf");

        // ===== Заголовок =====
        document.add(new Paragraph(article.getTitle())
                .setFont(bold)
                .setFontSize(22)
                .simulateBold()
                .setMarginBottom(12)
        );

        // ===== Метаданные =====
        String author = article.getCreatedBy().getFio() != null
                ? article.getCreatedBy().getFio()
                : article.getCreatedBy().getUsername();

        Paragraph meta = new Paragraph()
                .setFont(semiBold)
                .setFontSize(10)
                .setFontColor(ColorConstants.GRAY)
                .add("Автор: " + author + "\n")
                .add("Создано: " + PDF_DATE_FORMAT.format(article.getCreatedAt()) + "\n");

        if (article.getCategory() != null) {
            meta.add("Категория: " + article.getCategory().getName() + "\n");
        }

        document.add(meta);

        // ===== Теги (как бейджи) =====
        if (!article.getTagSet().isEmpty()) {
            Paragraph tags = new Paragraph()
                    .setFont(bold)
                    .setFontSize(9)
                    .setMarginBottom(10);

            for (String tag : article.getTagSet()) {
                tags.add(new Text(" " + tag + " ")
                        .setBackgroundColor(new DeviceRgb(230, 230, 250))
                        .setFontColor(ColorConstants.BLACK)
                );
                tags.add(" ");
            }
            document.add(tags);
        }

        // ===== Разделитель =====
        document.add(new LineSeparator(new SolidLine()).setMarginBottom(10));

        // ===== Контент =====
        document.add(new Paragraph(article.getContent())
                .setFont(regular)
                .setFontSize(12)
                .setMultipliedLeading(1.6f)
                .setTextAlignment(TextAlignment.JUSTIFIED)
        );

        // ===== Footer =====
        document.add(new Paragraph("Сгенерировано: " + PDF_DATE_FORMAT.format(Instant.now()))
                .setFont(semiBold)
                .setFontSize(8)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginTop(30)
        );

        document.close();
    }

    private PdfFont loadFontFromResources(String resourcePath) throws IOException {

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalStateException("Шрифт не найден в resources: " + resourcePath);
            }

            byte[] fontBytes = is.readAllBytes();

            return PdfFontFactory.createFont(
                    fontBytes,
                    PdfEncodings.IDENTITY_H,
                    PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED
            );
        }
    }

}
