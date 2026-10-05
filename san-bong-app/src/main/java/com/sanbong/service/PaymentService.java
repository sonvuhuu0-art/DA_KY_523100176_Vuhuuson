package com.sanbong.service;

import com.sanbong.dao.BookingDao;
import com.sanbong.dao.PaymentDao;
import com.sanbong.model.Payment;

import java.math.BigDecimal;

public class PaymentService {

    private final PaymentDao paymentDao = new PaymentDao();
    private final BookingDao bookingDao = new BookingDao();

    /** Records a payment taken at the counter (cash or bank transfer) and marks the booking as paid. */
    public void recordPayment(int bookingId, BigDecimal amount, String method) {
        Payment payment = new Payment(bookingId, amount, method);
        payment.setStatus("success");
        paymentDao.insert(payment);
        bookingDao.updatePaymentStatus(bookingId, "paid");
    }
}
