package com.sanbong.util;

import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;

import java.time.LocalDate;

public final class DatePickerUtil {

    private DatePickerUtil() {
    }

    /** Disables every date before today so users cannot create bookings in the past. */
    public static void restrictToFutureDates(DatePicker picker) {
        picker.setDayCellFactory(picker1 -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (!empty && date.isBefore(LocalDate.now())) {
                    setDisable(true);
                    getStyleClass().add("date-cell-disabled");
                }
            }
        });
    }
}
