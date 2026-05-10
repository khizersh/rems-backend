package com.rem.backend.propertymanagement.service;

import com.rem.backend.propertymanagement.entity.PropertyAsset;
import com.rem.backend.propertymanagement.enums.PropertyAssetStatus;
import com.rem.backend.propertymanagement.repository.PropertyAssetRepo;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.ValidationService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@AllArgsConstructor
public class PropertyAssetService {

    private final PropertyAssetRepo propertyAssetRepo;

    public Map<String, Object> getAssetById(long assetId) {
        try {
            ValidationService.validate(assetId, "propertyAssetId");
            Optional<PropertyAsset> assetOpt = propertyAssetRepo.findById(assetId);
            if (assetOpt.isEmpty()) {
                return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, null);
            }
            return ResponseMapper.buildResponse(Responses.SUCCESS, assetOpt.get());
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public boolean isAssetAvailable(long assetId) {
        return propertyAssetRepo.existsByIdAndStatus(assetId, PropertyAssetStatus.AVAILABLE);
    }
}

