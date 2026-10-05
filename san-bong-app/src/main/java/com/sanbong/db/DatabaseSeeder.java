package com.sanbong.db;

import com.sanbong.dao.FieldDao;
import com.sanbong.dao.TimeSlotDao;
import com.sanbong.dao.UserDao;
import com.sanbong.dao.VenueDao;
import com.sanbong.model.Field;
import com.sanbong.model.TimeSlot;
import com.sanbong.model.User;
import com.sanbong.model.Venue;
import com.sanbong.util.DatabaseConnection;
import org.mindrot.jbcrypt.BCrypt;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalTime;

/** Populates sample data (2 venue owners, 2 venues, 5 fields, time slots, admin + customer accounts) once, on an empty database. */
public final class DatabaseSeeder {

    private DatabaseSeeder() {
    }

    public static void seedIfEmpty() {
        if (!isEmpty()) {
            return;
        }

        VenueDao venueDao = new VenueDao();
        FieldDao fieldDao = new FieldDao();
        TimeSlotDao timeSlotDao = new TimeSlotDao();
        UserDao userDao = new UserDao();

        User admin = new User("Quản Trị Viên", "admin@sanbong.local", BCrypt.hashpw("admin123", BCrypt.gensalt()),
                "0900000000", "admin");
        userDao.insert(admin);

        User owner1 = new User("Chủ Sân Thành Công", "chusan1@sanbong.local", BCrypt.hashpw("owner123", BCrypt.gensalt()),
                "0903333333", "venue_owner");
        int owner1Id = userDao.insert(owner1);

        User owner2 = new User("Chủ Sân Mỹ Đình", "chusan2@sanbong.local", BCrypt.hashpw("owner123", BCrypt.gensalt()),
                "0904444444", "venue_owner");
        int owner2Id = userDao.insert(owner2);

        // Demo account showing the admin-approval workflow for new venue-owner sign-ups.
        User pendingOwner = new User("Chủ Sân Cầu Giấy", "chusan3@sanbong.local", BCrypt.hashpw("owner123", BCrypt.gensalt()),
                "0905555555", "venue_owner");
        pendingOwner.setStatus("pending");
        userDao.insert(pendingOwner);

        Venue venue1Data = new Venue("Cụm sân Thành Công", "12 Thành Công, Ba Đình, Hà Nội", "0901111111",
                "Cụm sân cỏ nhân tạo chất lượng cao, ánh sáng tốt, gần trung tâm.");
        venue1Data.setOwnerId(owner1Id);
        int venue1 = venueDao.insert(venue1Data);

        Venue venue2Data = new Venue("Cụm sân Mỹ Đình", "45 Lê Đức Thọ, Nam Từ Liêm, Hà Nội", "0902222222",
                "Cụm sân rộng rãi, bãi đỗ xe thoải mái, có căng tin phục vụ.");
        venue2Data.setOwnerId(owner2Id);
        int venue2 = venueDao.insert(venue2Data);

        int field1 = fieldDao.insert(new Field(venue1, "Sân A - 5 người", "5", "Mặt cỏ nhân tạo, có mái che một phần."));
        int field2 = fieldDao.insert(new Field(venue1, "Sân B - 7 người", "7", "Sân tiêu chuẩn 7 người, chiếu sáng tốt."));
        int field3 = fieldDao.insert(new Field(venue2, "Sân C - 5 người", "5", "Sân 5 người, phù hợp thi đấu buổi tối."));
        int field4 = fieldDao.insert(new Field(venue2, "Sân D - 7 người", "7", "Sân 7 người, mặt cỏ mới thay."));
        int field5 = fieldDao.insert(new Field(venue2, "Sân E - 11 người", "11", "Sân 11 người tiêu chuẩn thi đấu."));

        int[] fiveASideFields = {field1, field3};
        int[] sevenASideFields = {field2, field4};
        int[] elevenASideFields = {field5};

        for (int fieldId : fiveASideFields) {
            seedSlotsForField(timeSlotDao, fieldId, new BigDecimal("300000"), new BigDecimal("400000"));
        }
        for (int fieldId : sevenASideFields) {
            seedSlotsForField(timeSlotDao, fieldId, new BigDecimal("450000"), new BigDecimal("600000"));
        }
        for (int fieldId : elevenASideFields) {
            seedSlotsForField(timeSlotDao, fieldId, new BigDecimal("700000"), new BigDecimal("900000"));
        }

        String[][] customers = {
                {"Nguyễn Văn A", "khach1@sanbong.local", "0911111111"},
                {"Trần Thị B", "khach2@sanbong.local", "0922222222"},
                {"Lê Văn C", "khach3@sanbong.local", "0933333333"},
                {"Phạm Thị D", "khach4@sanbong.local", "0944444444"},
                {"Hoàng Văn E", "khach5@sanbong.local", "0955555555"},
        };
        for (String[] c : customers) {
            User customer = new User(c[0], c[1], BCrypt.hashpw("123456", BCrypt.gensalt()), c[2], "customer");
            userDao.insert(customer);
        }
    }

    private static void seedSlotsForField(TimeSlotDao dao, int fieldId, BigDecimal weekdayPrice, BigDecimal weekendPrice) {
        int[][] ranges = {{17, 18}, {18, 19}, {19, 20}, {20, 21}};
        for (int[] range : ranges) {
            LocalTime start = LocalTime.of(range[0], 0);
            LocalTime end = LocalTime.of(range[1], 0);
            dao.insert(new TimeSlot(fieldId, start, end, "weekday", weekdayPrice));
            dao.insert(new TimeSlot(fieldId, start, end, "weekend", weekendPrice));
        }
    }

    private static boolean isEmpty() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            var rs = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM users");
            return rs.next() && rs.getInt("cnt") == 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
