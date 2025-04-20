package com.rem.backend.controller;


import com.rem.backend.entity.booking.Booking;
import com.rem.backend.service.BookingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.rem.backend.usermanagement.utillity.JWTUtils.LOGGED_IN_USER;

@RestController
@RequestMapping("/api/booking/")
@AllArgsConstructor
public class BookingController {

    private final BookingService bookingService;


    @PostMapping("/add")
    public Map addBooking(@RequestBody Booking booking, HttpServletRequest request){
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        return bookingService.createBooking(booking, loggedInUser);
    }
}
