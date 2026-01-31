package com.rem.backend.dto.expense;

import com.rem.backend.enums.ExpenseType;
import lombok.Data;


@Data
public class ExpenseFetchRequestDTO {

    private long id;
    private long id2;
    private String filteredBy;
    private ExpenseType expenseType;
    private Long accountGroupId;
    private Long coaId;
    private int page = 0;
    private int size = 10;
    private String sortBy = "createdDate";
    private String sortDir = "asc";

}
