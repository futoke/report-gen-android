package com.example.ichiro.test;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.ZipInputStream;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PdfGenerator {
    private PdfTask pdfTask;
    private Context context;
    private File file; // The name of the generated file with path.

    private String tableName;
    private String[] cellContent;
    private String[] cellHeaders;
    private String[][] pages;

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private static final String DOCX_FILE_TEMPLATE = "template.docx";
    private static final String DOCX_DOCUMENT_PATH = "word/document.xml";

    private static final byte ENGLISH = 0;
    private static final byte RUSSIAN = 1;
    private static final byte CHINESE = 2;

    private static final Byte HEADER_1 = 0;
    private static final Byte HEADER_3 = 1;
    private static final Byte HEADER_4 = 2;
    private static final Byte HEADER_5 = 3;
    private static final Byte HEADER_6 = 4;
    private static final Byte HEADER_2 = 5;
    private static final Byte HEADER_8 = 6;
    private static final Byte HEADER_81 = 7;
    private static final Byte HEADER_82 = 8;
    private static final Byte HEADER_9 = 9;
    private static final Byte HEADER_7 = 10;
    private static final Byte HEADER_10 = 11;
    private static final Byte HEADER_11 = 12;

    private static final Byte CONTENT_1 = 0;
    private static final Byte CONTENT_3 = 1;
    private static final Byte CONTENT_4 = 2;
    private static final Byte CONTENT_5 = 3;
    private static final Byte CONTENT_6 = 4;
    private static final Byte CONTENT_2 = 5;
    private static final Byte CONTENT_81 = 6;
    private static final Byte CONTENT_82 = 7;
    private static final Byte CONTENT_9 = 8;
    private static final Byte CONTENT_7 = 9;
    private static final Byte CONTENT_10 = 10;
    private static final Byte CONTENT_11 = 11;

    private static final Integer ENGLISH_CHUNK_SIZES[] = {
            300, // Content 1.
            300, // Content 3.
            750, // Content 4.
            300, // Content 5.
            300, // Content 6.
            300, // Content 2.
            200, // Content 81.
            200, // Content 82.
            300, // Content 9.
            300, // Content 7.
            850, // Content 10.
            850  // Content 11.
    };
    private static final Integer RUSSIAN_CHUNK_SIZES[] = {
            280, // Content 1.
            250, // Content 3.
            630, // Content 4.
            250, // Content 5.
            280, // Content 6.
            280, // Content 2.
            200, // Content 81.
            200, // Content 82.
            280, // Content 9.
            280, // Content 7.
            650, // Content 10.
            650  // Content 11.
    };
    private static final Integer CHINESE_CHUNK_SIZES[] = {
            130, // Content 1.
            130, // Content 3.
            300, // Content 4.
            130, // Content 5.
            130, // Content 6.
            130, // Content 2.
            80, // Content 81.
            80, // Content 82.
            130, // Content 9.
            130, // Content 7.
            300, // Content 10.
            300  // Content 11.
    };

    private static final Float PAGE_HEIGHT = 595.0f;
    private static final Float TOP_MARGIN = 40.0f;
    private static final Float BOTTOM_MARGIN = 2.0f;
    private static final Float LEFT_MARGIN = 2.0f;
    private static final Float RIGHT_MARGIN = 2.0f;
    private static final String PATH_TO_DROID_SANS = "/system/fonts/DroidSansFallback.ttf";
    private static final Float CELL_FIRST_LINE_INDENT = 20.0f;

    private static BaseFont droidSans;

    private static Font tableNameFont;
    private static Font headerFont;
    private static Font subHeaderFont;
    private static Font textFont;
    private static Font textFontSmall;

    /**
     * @param context
     * @param tableName The table name. Place in the header.
     * @param cellContent
     * @param cellHeaders
     */
    public
    PdfGenerator (
            Context context,
            String tableName,
            String[] cellContent,
            String[] cellHeaders)
    {
        this.context = context;
        this.tableName = tableName;
        this.cellContent = cellContent;
        this.cellHeaders = cellHeaders;

        pdfTask = new PdfTask();
    }

    /**
     * The function detects language of a string.
     *
     * @param str The input string.
     * @return Language as a constant from the list {RUSSIAN, ENGLISH, CHINESE}.
     */
    private byte
    detectLang(String str)
    {
        Integer[] freqLangList;
        Integer freqLang;
        Byte mostCommonLang;
        Character.UnicodeBlock ub;

        mostCommonLang = ENGLISH;
        freqLangList = new Integer[]{0, 0, 0};
        freqLang = 0;

        for (char c : str.toCharArray() ) {
            if (Character.isLetter(c)) {
                ub = Character.UnicodeBlock.of(c);
                if (ub == Character.UnicodeBlock.BASIC_LATIN) {
                    freqLangList[ENGLISH] += 1;
                } else if (ub == Character.UnicodeBlock.CYRILLIC) {
                    freqLangList[RUSSIAN] += 1;
                } else {
                    freqLangList[CHINESE] += 1;
                }
            }
        }

        for (Byte i = 0; i < 3; i++) {
            if (freqLangList[i] > freqLang) {
                freqLang = freqLangList[i];
                mostCommonLang = i;
            }
        }

        return mostCommonLang;
    }


    /**
     *
     */
    private void
    divIntoPages()
    {
        Integer cols;
        Integer rows;
        Integer cnt;
        Byte lang;
        String buffer;
        List<String> dividedCell;
        List<List<String>> cellChunks;

        rows = 0;
        cols = cellContent.length;
        cellChunks = new ArrayList<>();


        // Combine all strings to detect language.
        lang = detectLang(StringUtils.join(cellContent, ""));


        for (int i = 0; i < cols; i++) {
            switch (lang) {
                case ENGLISH:
                    buffer = WordUtils.wrap(cellContent[i], ENGLISH_CHUNK_SIZES[i]);
                    break;
                case RUSSIAN:
                    buffer = WordUtils.wrap(cellContent[i], RUSSIAN_CHUNK_SIZES[i]);
                    break;
                case CHINESE:
                    buffer = "";
                    cnt = 0;
                    for (char c : cellContent[i].toCharArray()) {
                        buffer += c;
                        if (cnt.equals(CHINESE_CHUNK_SIZES[i])) {
                            cnt = 0;
                            buffer += LINE_SEPARATOR;
                        } else {
                            cnt++;
                        }
                    }
//                    buffer = WordUtils.wrap(
//                            cellContent[i],
//                            CHINESE_CHUNK_SIZES[i],
//                            LINE_SEPARATOR,
//                            true
//                    );
                    break;
                default:
                    buffer = WordUtils.wrap(cellContent[i], ENGLISH_CHUNK_SIZES[i]);
            }

            dividedCell = new ArrayList<>(
                    Arrays.asList(
                            buffer.split(LINE_SEPARATOR)
                    )
            );
            cellChunks.add(dividedCell);

            if (rows < dividedCell.size()) {
                rows = dividedCell.size();
            }
        }

        pages = new String[rows][cols];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                try {
                    pages[i][j] = cellChunks.get(j).get(i);
                } catch (IndexOutOfBoundsException iobe) {
                    pages[i][j] = "";
                }

            }
        }
    }

    /**
     *
     */
    private void
    createFonts()
    {
        try {
            droidSans = BaseFont.createFont(
                    PATH_TO_DROID_SANS,
                    BaseFont.IDENTITY_H,
                    BaseFont.EMBEDDED
            );
        } catch (Exception ex) {
            ex.printStackTrace(); // TODO
        }

        tableNameFont = new Font(droidSans, 18, Font.NORMAL, BaseColor.BLUE);
        headerFont = new Font(droidSans, 12, Font.BOLD, BaseColor.MAGENTA);
        subHeaderFont = new Font(droidSans, 10, Font.BOLD, BaseColor.MAGENTA);
        textFont = new Font(droidSans, 10, Font.NORMAL, BaseColor.BLACK);
        textFontSmall = new Font(droidSans, 7, Font.NORMAL, BaseColor.BLACK);
    }

    /**
     * @param document
     * @param title
     * @throws DocumentException
     * @throws IOException
     */
    private void
    createTitle(Document document, String title)
            throws DocumentException, IOException
    {
        Paragraph titleParagraph = new Paragraph();
        float spacingAfter = 10.0f;

        titleParagraph.setFont(tableNameFont);
        titleParagraph.add(title);
        titleParagraph.setAlignment(Element.ALIGN_CENTER);
        // Instead of the top page margin.
        titleParagraph.setLeading(TOP_MARGIN - spacingAfter);
        titleParagraph.setSpacingAfter(spacingAfter);

        document.add(titleParagraph);
    }

    /**
     * @param header
     * @param content
     * @return
     */
    private PdfPCell
    createCell(String header, String content)
    {
        PdfPCell cell;
        Paragraph cellContent;

        cell = new PdfPCell();
        cellContent = new Paragraph(content, textFont);

        cellContent.setFirstLineIndent(20.0f);

        cell.addElement(new Paragraph(header, headerFont));
        cell.addElement(cellContent);

        return cell;
    }

    /**
     * @param header
     * @param subHeader_1
     * @param subHeader_2
     * @param content_1
     * @param content_2
     * @return
     */
    private PdfPCell
    createNestedTable(
            String header,
            String subHeader_1,
            String subHeader_2,
            String content_1,
            String content_2)
    {
        PdfPTable nestedTable = new PdfPTable(1);
        nestedTable.setWidthPercentage(100.0f);
        nestedTable.setSpacingBefore(0.0f);
        nestedTable.setSpacingAfter(0.0f);

        // Row 1 in the nested table.
        Paragraph cellHeader = new Paragraph(header, headerFont);
        Paragraph cellSubheader = new Paragraph(subHeader_1, subHeaderFont);
        cellSubheader.setFirstLineIndent(CELL_FIRST_LINE_INDENT);
        Paragraph cellContent = new Paragraph(content_1, textFontSmall);
        cellContent.setFirstLineIndent(30.0f);

        PdfPCell nestedTableCell = new PdfPCell();
        nestedTableCell.addElement(cellHeader);
        nestedTableCell.addElement(cellSubheader);
        nestedTableCell.addElement(cellContent);

        nestedTableCell.setBorder(PdfPCell.NO_BORDER);
        nestedTableCell.setFixedHeight(
                (PAGE_HEIGHT - TOP_MARGIN - BOTTOM_MARGIN) / 6
        );
        nestedTable.addCell(nestedTableCell);

        // Row 2 in the nested table.
        cellSubheader = new Paragraph(subHeader_2, subHeaderFont);
        cellSubheader.setFirstLineIndent(CELL_FIRST_LINE_INDENT);
        cellContent = new Paragraph(content_2, textFontSmall);
        cellContent.setFirstLineIndent(30.0f);

        nestedTableCell = new PdfPCell();
        nestedTableCell.addElement(cellHeader);
        nestedTableCell.addElement(cellSubheader);
        nestedTableCell.addElement(cellContent);
        nestedTableCell.setBorder(PdfPCell.NO_BORDER);
        nestedTable.addCell(nestedTableCell);

        PdfPCell cell = new PdfPCell();
        cell.setPadding(0);
        cell.addElement(nestedTable);

        return cell;
    }

    /**
     * @param document
     * @param cellContentChunks
     * @throws DocumentException
     */
    private void
    createTable(Document document, String[] cellContentChunks)
            throws DocumentException
    {
        Paragraph table = new Paragraph();
        float rowHeight;
        PdfPCell cell;

        // The main table fits top and bottom parts.
        PdfPTable mainTable = new PdfPTable(1);
        mainTable.setWidthPercentage(100.0f);
        mainTable.setSpacingBefore(0.0f);
        mainTable.setSpacingAfter(0.0f);

        // Calculate row height.
        rowHeight = (PAGE_HEIGHT - TOP_MARGIN - BOTTOM_MARGIN) / 3;

        // Define the top part of the table.
        PdfPCell topTableCell = new PdfPCell();
        topTableCell.setBorder(PdfPCell.NO_BORDER);
        topTableCell.setPadding(0);

        PdfPTable topTable = new PdfPTable(5);
        topTable.setWidthPercentage(100.0f);
        topTable.setSpacingBefore(0.0f);
        topTable.setSpacingAfter(0.0f);

        // Cell 1 (row 1).
        cell = createCell(cellHeaders[HEADER_1], cellContentChunks[CONTENT_1]);
        cell.setFixedHeight(rowHeight);
        cell.setBorder(Rectangle.TOP | Rectangle.RIGHT | Rectangle.LEFT);
        topTable.addCell(cell);

        // Cell 2.
        cell = createCell(cellHeaders[HEADER_3], cellContentChunks[CONTENT_3]);
        topTable.addCell(cell);

        // Cell 3.
        cell = createCell(cellHeaders[HEADER_4], cellContentChunks[CONTENT_4]);
        cell.setRowspan(2);
        topTable.addCell(cell);

        // Cell 4.
        cell = createCell(cellHeaders[HEADER_5], cellContentChunks[CONTENT_5]);
        topTable.addCell(cell);

        // Cell 5.
        cell = createCell(cellHeaders[HEADER_6], cellContentChunks[CONTENT_6]);
        cell.setBorder(Rectangle.TOP | Rectangle.RIGHT | Rectangle.LEFT);
        topTable.addCell(cell);

        // Cell 6 (row2).
        cell = createCell(cellHeaders[HEADER_2], cellContentChunks[CONTENT_2]);
        cell.setFixedHeight(rowHeight);
        cell.setBorder(Rectangle.BOTTOM | Rectangle.RIGHT | Rectangle.LEFT);
        topTable.addCell(cell);

        // Cell 7 fits a nested table.
        cell = createNestedTable(
                cellHeaders[HEADER_8],
                cellHeaders[HEADER_81],
                cellHeaders[HEADER_82],
                cellContentChunks[CONTENT_81],
                cellContentChunks[CONTENT_82]
        );
        topTable.addCell(cell);

        // Cell 8.
        cell = createCell(cellHeaders[HEADER_9], cellContentChunks[CONTENT_9]);
        topTable.addCell(cell);

        // Cell 9.
        cell = createCell(cellHeaders[HEADER_7], cellContentChunks[CONTENT_7]);
        cell.setBorder(Rectangle.BOTTOM | Rectangle.RIGHT | Rectangle.LEFT);
        topTable.addCell(cell);

        // Add the top table into the main table.
        topTableCell.addElement(topTable);
        mainTable.addCell(topTableCell);

        // Define the bottom part.
        PdfPCell bottomTableCell = new PdfPCell();
        bottomTableCell.setBorder(PdfPCell.NO_BORDER);
        bottomTableCell.setPadding(0);

        PdfPTable bottomTable = new PdfPTable(2);
        bottomTable.setWidthPercentage(100.0f);
        bottomTable.setSpacingBefore(0.0f);
        bottomTable.setSpacingAfter(0.0f);

        // Left cell of the bottom table.
        cell = createCell(cellHeaders[HEADER_10], cellContentChunks[CONTENT_10]);
        cell.setFixedHeight(rowHeight);
        bottomTable.addCell(cell);

        // Right cell of the bottom table.
        cell = createCell(cellHeaders[HEADER_11], cellContentChunks[CONTENT_11]);
        bottomTable.addCell(cell);

        // Add the bottom table into the main table.
        bottomTableCell.addElement(bottomTable);
        mainTable.addCell(bottomTableCell);

        table.add(mainTable);

        document.add(table);
    }

    /**
     * @param destFilePath
     */
    private void
    copyTemplateFromAssets(File destFilePath)
    {
        AssetManager assetManager = context.getAssets();
        InputStream in = null;
        OutputStream out = null;

        try {
            in = assetManager.open(DOCX_FILE_TEMPLATE);
            out = new FileOutputStream(destFilePath);
            copyFile(in, out);
        } catch(IOException e) {
            Log.e(
                    "tag", // TODO: 25.10.15
                    String.format(
                            "Failed to copy asset file: %s",
                            destFilePath
                    ),
                    e
            );
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // TODO: 25.10.15
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // TODO: 25.10.15
                }
            }
        }
    }

    /**
     * @param in
     * @param out
     * @throws IOException
     */
    private void
    copyFile(InputStream in, OutputStream out)
            throws IOException
    {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    /**
     * @throws IOException
     * @throws DocumentException
     */
    private void
    fillPdfFile()
            throws IOException, DocumentException
    {
        createFonts();

        Document document = new Document(
                PageSize.A4.rotate(),
                LEFT_MARGIN,
                RIGHT_MARGIN,
                /*
                Actually, top margin will define in the padding field
                of the title paragraph.
                */
                TOP_MARGIN - TOP_MARGIN,
                BOTTOM_MARGIN
        );
        OutputStream fos = new FileOutputStream(file);
        PdfWriter.getInstance(document, fos);
        document.open();

//        divIntoPages();

//        for (int i = 0; i < pages.length; i++) {
//            document.newPage();
            createTitle(document, tableName);
//            createTable(document, pages[i]);
//        }
        document.close();
    }

    private void fillDocxFile() {
        String documentXml;
        ZipInputStream zis = null;
        InputStream is = null;

        File path = new File(String.format("%s%s", file, ".zip")); // FIXME: 25.10.15
        
        copyTemplateFromAssets(path);

        try {
            ZipFile zipFile = new ZipFile(path);
            zis = zipFile.getInputStream(zipFile.getFileHeader(DOCX_DOCUMENT_PATH));

            documentXml = IOUtils.toString(zis, "utf-8");
            documentXml = documentXml.replace("#header-1", "Превед");

            zipFile.removeFile(DOCX_DOCUMENT_PATH);

            ZipParameters parameters = new ZipParameters();
            parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
            parameters.setFileNameInZip(DOCX_DOCUMENT_PATH);
            parameters.setSourceExternalStream(true);

            is = IOUtils.toInputStream(documentXml);
            zipFile.addStream(is, parameters);
        } catch (ZipException e) {
            e.printStackTrace(); // TODO: 25.10.15  
        } catch (IOException e) {
            e.printStackTrace(); // TODO: 25.10.15
        } finally {
            try {
                zis.close();
                //is.close();
            } catch (IOException e) {
                e.printStackTrace(); // TODO: 25.10.15
            }
        }
    }

    /**
     * Main method
     */
    public void
    execute()
    {
        FileDialog fileSaveDialog = new FileDialog(
            context,
            DialogType.FILE_SAVE,
            new FileDialog.FileDialogListener() {
                @Override
                public void
                onChosenDir(String chosenDir)
                {
                    /* The code in this function will be executed when the dialog
                       OK button is pushed */
                    try {
                        file = new File(chosenDir);
                        pdfTask.execute();
                    } catch (Exception e) {
                        // TODO
                    }
                }
            });
        fileSaveDialog.chooseFileOrDir();
    }

//    private File createPdfFile() {
//
//        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
//            return null;
//        } else {
//            String dir = Environment.getExternalStorageDirectory() + File.separator + "pdfdemo";
//
//            File folder = new File(dir);
//            if (!folder.exists()) {
//                folder.mkdirs();
//            }
//            return new File(dir, "test.pdf");
//        }
//    }

    /**
     *
     */
    private void
    viewPdfFile()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setMessage(
            String.format(
                context.getResources().getString(R.string.msg_file_written),
                file.toString()
            )
        );
        builder.setIcon(android.R.drawable.ic_dialog_info);

        builder.setPositiveButton(R.string.button_yes, new DialogInterface.OnClickListener() {
            public void
            onClick(DialogInterface dialog, int id)
            {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(file), "application/pdf");
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                context.startActivity(intent);
            }
        });
        builder.setNegativeButton(R.string.button_no, new DialogInterface.OnClickListener() {
            public void
            onClick(DialogInterface dialog, int id)
            {
            }
        });
       builder.show();
    }

    /**
     *
     */
    private class
            PdfTask
            extends AsyncTask<Void, Void, Void>
    {
        ProgressDialog dialog;

        /**
         *
         */
        @Override
        protected void
        onPreExecute()
        {
            super.onPreExecute();
            dialog = ProgressDialog.show(
                    context,
                    "",
                    context.getResources().getString(R.string.msg_file_generating),
                    true
            );
            dialog.show();
        }

        /**
         * @param
         * @return
         */
        @Override
        protected Void
        doInBackground(Void... params)
        {
            try {
                fillPdfFile();
                fillDocxFile();
            } catch (DocumentException de) {
                // TODO
            } catch (FileNotFoundException fnfe) {
                Log.w("ExternalStorage", String.format("Error writing %s", file), fnfe);
            } catch (IOException e) {
                e.printStackTrace(); // TODO
            }
            return null;
        }

        /**
         * @param result
         */
        @Override
        protected void
        onPostExecute(Void result)
        {
            super.onPostExecute(result);
            dialog.dismiss();
            viewPdfFile();
        }
    }
}
