package com.sanbong.service;

import com.sanbong.dao.BookingDao;
import com.sanbong.dao.TimeSlotDao;
import com.sanbong.exception.SlotAlreadyBookedException;
import com.sanbong.model.Booking;
import com.sanbong.model.TimeSlot;
import com.sanbong.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public class BookingService {

    private final BookingDao bookingDao = new BookingDao();
    private final TimeSlotDao timeSlotDao = new TimeSlotDao();

    public List<TimeSlot> getAvailableSlots(int fieldId, LocalDate date) {
        return timeSlotDao.findAvailableSlots(fieldId, date);
    }

    /**
     * Checks for a clash and inserts the booking inside a single JDBC
     * transaction so two simultaneous requests for the same slot cannot
     * both succeed (classic race condition on a "check then insert").
     */
    public Booking createBooking(int userId, int fieldId, int timeSlotId, LocalDate date, String note)
            throws SlotAlreadyBookedException {
        Optional<TimeSlot> slotOpt = timeSlotDao.findById(timeSlotId);
        if (slotOpt.isEmpty()) {
            throw new IllegalArgumentException("Khung giờ không tồn tại.");
        }
        TimeSlot slot = slotOpt.get();

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (bookingDao.existsActiveBooking(conn, fieldId, timeSlotId, date)) {
                    conn.rollback();
                    throw new SlotAlreadyBookedException(
                            "Khung giờ " + slot.getLabel() + " ngày " + date + " đã có người đặt.");
                }

                Booking booking = new Booking();
                booking.setUserId(userId);
                booking.setFieldId(fieldId);
                booking.setTimeSlotId(timeSlotId);
                booking.setBookingDate(date);
                booking.setTotalPrice(slot.getPrice());
                booking.setStatus("pending");
                booking.setPaymentStatus("unpaid");
                booking.setNote(note);

                int id = bookingDao.insert(conn, booking);
                conn.commit();
                booking.setId(id);
                return booking;
            } catch (SlotAlreadyBookedException e) {
                throw e;
            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Không thể tạo lịch đặt sân.", e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi kết nối cơ sở dữ liệu.", e);
        }
    }

    public List<Booking> getMyBookings(int userId) {
        bookingDao.completePastConfirmedBookings();
        return bookingDao.findByUserId(userId);
    }

    public List<Booking> getAllBookings() {
        return getAllBookings(null);
    }

    /** Lists bookings, optionally scoped to a single venue owner (null = all, used by admin). */
    public List<Booking> getAllBookings(Integer ownerId) {
        bookingDao.completePastConfirmedBookings();
        return bookingDao.findAll(ownerId);
    }

    /** Cancels a booking; only allowed while at least 2 hours remain before the slot starts. */
    public void cancelBooking(int bookingId) {
        Booking booking = bookingDao.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lịch đặt."));

        if ("cancelled".equals(booking.getStatus())) {
            throw new IllegalStateException("Lịch đặt này đã bị hủy trước đó.");
        }

        LocalTime slotStart = LocalTime.parse(booking.getSlotLabel().split(" - ")[0]);
        LocalDateTime slotStartDateTime = LocalDateTime.of(booking.getBookingDate(), slotStart);
        if (LocalDateTime.now().plusHours(2).isAfter(slotStartDateTime)) {
            throw new IllegalStateException("Chỉ có thể hủy lịch trước giờ đặt tối thiểu 2 tiếng.");
        }

        bookingDao.updateStatus(bookingId, "cancelled");
    }

    public void confirmBooking(int bookingId) {
        bookingDao.updateStatus(bookingId, "confirmed");
    }

    public void adminCancelBooking(int bookingId) {
        bookingDao.updateStatus(bookingId, "cancelled");
    }

    public void markCompleted(int bookingId) {
        bookingDao.updateStatus(bookingId, "completed");
    }
}
