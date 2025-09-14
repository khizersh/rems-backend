package com.rem.backend.utility;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Utility {

    public static final String RESPONSE_CODE = "responseCode";
    public static final String RESPONSE_MESSAGE = "responseMessage";
    public static final String DATA = "data";
    public static final String COMPANY_NAME = "COMPANY_NAME";
    public static final long ADMIN_ROLE_ID = 1;
    public static final long USER_ROLE_ID = 2;


    public static LocalDateTime getDateInLastDays(int days) {
        return LocalDateTime.now().minusDays(days);
    }

    public static LocalDateTime getLocalDateTimeByString(String input) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d-M-yyyy");
        LocalDate date = LocalDate.parse(input, formatter);
        return date.atStartOfDay();
    }


}
