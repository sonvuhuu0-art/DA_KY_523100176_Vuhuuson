package com.sanbong.service;

import com.sanbong.dao.UserDao;
import com.sanbong.model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Optional;

public class AuthService {

    private final UserDao userDao = new UserDao();

    public Optional<User> login(String email, String rawPassword) {
        Optional<User> found = userDao.findByEmail(email);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        User user = found.get();
        if ("locked".equals(user.getStatus())) {
            throw new IllegalStateException("Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên.");
        }
        if ("pending".equals(user.getStatus())) {
            throw new IllegalStateException("Tài khoản chủ sân của bạn đang chờ quản trị viên phê duyệt.");
        }
        if (!BCrypt.checkpw(rawPassword, user.getPassword())) {
            return Optional.empty();
        }
        return Optional.of(user);
    }

    public User register(String fullName, String email, String rawPassword, String phone) {
        return register(fullName, email, rawPassword, phone, false);
    }

    /**
     * Registers a new account. Venue-owner sign-ups start in 'pending' status and cannot log in
     * until an admin approves them — customers are active immediately.
     */
    public User register(String fullName, String email, String rawPassword, String phone, boolean asVenueOwner) {
        if (userDao.existsByEmail(email)) {
            throw new IllegalArgumentException("Email này đã được đăng ký.");
        }
        String hashed = BCrypt.hashpw(rawPassword, BCrypt.gensalt());
        String role = asVenueOwner ? "venue_owner" : "customer";
        User user = new User(fullName, email, hashed, phone, role);
        user.setStatus(asVenueOwner ? "pending" : "active");
        int id = userDao.insert(user);
        user.setId(id);
        return user;
    }

    public void changePassword(int userId, String oldPassword, String newPassword) {
        User user = userDao.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy tài khoản."));
        if (!BCrypt.checkpw(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không đúng.");
        }
        if (newPassword.length() < 6) {
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 6 ký tự.");
        }
        userDao.updatePassword(userId, BCrypt.hashpw(newPassword, BCrypt.gensalt()));
    }
}
