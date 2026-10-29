package com.aleksandarstefanovic.budgettracker.controllers;

import java.io.IOException;
import java.sql.Date;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

import com.aleksandarstefanovic.budgettracker.db.dao.ErrorCode;
import com.aleksandarstefanovic.budgettracker.db.dao.repo.TransferRepo;
import com.aleksandarstefanovic.budgettracker.db.dao.repo.TransferRepo.Result;
import com.aleksandarstefanovic.budgettracker.db.dao.repo.TransferRepo.MonthlySummaryResult;
import com.aleksandarstefanovic.budgettracker.db.dao.repo.TransferRepo.SingleResult;
import com.aleksandarstefanovic.budgettracker.models.Transfer;
import com.aleksandarstefanovic.budgettracker.services.TransferCsvImportService;
import com.aleksandarstefanovic.budgettracker.services.TransferCsvImportService.ImportReport;
import com.aleksandarstefanovic.budgettracker.services.TransferCsvImportService.InvalidCsvException;

@RestController
@RequestMapping("/transactions")
@CrossOrigin(origins = "http://localhost:8080/")
public class TransferController {
    private final TransferRepo transferRepo;
    private final TransferCsvImportService csvImportService;

    public TransferController(TransferRepo transferRepo, TransferCsvImportService csvImportService) {
        this.transferRepo = transferRepo;
        this.csvImportService = csvImportService;
    }

    @PostMapping("/insertTransaction")
    public ResponseEntity<String> insertTransaction(@RequestBody Transfer transfer) {
        return APIResponse.fromErrorCode(transferRepo.insertTransfer(transfer));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> importTransactions(@RequestPart("file") MultipartFile file) throws IOException {
        try {
            ImportReport report = csvImportService.importTransactions(file);
            return ResponseEntity.ok(report);
        } catch (InvalidCsvException exception) {
            return ResponseEntity.status(exception.getErrorCode().getStatus())
                    .body(new CsvImportError(exception.getErrorCode().name(), exception.getMessage()));
        }
    }

    @PutMapping("/updateTransaction")
    public ResponseEntity<String> updateTransaction(@RequestBody Transfer transfer) {
        return APIResponse.fromErrorCode(transferRepo.updateTransfer(transfer));
    }

    @DeleteMapping("/deleteTransaction/{id}")
    public ResponseEntity<String> deleteTransaction(@PathVariable Integer id) {
        return APIResponse.fromErrorCode(transferRepo.deleteTransfer(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTransactionByID(@PathVariable Integer id) {
        SingleResult result = transferRepo.getTransferByID(id);
        if (result.errCode() != ErrorCode.OK) {
            return ResponseEntity.status(result.errCode().getStatus()).body(result.errCode().getDescription());
        }
        return ResponseEntity.ok(result.transfer());
    }

    @GetMapping("/monthly-summary")
    public ResponseEntity<?> getMonthlySummary(@RequestParam Date month) {
        MonthlySummaryResult result = transferRepo.getMonthlySummary(month);
        if (result.errCode() != ErrorCode.OK) {
            return ResponseEntity.status(result.errCode().getStatus()).body(result.errCode().getDescription());
        }
        return ResponseEntity.ok(result.summary());
    }

    @GetMapping
    public ResponseEntity<?> getTransactions(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Date startDate,
            @RequestParam(required = false) Date endDate,
            @RequestParam(required = false) String categoryName,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String description) {
        Result result = transferRepo.getTransfers(page, pageSize, startDate, endDate, categoryName, type, description);
        if (result.errCode() != ErrorCode.OK) {
            return ResponseEntity.status(result.errCode().getStatus()).body(result.errCode().getDescription());
        }
        List<Transfer> transfers = result.list();
        return ResponseEntity.ok(transfers);
    }

    public record CsvImportError(String code, String message) {}
}
