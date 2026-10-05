package com.sanbong.model;

import java.math.BigDecimal;
import java.time.LocalTime;

public class TimeSlot {
    private int id;
    private int fieldId;
    private LocalTime startTime;
    private LocalTime endTime;
    private String dayType; // weekday | weekend
    private BigDecimal price;

    /** Populated by availability queries; not a DB column. */
    private boolean booked;

    public TimeSlot() {
    }

    public TimeSlot(int fieldId, LocalTime startTime, LocalTime endTime, String dayType, BigDecimal price) {
        this.fieldId = fieldId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.dayType = dayType;
        this.price = price;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getFieldId() {
        return fieldId;
    }

    public void setFieldId(int fieldId) {
        this.fieldId = fieldId;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getDayType() {
        return dayType;
    }

    public void setDayType(String dayType) {
        this.dayType = dayType;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public boolean isBooked() {
        return booked;
    }

    public void setBooked(boolean booked) {
        this.booked = booked;
    }

    public String getLabel() {
        return startTime + " - " + endTime;
    }
}
