package com.talkify.identity.domain.model;

import com.talkify.common.exception.AppException;
import com.talkify.common.exception.ErrorCode;

public enum OtpPurpose {

    REGISTRATION {
        @Override
        public void assertValidState(User user) {
            if (user.getStatus() == UserStatus.ACTIVE) {
                throw new AppException(ErrorCode.USER_ALREADY_ACTIVE);
            }
        }

        @Override
        public void applyEffect(User user) {
            user.activate();
        }
    },

    PASSWORD_RESET {
        @Override
        public void assertValidState(User user) {
            if (user.getStatus() != UserStatus.ACTIVE) {
                throw new AppException(ErrorCode.USER_NOT_VERIFIED);
            }
        }

        @Override
        public void applyEffect(User user) {
            // OTP chỉ xác thực danh tính — đổi password ở bước tiếp theo
        }
    },

    TWO_FACTOR_AUTH {
        @Override
        public void assertValidState(User user) {
            if (user.getStatus() != UserStatus.ACTIVE) {
                throw new AppException(ErrorCode.USER_NOT_VERIFIED);
            }
        }

        @Override
        public void applyEffect(User user) {
            // 2FA session marking — implement cùng session management
        }
    };

    /**
     * Kiểm tra user state có hợp lệ với purpose này không.
     * Throw AppException ngay nếu không hợp lệ — trước khi chạm vào Redis.
     */
    public abstract void assertValidState(User user);

    /**
     * Áp dụng domain action sau khi OTP verify thành công.
     * Mỗi purpose có side effect khác nhau.
     */
    public abstract void applyEffect(User user);
}