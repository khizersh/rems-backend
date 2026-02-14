package com.rem.backend.warehousemanagement.service;

import com.rem.backend.warehousemanagement.entity.StockLedger;
import com.rem.backend.enums.StockRefType;
import com.rem.backend.warehousemanagement.repo.StockLedgerRepository;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.ValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StockLedgerService {

    private final StockLedgerRepository stockLedgerRepository;

    public Map<String, Object> getLedgerByWarehouse(Long warehouseId, Pageable pageable) {
        try {
            ValidationService.validate(warehouseId, "Warehouse ID");

            Page<StockLedger> ledgers = stockLedgerRepository.findByWarehouseIdOrderByTxnDateDesc(warehouseId, pageable);

            return ResponseMapper.buildResponse(Responses.SUCCESS, ledgers);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getLedgerByItem(Long itemId, Pageable pageable) {
        try {
            ValidationService.validate(itemId, "Item ID");

            Page<StockLedger> ledgers = stockLedgerRepository.findByItemIdOrderByTxnDateDesc(itemId, pageable);

            return ResponseMapper.buildResponse(Responses.SUCCESS, ledgers);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getLedgerByWarehouseAndItem(Long warehouseId, Long itemId, Pageable pageable) {
        try {
            ValidationService.validate(warehouseId, "Warehouse ID");
            ValidationService.validate(itemId, "Item ID");

            Page<StockLedger> ledgers = stockLedgerRepository.findByWarehouseIdAndItemIdOrderByTxnDateDesc(
                warehouseId, itemId, pageable);

            return ResponseMapper.buildResponse(Responses.SUCCESS, ledgers);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getLedgerByReference(StockRefType refType, Long refId, Pageable pageable) {
        try {
            ValidationService.validate(refType, "Reference Type");
            ValidationService.validate(refId, "Reference ID");

            Page<StockLedger> ledgers = stockLedgerRepository.findByRefTypeAndRefId(refType, refId, pageable);

            return ResponseMapper.buildResponse(Responses.SUCCESS, ledgers);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getLedgerByRefType(StockRefType refType, Pageable pageable) {
        try {
            ValidationService.validate(refType, "Reference Type");

            Page<StockLedger> ledgers = stockLedgerRepository.findByRefType(refType, pageable);

            return ResponseMapper.buildResponse(Responses.SUCCESS, ledgers);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getLedgerByWarehouseAndDateRange(Long warehouseId, LocalDateTime startDate,
                                                               LocalDateTime endDate, Pageable pageable) {
        try {
            ValidationService.validate(warehouseId, "Warehouse ID");
            ValidationService.validate(startDate, "Start Date");
            ValidationService.validate(endDate, "End Date");

            if (startDate.isAfter(endDate)) {
                throw new IllegalArgumentException("Start date cannot be after end date");
            }

            Page<StockLedger> ledgers = stockLedgerRepository.findByWarehouseAndDateRange(
                warehouseId, startDate, endDate, pageable);

            return ResponseMapper.buildResponse(Responses.SUCCESS, ledgers);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getLedgerByItemAndDateRange(Long itemId, LocalDateTime startDate,
                                                          LocalDateTime endDate, Pageable pageable) {
        try {
            ValidationService.validate(itemId, "Item ID");
            ValidationService.validate(startDate, "Start Date");
            ValidationService.validate(endDate, "End Date");

            if (startDate.isAfter(endDate)) {
                throw new IllegalArgumentException("Start date cannot be after end date");
            }

            Page<StockLedger> ledgers = stockLedgerRepository.findByItemAndDateRange(
                itemId, startDate, endDate, pageable);

            return ResponseMapper.buildResponse(Responses.SUCCESS, ledgers);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getLedgerByReferenceList(StockRefType refType, Long refId) {
        try {
            ValidationService.validate(refType, "Reference Type");
            ValidationService.validate(refId, "Reference ID");

            List<StockLedger> ledgers = stockLedgerRepository.findByRefTypeAndRefId(refType, refId);

            return ResponseMapper.buildResponse(Responses.SUCCESS, ledgers);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }
}
