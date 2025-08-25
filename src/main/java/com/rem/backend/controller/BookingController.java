package com.rem.backend.controller;


import com.rem.backend.dto.commonRequest.FilterPaginationRequest;
import com.rem.backend.entity.booking.Booking;
import com.rem.backend.service.BookingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
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


    @GetMapping("/getDetailById/{id}")
    public Map getDetailsForPrintBooking(@PathVariable long id){
        return bookingService.getDetailsById(id);
    }


    @PostMapping("/getByIds")
    public ResponseEntity<?> getProjectsByIds(@RequestBody FilterPaginationRequest request) {
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                request.getSortDir().equalsIgnoreCase("asc")
                        ? Sort.by(request.getSortBy()).ascending()
                        : Sort.by(request.getSortBy()).descending());

        Map<String , Object> projectPage = bookingService.getBookingsByIds(request.getId(), request.getFilteredBy(), pageable);
        return ResponseEntity.ok(projectPage);
    }
}
