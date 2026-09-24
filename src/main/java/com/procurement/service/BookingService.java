package com.procurement.service;

import com.procurement.dto.BookingRequest;
import com.procurement.entity.Booking;
import com.procurement.entity.BookingStatus;
import com.procurement.entity.Farmer;
import com.procurement.entity.NotificationType;
import com.procurement.entity.ProcurementSchedule;
import com.procurement.entity.ScheduleStatus;
import com.procurement.entity.User;
import com.procurement.entity.WaitingListEntry;
import com.procurement.entity.WaitingStatus;
import com.procurement.repository.BookingRepository;
import com.procurement.repository.FarmerRepository;
import com.procurement.repository.ScheduleRepository;
import com.procurement.repository.UserRepository;
import com.procurement.repository.WaitingListRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class BookingService {

 private final UserRepository users;
 private final FarmerRepository farmers;
 private final ScheduleRepository schedules;
 private final BookingRepository bookings;
 private final WaitingListRepository waiting;
 private final NotificationService notifications;

 public BookingService(
         UserRepository users,
         FarmerRepository farmers,
         ScheduleRepository schedules,
         BookingRepository bookings,
         WaitingListRepository waiting,
         NotificationService notifications
 ) {
  this.users = users;
  this.farmers = farmers;
  this.schedules = schedules;
  this.bookings = bookings;
  this.waiting = waiting;
  this.notifications = notifications;
 }

 // =====================================================
 // FIND FARMER USING LOGGED-IN USER PHONE
 // =====================================================
 private Farmer farmer(String phone) {

  User user = users.findByPhoneNumber(phone)
          .orElseThrow(() ->
                  new NoSuchElementException(
                          "User not found with phone: " + phone
                  )
          );

  return farmers.findByUserId(user.getId())
          .orElseThrow(() ->
                  new NoSuchElementException(
                          "Farmer profile not found for user ID: "
                                  + user.getId()
                  )
          );
 }

 // =====================================================
 // CREATE BOOKING
 // =====================================================
 @Transactional
 public Object create(
         String phone,
         BookingRequest request
 ) {

  Farmer farmer = farmer(phone);

  ProcurementSchedule schedule =
          schedules.findByIdForUpdate(
                  request.getScheduleId()
          ).orElseThrow(() ->
                  new NoSuchElementException(
                          "Schedule not found with ID: "
                                  + request.getScheduleId()
                  )
          );

  // Check schedule status
  if (schedule.getStatus() != ScheduleStatus.OPEN) {

   throw new IllegalArgumentException(
           "Schedule is not open"
   );
  }

  // Check duplicate booking
  boolean alreadyBooked =
          bookings.existsByFarmerIdAndScheduleIdAndStatusIn(
                  farmer.getId(),
                  schedule.getId(),
                  List.of(
                          BookingStatus.PENDING,
                          BookingStatus.CONFIRMED
                  )
          );

  if (alreadyBooked) {

   throw new IllegalArgumentException(
           "You already have a booking for this schedule"
   );
  }

  // Calculate available capacity
  BigDecimal availableCapacity =
          schedule.getMaxCapacityQuintal()
                  .subtract(
                          schedule.getBookedCapacityQuintal()
                  );

  // =================================================
  // CAPACITY AVAILABLE: CONFIRM BOOKING
  // =================================================
  if (availableCapacity.compareTo(
          request.getQuantityQuintal()
  ) >= 0) {

   Booking booking = new Booking();

   booking.setFarmer(farmer);
   booking.setSchedule(schedule);
   booking.setQuantityQuintal(
           request.getQuantityQuintal()
   );
   booking.setStatus(BookingStatus.CONFIRMED);

   Booking savedBooking =
           bookings.saveAndFlush(booking);

   notifications.create(
           farmer,
           savedBooking,
           NotificationType.BOOKING_CONFIRMED,
           "Your procurement booking is confirmed"
   );

   return savedBooking;
  }

  // =================================================
  // CAPACITY NOT AVAILABLE: WAITING LIST
  // =================================================
  WaitingListEntry waitingEntry =
          new WaitingListEntry();

  waitingEntry.setFarmer(farmer);
  waitingEntry.setSchedule(schedule);
  waitingEntry.setQuantityQuintal(
          request.getQuantityQuintal()
  );

  int queuePosition = Math.toIntExact(
          waiting.countByScheduleIdAndStatus(
                  schedule.getId(),
                  WaitingStatus.WAITING
          ) + 1
  );

  waitingEntry.setQueuePosition(queuePosition);

  waitingEntry.setStatus(
          WaitingStatus.WAITING
  );

  return waiting.save(waitingEntry);
 }

 // =====================================================
 // CANCEL BOOKING
 // =====================================================
 @Transactional
 public Booking cancel(
         String phone,
         Long bookingId
 ) {

  Farmer farmer = farmer(phone);

  Booking booking =
          bookings.findById(bookingId)
                  .orElseThrow(() ->
                          new NoSuchElementException(
                                  "Booking not found with ID: "
                                          + bookingId
                          )
                  );

  // Security check
  if (!booking.getFarmer().getId()
          .equals(farmer.getId())) {

   throw new IllegalArgumentException(
           "Not your booking"
   );
  }

  // Check booking status
  if (!(booking.getStatus() == BookingStatus.PENDING
          || booking.getStatus()
          == BookingStatus.CONFIRMED)) {

   throw new IllegalArgumentException(
           "Booking cannot be cancelled now"
   );
  }

  booking.setStatus(
          BookingStatus.CANCELLED
  );

  Booking savedBooking =
          bookings.saveAndFlush(booking);

  notifications.create(
          farmer,
          savedBooking,
          NotificationType.BOOKING_CANCELLED,
          "Your booking has been cancelled"
  );

  return savedBooking;
 }

 // =====================================================
 // GET MY BOOKINGS
 // =====================================================
 public List<Booking> mine(String phone) {

  Farmer farmer = farmer(phone);

  return bookings.findByFarmerIdOrderByBookedAtDesc(
          farmer.getId()
  );
 }

 // =====================================================
 // GET BOOKING BY ID
 // =====================================================
 @Transactional(readOnly = true)
 public Booking getById(
         String phone,
         Long bookingId
 ) {

  Farmer farmer = farmer(phone);

  Booking booking =
          bookings.findById(bookingId)
                  .orElseThrow(() ->
                          new NoSuchElementException(
                                  "Booking not found with ID: "
                                          + bookingId
                          )
                  );

  // Security check
  if (!booking.getFarmer().getId()
          .equals(farmer.getId())) {

   throw new IllegalArgumentException(
           "Not your booking"
   );
  }

  return booking;
 }

 // =====================================================
 // GET BOOKINGS BY SCHEDULE
 // =====================================================
 @Transactional(readOnly = true)
 public List<Booking> scheduleBookings(
         Long scheduleId
 ) {

  return bookings.findByScheduleIdOrderByBookedAtAsc(
          scheduleId
  );
 }
}