package com.rem.backend.service;

import com.rem.backend.entity.customer.Customer;
import com.rem.backend.repository.CustomerRepo;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.ValidationService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@AllArgsConstructor
public class CustomerService {

    private final CustomerRepo customerRepo;

    public Map<String, Object> getCustomerById(long id) {
        try {
            ValidationService.validate(id , "id");
            Optional<Customer> customerOptional = customerRepo.findById(id);
            if (customerOptional.isPresent()) {
                return ResponseMapper.buildResponse(Responses.SUCCESS, customerOptional.get());
            }
            return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, null);
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }
}
