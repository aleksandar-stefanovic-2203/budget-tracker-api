package com.aleksandarstefanovic.budgettracker.services;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.aleksandarstefanovic.budgettracker.db.dao.ErrorCode;
import com.aleksandarstefanovic.budgettracker.db.dao.repo.TransferRepo;
import com.aleksandarstefanovic.budgettracker.db.dao.validation.TransferValidator;
import com.aleksandarstefanovic.budgettracker.models.Transfer;

@Service
public class TransferCsvImportService {
    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024;
    private static final List<String> REQUIRED_HEADERS = List.of("type", "amount", "date", "categoryid");

    private final TransferRepo transferRepo;

    public TransferCsvImportService(TransferRepo transferRepo) {
        this.transferRepo = transferRepo;
    }

    public record RowError(int row, String code, String message) {}

    public record ImportReport(int totalRows, int importedRows, int failedRows, List<RowError> errors) {}

    public static final class InvalidCsvException extends Exception {
        private final ErrorCode errorCode;

        public InvalidCsvException(ErrorCode errorCode) {
            super(errorCode.getDescription());
            this.errorCode = errorCode;
        }

        public ErrorCode getErrorCode() {
            return errorCode;
        }
    }

    private record CsvRecord(int startLine, List<String> fields) {}

    public ImportReport importTransactions(MultipartFile file) throws IOException, InvalidCsvException {
        if (file == null || file.isEmpty()) {
            throw new InvalidCsvException(ErrorCode.TRANSACTION_CSV_FILE_INVALID);
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new InvalidCsvException(ErrorCode.TRANSACTION_CSV_FILE_TOO_LARGE);
        }

        String csv = decodeUtf8(file.getBytes());
        List<CsvRecord> records = parseCsv(csv);
        if (records.isEmpty()) {
            throw new InvalidCsvException(ErrorCode.TRANSACTION_CSV_FILE_INVALID);
        }

        Map<String, Integer> headerIndexes = validateHeader(records.get(0));
        int totalRows = 0;
        int importedRows = 0;
        List<RowError> errors = new ArrayList<>();

        for (int index = 1; index < records.size(); index++) {
            CsvRecord record = records.get(index);
            if (isBlank(record.fields())) continue;

            totalRows++;
            ErrorCode rowError;
            if (record.fields().size() != headerIndexes.size()) {
                rowError = ErrorCode.TRANSACTION_CSV_ROW_INVALID;
            } else {
                try {
                    Transfer transfer = transferFrom(record.fields(), headerIndexes);
                    rowError = validateTransfer(transfer);
                    if (rowError == ErrorCode.OK) {
                        rowError = transferRepo.insertTransfer(transfer);
                    }
                } catch (RowConversionException exception) {
                    rowError = exception.errorCode;
                }
            }

            if (rowError == ErrorCode.OK) {
                importedRows++;
            } else {
                errors.add(new RowError(record.startLine(), rowError.name(), rowError.getDescription()));
            }
        }

        return new ImportReport(totalRows, importedRows, errors.size(), List.copyOf(errors));
    }

    private static String decodeUtf8(byte[] bytes) throws InvalidCsvException {
        try {
            String content = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
            return content.startsWith("\uFEFF") ? content.substring(1) : content;
        } catch (CharacterCodingException exception) {
            throw new InvalidCsvException(ErrorCode.TRANSACTION_CSV_FILE_INVALID);
        }
    }

    private static List<CsvRecord> parseCsv(String csv) throws InvalidCsvException {
        List<CsvRecord> records = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        int line = 1;
        int recordStartLine = 1;
        boolean insideQuotes = false;
        boolean afterClosingQuote = false;

        for (int index = 0; index < csv.length(); index++) {
            char current = csv.charAt(index);

            if (insideQuotes) {
                if (current == '"') {
                    if (index + 1 < csv.length() && csv.charAt(index + 1) == '"') {
                        field.append('"');
                        index++;
                    } else {
                        insideQuotes = false;
                        afterClosingQuote = true;
                    }
                } else {
                    field.append(current);
                    if (current == '\n') line++;
                    else if (current == '\r' && (index + 1 >= csv.length() || csv.charAt(index + 1) != '\n')) {
                        line++;
                    }
                }
                continue;
            }

            if (afterClosingQuote) {
                if (current == ',') {
                    addField(fields, field);
                    afterClosingQuote = false;
                } else if (current == '\r' || current == '\n') {
                    addField(fields, field);
                    addRecord(records, fields, recordStartLine);
                    afterClosingQuote = false;
                    if (current == '\r' && index + 1 < csv.length() && csv.charAt(index + 1) == '\n') index++;
                    line++;
                    recordStartLine = line;
                } else if (current != ' ' && current != '\t') {
                    throw new InvalidCsvException(ErrorCode.TRANSACTION_CSV_FILE_INVALID);
                }
                continue;
            }

            if (current == '"') {
                if (field.length() != 0) {
                    throw new InvalidCsvException(ErrorCode.TRANSACTION_CSV_FILE_INVALID);
                }
                insideQuotes = true;
            } else if (current == ',') {
                addField(fields, field);
            } else if (current == '\r' || current == '\n') {
                addField(fields, field);
                addRecord(records, fields, recordStartLine);
                if (current == '\r' && index + 1 < csv.length() && csv.charAt(index + 1) == '\n') index++;
                line++;
                recordStartLine = line;
            } else {
                field.append(current);
            }
        }

        if (insideQuotes) {
            throw new InvalidCsvException(ErrorCode.TRANSACTION_CSV_FILE_INVALID);
        }
        if (afterClosingQuote || field.length() > 0 || !fields.isEmpty()) {
            addField(fields, field);
            addRecord(records, fields, recordStartLine);
        }
        return records;
    }

    private static void addField(List<String> fields, StringBuilder field) {
        fields.add(field.toString());
        field.setLength(0);
    }

    private static void addRecord(List<CsvRecord> records, List<String> fields, int startLine) {
        records.add(new CsvRecord(startLine, List.copyOf(fields)));
        fields.clear();
    }

    private static Map<String, Integer> validateHeader(CsvRecord header) throws InvalidCsvException {
        Map<String, Integer> indexes = new HashMap<>();
        for (int index = 0; index < header.fields().size(); index++) {
            String name = header.fields().get(index).strip().toLowerCase(Locale.ROOT);
            if (name.isEmpty() || indexes.putIfAbsent(name, index) != null) {
                throw new InvalidCsvException(ErrorCode.TRANSACTION_CSV_FILE_INVALID);
            }
        }
        if (!indexes.keySet().containsAll(REQUIRED_HEADERS)
                || indexes.size() != header.fields().size()
                || indexes.keySet().stream().anyMatch(
                        name -> !REQUIRED_HEADERS.contains(name) && !name.equals("description"))) {
            throw new InvalidCsvException(ErrorCode.TRANSACTION_CSV_FILE_INVALID);
        }
        return indexes;
    }

    private static boolean isBlank(List<String> fields) {
        return fields.size() == 1 && fields.get(0).isBlank();
    }

    private static Transfer transferFrom(List<String> fields, Map<String, Integer> indexes)
            throws RowConversionException {
        String type = value(fields, indexes, "type");
        String amountText = value(fields, indexes, "amount");
        String dateText = value(fields, indexes, "date");
        String description = indexes.containsKey("description")
                ? fields.get(indexes.get("description"))
                : null;
        String categoryText = value(fields, indexes, "categoryid");

        Double amount = null;
        if (amountText != null) {
            try {
                amount = Double.valueOf(amountText);
            } catch (NumberFormatException exception) {
                throw new RowConversionException(ErrorCode.TRANSACTION_AMOUNT_INVALID_FORMAT);
            }
        }

        Date date = null;
        if (dateText != null) {
            try {
                date = Date.valueOf(LocalDate.parse(dateText));
            } catch (DateTimeParseException exception) {
                throw new RowConversionException(ErrorCode.TRANSACTION_DATE_INVALID_FORMAT);
            }
        }

        Integer categoryId = null;
        if (categoryText != null) {
            try {
                categoryId = Integer.valueOf(categoryText);
            } catch (NumberFormatException exception) {
                throw new RowConversionException(ErrorCode.TRANSACTION_CATEGORY_ID_INVALID_FORMAT);
            }
        }

        if (description != null && description.isBlank()) description = null;
        return new Transfer(null, type, amount, date, description, categoryId);
    }

    private static String value(List<String> fields, Map<String, Integer> indexes, String name) {
        Integer index = indexes.get(name);
        if (index == null) return null;
        String value = fields.get(index).strip();
        return value.isEmpty() ? null : value;
    }

    private static ErrorCode validateTransfer(Transfer transfer) {
        ErrorCode errorCode = TransferValidator.isTypeValid(transfer.getType());
        if (errorCode != ErrorCode.OK) return errorCode;
        errorCode = TransferValidator.isAmountValid(transfer.getAmount());
        if (errorCode != ErrorCode.OK) return errorCode;
        errorCode = TransferValidator.isDateValid(transfer.getDate());
        if (errorCode != ErrorCode.OK) return errorCode;
        errorCode = TransferValidator.isDescriptionValid(transfer.getDescription());
        if (errorCode != ErrorCode.OK) return errorCode;
        return TransferValidator.isCategoryIDValid(transfer.getCategoryId());
    }

    private static final class RowConversionException extends Exception {
        private final ErrorCode errorCode;

        private RowConversionException(ErrorCode errorCode) {
            this.errorCode = errorCode;
        }
    }
}
