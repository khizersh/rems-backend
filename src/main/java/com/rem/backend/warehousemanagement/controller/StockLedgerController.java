package com.rem.backend.warehousemanagement.controller;

import com.rem.backend.dto.commonRequest.FilterPaginationRequest;
import com.rem.backend.enums.StockRefType;
import com.rem.backend.warehousemanagement.service.StockLedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/stock-ledger")
@RequiredArgsConstructor
public class StockLedgerController {

    private final StockLedgerService stockLedgerService;

    @PostMapping("/getByWarehouse")
    public ResponseEntity<?> getLedgerByWarehouse(@RequestBody FilterPaginationRequest request) {
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                request.getSortDir().equalsIgnoreCase("asc")
                        ? Sort.by(request.getSortBy()).ascending()
                        : Sort.by(request.getSortBy()).descending());

        Map<String, Object> ledgerPage = stockLedgerService.getLedgerByWarehouse(request.getId(), pageable);
        return ResponseEntity.ok(ledgerPage);
    }

    @PostMapping("/getByItem")
    public ResponseEntity<?> getLedgerByItem(@RequestBody FilterPaginationRequest request) {
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                request.getSortDir().equalsIgnoreCase("asc")
                        ? Sort.by(request.getSortBy()).ascending()
                        : Sort.by(request.getSortBy()).descending());

        Map<String, Object> ledgerPage = stockLedgerService.getLedgerByItem(request.getId(), pageable);
        return ResponseEntity.ok(ledgerPage);
    }

    @PostMapping("/getByWarehouseAndItem")
    public ResponseEntity<?> getLedgerByWarehouseAndItem(@RequestBody FilterPaginationRequest request) {
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                request.getSortDir().equalsIgnoreCase("asc")
                        ? Sort.by(request.getSortBy()).ascending()
                        : Sort.by(request.getSortBy()).descending());

        Map<String, Object> ledgerPage = stockLedgerService.getLedgerByWarehouseAndItem(request.getId(), request.getId2(), pageable);
        return ResponseEntity.ok(ledgerPage);
    }

    @GetMapping("/reference/{refType}/{refId}")
    public ResponseEntity<?> getLedgerByReference(
            @PathVariable StockRefType refType,
            @PathVariable Long refId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "txnDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                sortDir.equalsIgnoreCase("asc")
                        ? Sort.by(sortBy).ascending()
                        : Sort.by(sortBy).descending());

        Map<String, Object> ledgerPage = stockLedgerService.getLedgerByReference(refType, refId, pageable);
        return ResponseEntity.ok(ledgerPage);
    }

    @GetMapping("/reference-type/{refType}")
    public ResponseEntity<?> getLedgerByRefType(
            @PathVariable StockRefType refType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "txnDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                sortDir.equalsIgnoreCase("asc")
                        ? Sort.by(sortBy).ascending()
                        : Sort.by(sortBy).descending());

        Map<String, Object> ledgerPage = stockLedgerService.getLedgerByRefType(refType, pageable);
        return ResponseEntity.ok(ledgerPage);
    }

    @GetMapping("/warehouse/{warehouseId}/date-range")
    public ResponseEntity<?> getLedgerByWarehouseAndDateRange(
            @PathVariable Long warehouseId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "txnDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                sortDir.equalsIgnoreCase("asc")
                        ? Sort.by(sortBy).ascending()
                        : Sort.by(sortBy).descending());

        Map<String, Object> ledgerPage = stockLedgerService.getLedgerByWarehouseAndDateRange(warehouseId, startDate, endDate, pageable);
        return ResponseEntity.ok(ledgerPage);
    }

    @GetMapping("/item/{itemId}/date-range")
    public ResponseEntity<?> getLedgerByItemAndDateRange(
            @PathVariable Long itemId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "txnDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                sortDir.equalsIgnoreCase("asc")
                        ? Sort.by(sortBy).ascending()
                        : Sort.by(sortBy).descending());

        Map<String, Object> ledgerPage = stockLedgerService.getLedgerByItemAndDateRange(itemId, startDate, endDate, pageable);
        return ResponseEntity.ok(ledgerPage);
    }

    @GetMapping("/reference-list/{refType}/{refId}")
    public Map<String, Object> getLedgerByReferenceList(@PathVariable StockRefType refType,
                                                        @PathVariable Long refId) {
        return stockLedgerService.getLedgerByReferenceList(refType, refId);
    }
}
