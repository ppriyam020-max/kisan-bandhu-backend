package com.procurement.controller;

import com.procurement.dto.BookingRequest;
import com.procurement.entity.Booking;
import com.procurement.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(
            BookingService bookingService
    ) {
        this.bookingService = bookingService;
    }

    // =====================================================
    // CREATE BOOKING
    // =====================================================
    @PostMapping
    public Object createBooking(
            Authentication authentication,
            @Valid @RequestBody BookingRequest request
    ) {

        String phone = getUsername(authentication);

        System.out.println(
                "Booking username from JWT: " + phone
        );

        return bookingService.create(
                phone,
                request
        );
    }

    // =====================================================
    // GET MY BOOKINGS
    // =====================================================
    @GetMapping("/my")
    public List<Booking> getMyBookings(
            Authentication authentication
    ) {

        String phone = getUsername(authentication);

        return bookingService.mine(phone);
    }

    // =====================================================
    // GET BOOKING BY ID
    // =====================================================
    @GetMapping("/{id}")
    public Booking getBookingById(
            Authentication authentication,
            @PathVariable Long id
    ) {

        String phone = getUsername(authentication);

        return bookingService.getById(
                phone,
                id
        );
    }

    // =====================================================
    // CANCEL BOOKING
    // =====================================================
    @PutMapping("/{id}/cancel")
    public Booking cancelBooking(
            Authentication authentication,
            @PathVariable Long id
    ) {

        String phone = getUsername(authentication);

        return bookingService.cancel(
                phone,
                id
        );
    }

    // =====================================================
    // AUTHENTICATION CHECK
    // =====================================================
    private String getUsername(
            Authentication authentication
    ) {

        if (authentication == null
                || !authentication.isAuthenticated()) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Please login first"
            );
        }

        String username =
                authentication.getName();

        if (username == null
                || username.trim().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "User information not found in token"
            );
        }

        return username.trim();
    }
}