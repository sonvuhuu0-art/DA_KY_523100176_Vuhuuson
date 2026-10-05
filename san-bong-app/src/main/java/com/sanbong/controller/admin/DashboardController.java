package com.sanbong.controller.admin;

import com.sanbong.dao.BookingDao;
import com.sanbong.dao.TimeSlotDao;
import com.sanbong.model.Booking;
import com.sanbong.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DashboardController {

    @FXML
    private Label revenueLabel;
    @FXML
    private Label todayBookingsLabel;
    @FXML
    private Label occupancyLabel;
    @FXML
    private PieChart statusPieChart;
    @FXML
    private BarChart<String, Number> fieldBarChart;
    @FXML
    private LineChart<String, Number> revenueTrendChart;

    private final BookingDao bookingDao = new BookingDao();
    private final TimeSlotDao timeSlotDao = new TimeSlotDao();

    @FXML
    private void initialize() {
        LocalDate today = LocalDate.now();
        Integer ownerScope = SessionManager.ownerScope();

        var revenue = bookingDao.sumRevenueForMonth(today.getYear(), today.getMonthValue(), ownerScope);
        revenueLabel.setText(String.format("%,.0f đ", revenue));

        int todayBookings = bookingDao.countBookingsOnDate(today, ownerScope);
        todayBookingsLabel.setText(String.valueOf(todayBookings));

        String dayType = (today.getDayOfWeek() == DayOfWeek.SATURDAY || today.getDayOfWeek() == DayOfWeek.SUNDAY)
                ? "weekend" : "weekday";
        int totalSlots = timeSlotDao.countActiveSlotsForDayType(dayType, ownerScope);
        double occupancy = totalSlots == 0 ? 0 : (todayBookings * 100.0 / totalSlots);
        occupancyLabel.setText(String.format("%.0f%%", occupancy));

        List<Booking> allBookings = bookingDao.findAll(ownerScope);
        buildStatusPieChart(allBookings);
        buildFieldBarChart(allBookings);
        buildRevenueTrendChart(today, ownerScope);
    }

    private void buildRevenueTrendChart(LocalDate today, Integer ownerScope) {
        Map<Integer, BigDecimal> revenueByDay = bookingDao.revenueByDayForMonth(today.getYear(), today.getMonthValue(), ownerScope);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        int daysInMonth = today.lengthOfMonth();
        for (int day = 1; day <= daysInMonth; day++) {
            BigDecimal revenue = revenueByDay.getOrDefault(day, BigDecimal.ZERO);
            series.getData().add(new XYChart.Data<>(String.valueOf(day), revenue));
        }
        revenueTrendChart.getData().setAll(series);
    }

    private void buildStatusPieChart(List<Booking> bookings) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Booking b : bookings) {
            counts.merge(statusLabel(b.getStatus()), 1L, Long::sum);
        }
        var data = FXCollections.<PieChart.Data>observableArrayList();
        counts.forEach((label, count) -> data.add(new PieChart.Data(label + " (" + count + ")", count)));
        statusPieChart.setData(data);
    }

    private void buildFieldBarChart(List<Booking> bookings) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Booking b : bookings) {
            counts.merge(b.getFieldName(), 1L, Long::sum);
        }
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        counts.forEach((field, count) -> series.getData().add(new XYChart.Data<>(field, count)));
        fieldBarChart.getData().setAll(series);
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "pending" -> "Chờ xác nhận";
            case "confirmed" -> "Đã xác nhận";
            case "cancelled" -> "Đã hủy";
            case "completed" -> "Hoàn thành";
            default -> status;
        };
    }
}
