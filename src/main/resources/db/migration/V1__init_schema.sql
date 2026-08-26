-- ============================================================================
-- Slotify - initial database schema.
--
-- Conventions
--   * All tables: BIGINT auto-increment primary key, created_at / updated_at.
--   * Soft-deletable tables carry a nullable deleted_at.
--   * Timestamps (DATETIME(6)) are stored in UTC. Local times of day
--     (TIME columns) are interpreted in the salon's timezone.
--   * Money is stored as minor units (cents) in BIGINT plus an ISO-4217
--     currency code; never as floating point.
--   * Every salon-scoped entity has a salon_id for multi-tenant filtering.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- Users & authentication
-- ---------------------------------------------------------------------------
CREATE TABLE users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    email           VARCHAR(255)    NOT NULL,
    phone           VARCHAR(32)     NULL COMMENT 'Contact only; not used for login',
    password_hash   VARCHAR(255)    NULL COMMENT 'NULL for social-login accounts',
    full_name       VARCHAR(150)    NOT NULL,
    avatar_url      VARCHAR(500)    NULL,
    role            ENUM('SUPER_ADMIN','SALON_OWNER','STAFF','CUSTOMER') NOT NULL DEFAULT 'CUSTOMER',
    provider        ENUM('LOCAL','GOOGLE') NOT NULL DEFAULT 'LOCAL',
    provider_id     VARCHAR(255)    NULL COMMENT 'Subject id from the social provider',
    email_verified  TINYINT(1)      NOT NULL DEFAULT 0,
    locale          VARCHAR(10)     NOT NULL DEFAULT 'en',
    status          ENUM('ACTIVE','SUSPENDED','DELETED') NOT NULL DEFAULT 'ACTIVE',
    created_at      DATETIME(6)     NOT NULL,
    updated_at      DATETIME(6)     NOT NULL,
    deleted_at      DATETIME(6)     NULL,
    UNIQUE KEY uk_users_email (email),
    KEY idx_users_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE refresh_tokens (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    token_hash  VARCHAR(128) NOT NULL COMMENT 'SHA-256 of the opaque refresh token',
    expires_at  DATETIME(6)  NOT NULL,
    revoked     TINYINT(1)   NOT NULL DEFAULT 0,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    UNIQUE KEY uk_refresh_tokens_hash (token_hash),
    KEY idx_refresh_tokens_user (user_id),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE password_reset_tokens (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    token_hash  VARCHAR(128) NOT NULL,
    expires_at  DATETIME(6)  NOT NULL,
    used        TINYINT(1)   NOT NULL DEFAULT 0,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    UNIQUE KEY uk_password_reset_tokens_hash (token_hash),
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE email_verification_tokens (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    token_hash  VARCHAR(128) NOT NULL,
    expires_at  DATETIME(6)  NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    UNIQUE KEY uk_email_verification_tokens_hash (token_hash),
    CONSTRAINT fk_email_verification_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE device_tokens (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    fcm_token   VARCHAR(512) NOT NULL,
    platform    ENUM('ANDROID','IOS','WEB') NOT NULL,
    app_type    ENUM('CUSTOMER','STAFF') NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    UNIQUE KEY uk_device_tokens_token (fcm_token(255)),
    KEY idx_device_tokens_user (user_id),
    CONSTRAINT fk_device_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- Global catalog (managed by SUPER_ADMIN)
-- ---------------------------------------------------------------------------
CREATE TABLE categories (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    icon_url    VARCHAR(500) NULL,
    sort_order  INT          NOT NULL DEFAULT 0,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE amenities (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    icon        VARCHAR(100) NULL COMMENT 'Icon identifier used by the clients',
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- Salons
-- ---------------------------------------------------------------------------
CREATE TABLE salons (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_id            BIGINT          NOT NULL,
    name                VARCHAR(150)    NOT NULL,
    slug                VARCHAR(160)    NOT NULL,
    description         TEXT            NULL,
    phone               VARCHAR(32)     NULL,
    email               VARCHAR(255)    NULL,
    address             VARCHAR(255)    NOT NULL,
    city                VARCHAR(100)    NOT NULL,
    country             CHAR(2)         NOT NULL COMMENT 'ISO-3166-1 alpha-2',
    lat                 DECIMAL(10,7)   NULL,
    lng                 DECIMAL(10,7)   NULL,
    timezone            VARCHAR(64)     NOT NULL DEFAULT 'Europe/Berlin' COMMENT 'IANA timezone id',
    currency            CHAR(3)         NOT NULL DEFAULT 'EUR' COMMENT 'ISO-4217',
    cover_url           VARCHAR(500)    NULL,
    rating_avg          DECIMAL(3,2)    NOT NULL DEFAULT 0.00,
    rating_count        INT             NOT NULL DEFAULT 0,
    status              ENUM('PENDING','ACTIVE','SUSPENDED') NOT NULL DEFAULT 'PENDING',
    commission_percent  DECIMAL(5,2)    NOT NULL DEFAULT 0.00 COMMENT 'Platform commission on online payments',
    created_at          DATETIME(6)     NOT NULL,
    updated_at          DATETIME(6)     NOT NULL,
    deleted_at          DATETIME(6)     NULL,
    UNIQUE KEY uk_salons_slug (slug),
    KEY idx_salons_owner (owner_id),
    KEY idx_salons_city_status (city, status),
    KEY idx_salons_geo (lat, lng),
    CONSTRAINT fk_salons_owner FOREIGN KEY (owner_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE salon_images (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    salon_id    BIGINT       NOT NULL,
    url         VARCHAR(500) NOT NULL,
    sort_order  INT          NOT NULL DEFAULT 0,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    KEY idx_salon_images_salon (salon_id),
    CONSTRAINT fk_salon_images_salon FOREIGN KEY (salon_id) REFERENCES salons (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE salon_opening_hours (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    salon_id    BIGINT      NOT NULL,
    day_of_week TINYINT     NOT NULL COMMENT '0 = Monday ... 6 = Sunday (java.time DayOfWeek - 1)',
    open_time   TIME        NULL COMMENT 'Local time in salon timezone',
    close_time  TIME        NULL,
    is_closed   TINYINT(1)  NOT NULL DEFAULT 0,
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL,
    UNIQUE KEY uk_salon_opening_hours (salon_id, day_of_week),
    CONSTRAINT fk_salon_opening_hours_salon FOREIGN KEY (salon_id) REFERENCES salons (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE salon_settings (
    id                          BIGINT AUTO_INCREMENT PRIMARY KEY,
    salon_id                    BIGINT      NOT NULL,
    slot_interval_min           INT         NOT NULL DEFAULT 15,
    min_advance_booking_min     INT         NOT NULL DEFAULT 60,
    max_advance_days            INT         NOT NULL DEFAULT 60,
    cancel_before_min           INT         NOT NULL DEFAULT 1440 COMMENT 'Customer may cancel until N minutes before start',
    auto_confirm                TINYINT(1)  NOT NULL DEFAULT 1,
    require_deposit             TINYINT(1)  NOT NULL DEFAULT 0,
    deposit_percent             DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    accept_stripe               TINYINT(1)  NOT NULL DEFAULT 1,
    accept_paypal               TINYINT(1)  NOT NULL DEFAULT 0,
    accept_cash                 TINYINT(1)  NOT NULL DEFAULT 1,
    created_at                  DATETIME(6) NOT NULL,
    updated_at                  DATETIME(6) NOT NULL,
    UNIQUE KEY uk_salon_settings_salon (salon_id),
    CONSTRAINT fk_salon_settings_salon FOREIGN KEY (salon_id) REFERENCES salons (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE salon_categories (
    salon_id    BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    PRIMARY KEY (salon_id, category_id),
    CONSTRAINT fk_salon_categories_salon FOREIGN KEY (salon_id) REFERENCES salons (id) ON DELETE CASCADE,
    CONSTRAINT fk_salon_categories_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE salon_amenities (
    salon_id    BIGINT NOT NULL,
    amenity_id  BIGINT NOT NULL,
    PRIMARY KEY (salon_id, amenity_id),
    CONSTRAINT fk_salon_amenities_salon FOREIGN KEY (salon_id) REFERENCES salons (id) ON DELETE CASCADE,
    CONSTRAINT fk_salon_amenities_amenity FOREIGN KEY (amenity_id) REFERENCES amenities (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- Services offered by a salon
-- ---------------------------------------------------------------------------
CREATE TABLE service_categories (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    salon_id    BIGINT       NOT NULL,
    name        VARCHAR(100) NOT NULL,
    sort_order  INT          NOT NULL DEFAULT 0,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    KEY idx_service_categories_salon (salon_id),
    CONSTRAINT fk_service_categories_salon FOREIGN KEY (salon_id) REFERENCES salons (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE services (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    salon_id            BIGINT       NOT NULL,
    category_id         BIGINT       NULL,
    name                VARCHAR(150) NOT NULL,
    description         TEXT         NULL,
    duration_min        INT          NOT NULL,
    buffer_after_min    INT          NOT NULL DEFAULT 0 COMMENT 'Cleanup time after the service',
    price_minor         BIGINT       NOT NULL,
    currency            CHAR(3)      NOT NULL,
    image_url           VARCHAR(500) NULL,
    is_active           TINYINT(1)   NOT NULL DEFAULT 1,
    sort_order          INT          NOT NULL DEFAULT 0,
    created_at          DATETIME(6)  NOT NULL,
    updated_at          DATETIME(6)  NOT NULL,
    deleted_at          DATETIME(6)  NULL,
    KEY idx_services_salon_active (salon_id, is_active),
    CONSTRAINT fk_services_salon FOREIGN KEY (salon_id) REFERENCES salons (id) ON DELETE CASCADE,
    CONSTRAINT fk_services_category FOREIGN KEY (category_id) REFERENCES service_categories (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- Staff, shifts & time off
-- ---------------------------------------------------------------------------
CREATE TABLE staffs (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    salon_id        BIGINT       NOT NULL,
    user_id         BIGINT       NULL COMMENT 'Linked login account (role STAFF); NULL until invitation accepted',
    display_name    VARCHAR(150) NOT NULL,
    title           VARCHAR(100) NULL COMMENT 'e.g. Senior Stylist',
    bio             TEXT         NULL,
    avatar_url      VARCHAR(500) NULL,
    rating_avg      DECIMAL(3,2) NOT NULL DEFAULT 0.00,
    rating_count    INT          NOT NULL DEFAULT 0,
    is_active       TINYINT(1)   NOT NULL DEFAULT 1,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    deleted_at      DATETIME(6)  NULL,
    KEY idx_staffs_salon (salon_id),
    KEY idx_staffs_user (user_id),
    CONSTRAINT fk_staffs_salon FOREIGN KEY (salon_id) REFERENCES salons (id) ON DELETE CASCADE,
    CONSTRAINT fk_staffs_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE staff_services (
    staff_id    BIGINT NOT NULL,
    service_id  BIGINT NOT NULL,
    PRIMARY KEY (staff_id, service_id),
    CONSTRAINT fk_staff_services_staff FOREIGN KEY (staff_id) REFERENCES staffs (id) ON DELETE CASCADE,
    CONSTRAINT fk_staff_services_service FOREIGN KEY (service_id) REFERENCES services (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE staff_shifts (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    staff_id    BIGINT      NOT NULL,
    day_of_week TINYINT     NOT NULL COMMENT '0 = Monday ... 6 = Sunday',
    start_time  TIME        NOT NULL COMMENT 'Local time in salon timezone',
    end_time    TIME        NOT NULL,
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL,
    KEY idx_staff_shifts_staff_day (staff_id, day_of_week),
    CONSTRAINT fk_staff_shifts_staff FOREIGN KEY (staff_id) REFERENCES staffs (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE staff_shift_overrides (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    staff_id    BIGINT      NOT NULL,
    date        DATE        NOT NULL COMMENT 'Local date in salon timezone',
    start_time  TIME        NULL,
    end_time    TIME        NULL,
    is_off      TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '1 = whole day off, times ignored',
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL,
    UNIQUE KEY uk_staff_shift_overrides (staff_id, date),
    CONSTRAINT fk_staff_shift_overrides_staff FOREIGN KEY (staff_id) REFERENCES staffs (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE staff_time_off (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    staff_id    BIGINT       NOT NULL,
    start_at    DATETIME(6)  NOT NULL,
    end_at      DATETIME(6)  NOT NULL,
    reason      VARCHAR(255) NULL,
    status      ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'APPROVED',
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    KEY idx_staff_time_off_staff_range (staff_id, start_at, end_at),
    CONSTRAINT fk_staff_time_off_staff FOREIGN KEY (staff_id) REFERENCES staffs (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- Promotions
-- ---------------------------------------------------------------------------
CREATE TABLE coupons (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    salon_id            BIGINT       NOT NULL,
    code                VARCHAR(50)  NOT NULL,
    type                ENUM('PERCENT','FIXED') NOT NULL,
    value               BIGINT       NOT NULL COMMENT 'Percent (0-100) or fixed amount in minor units',
    min_order_minor     BIGINT       NOT NULL DEFAULT 0,
    max_discount_minor  BIGINT       NULL,
    usage_limit         INT          NULL,
    used_count          INT          NOT NULL DEFAULT 0,
    per_user_limit      INT          NULL,
    starts_at           DATETIME(6)  NULL,
    ends_at             DATETIME(6)  NULL,
    is_active           TINYINT(1)   NOT NULL DEFAULT 1,
    created_at          DATETIME(6)  NOT NULL,
    updated_at          DATETIME(6)  NOT NULL,
    UNIQUE KEY uk_coupons_salon_code (salon_id, code),
    CONSTRAINT fk_coupons_salon FOREIGN KEY (salon_id) REFERENCES salons (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- Bookings
-- ---------------------------------------------------------------------------
CREATE TABLE bookings (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    code                VARCHAR(20)  NOT NULL COMMENT 'Human-friendly reference, e.g. SLT-8F3K2',
    salon_id            BIGINT       NOT NULL,
    customer_id         BIGINT       NOT NULL,
    staff_id            BIGINT       NULL COMMENT 'Assigned staff; set on creation (auto-assigned when customer picked "any")',
    staff_auto_assigned TINYINT(1)   NOT NULL DEFAULT 0,
    start_at            DATETIME(6)  NOT NULL,
    end_at              DATETIME(6)  NOT NULL,
    status              ENUM('PENDING','CONFIRMED','IN_PROGRESS','COMPLETED','CANCELLED','NO_SHOW','REJECTED') NOT NULL DEFAULT 'PENDING',
    subtotal_minor      BIGINT       NOT NULL,
    discount_minor      BIGINT       NOT NULL DEFAULT 0,
    total_minor         BIGINT       NOT NULL,
    currency            CHAR(3)      NOT NULL,
    coupon_id           BIGINT       NULL,
    payment_status      ENUM('UNPAID','PAID','PARTIALLY_PAID','REFUNDED') NOT NULL DEFAULT 'UNPAID',
    payment_method      ENUM('STRIPE','PAYPAL','CASH') NULL,
    note                VARCHAR(500) NULL COMMENT 'Customer note to the salon',
    cancel_reason       VARCHAR(255) NULL,
    cancelled_by        ENUM('CUSTOMER','SALON','SYSTEM') NULL,
    reminder_sent_at    DATETIME(6)  NULL,
    created_at          DATETIME(6)  NOT NULL,
    updated_at          DATETIME(6)  NOT NULL,
    UNIQUE KEY uk_bookings_code (code),
    KEY idx_bookings_salon_start (salon_id, start_at),
    KEY idx_bookings_staff_start_status (staff_id, start_at, status),
    KEY idx_bookings_customer_start (customer_id, start_at),
    KEY idx_bookings_status_start (status, start_at),
    CONSTRAINT fk_bookings_salon FOREIGN KEY (salon_id) REFERENCES salons (id),
    CONSTRAINT fk_bookings_customer FOREIGN KEY (customer_id) REFERENCES users (id),
    CONSTRAINT fk_bookings_staff FOREIGN KEY (staff_id) REFERENCES staffs (id) ON DELETE SET NULL,
    CONSTRAINT fk_bookings_coupon FOREIGN KEY (coupon_id) REFERENCES coupons (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE booking_items (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id      BIGINT       NOT NULL,
    service_id      BIGINT       NULL,
    service_name    VARCHAR(150) NOT NULL COMMENT 'Snapshot at booking time',
    duration_min    INT          NOT NULL,
    price_minor     BIGINT       NOT NULL,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    KEY idx_booking_items_booking (booking_id),
    CONSTRAINT fk_booking_items_booking FOREIGN KEY (booking_id) REFERENCES bookings (id) ON DELETE CASCADE,
    CONSTRAINT fk_booking_items_service FOREIGN KEY (service_id) REFERENCES services (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE coupon_usages (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    coupon_id   BIGINT      NOT NULL,
    user_id     BIGINT      NOT NULL,
    booking_id  BIGINT      NOT NULL,
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL,
    UNIQUE KEY uk_coupon_usages_booking (booking_id),
    KEY idx_coupon_usages_coupon_user (coupon_id, user_id),
    CONSTRAINT fk_coupon_usages_coupon FOREIGN KEY (coupon_id) REFERENCES coupons (id) ON DELETE CASCADE,
    CONSTRAINT fk_coupon_usages_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_coupon_usages_booking FOREIGN KEY (booking_id) REFERENCES bookings (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- Payments
-- ---------------------------------------------------------------------------
CREATE TABLE payments (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id      BIGINT       NOT NULL,
    provider        ENUM('STRIPE','PAYPAL','CASH') NOT NULL,
    provider_ref    VARCHAR(255) NULL COMMENT 'Stripe PaymentIntent id / PayPal order id',
    amount_minor    BIGINT       NOT NULL,
    currency        CHAR(3)      NOT NULL,
    type            ENUM('FULL','DEPOSIT') NOT NULL DEFAULT 'FULL',
    status          ENUM('PENDING','SUCCEEDED','FAILED','REFUNDED','PARTIAL_REFUND') NOT NULL DEFAULT 'PENDING',
    raw_json        JSON         NULL COMMENT 'Last provider payload, for support/debugging',
    paid_at         DATETIME(6)  NULL,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    KEY idx_payments_booking (booking_id),
    KEY idx_payments_provider_ref (provider_ref),
    CONSTRAINT fk_payments_booking FOREIGN KEY (booking_id) REFERENCES bookings (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE refunds (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id      BIGINT       NOT NULL,
    amount_minor    BIGINT       NOT NULL,
    provider_ref    VARCHAR(255) NULL,
    status          ENUM('PENDING','SUCCEEDED','FAILED') NOT NULL DEFAULT 'PENDING',
    reason          VARCHAR(255) NULL,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    KEY idx_refunds_payment (payment_id),
    CONSTRAINT fk_refunds_payment FOREIGN KEY (payment_id) REFERENCES payments (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- Reviews
-- ---------------------------------------------------------------------------
CREATE TABLE reviews (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id  BIGINT      NOT NULL,
    salon_id    BIGINT      NOT NULL,
    staff_id    BIGINT      NULL,
    customer_id BIGINT      NOT NULL,
    rating      TINYINT     NOT NULL COMMENT '1-5',
    comment     TEXT        NULL,
    reply       TEXT        NULL COMMENT 'Salon owner reply',
    replied_at  DATETIME(6) NULL,
    is_visible  TINYINT(1)  NOT NULL DEFAULT 1,
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL,
    UNIQUE KEY uk_reviews_booking (booking_id),
    KEY idx_reviews_salon (salon_id, is_visible),
    KEY idx_reviews_staff (staff_id),
    CONSTRAINT fk_reviews_booking FOREIGN KEY (booking_id) REFERENCES bookings (id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_salon FOREIGN KEY (salon_id) REFERENCES salons (id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_staff FOREIGN KEY (staff_id) REFERENCES staffs (id) ON DELETE SET NULL,
    CONSTRAINT fk_reviews_customer FOREIGN KEY (customer_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- Notifications & favorites
-- ---------------------------------------------------------------------------
CREATE TABLE notifications (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    type        VARCHAR(50)  NOT NULL COMMENT 'e.g. BOOKING_CONFIRMED, BOOKING_REMINDER',
    title       VARCHAR(150) NOT NULL,
    body        VARCHAR(500) NOT NULL,
    data_json   JSON         NULL COMMENT 'Deep-link payload for the clients',
    read_at     DATETIME(6)  NULL,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    KEY idx_notifications_user_created (user_id, created_at),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE favorites (
    user_id     BIGINT      NOT NULL,
    salon_id    BIGINT      NOT NULL,
    created_at  DATETIME(6) NOT NULL,
    PRIMARY KEY (user_id, salon_id),
    CONSTRAINT fk_favorites_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_favorites_salon FOREIGN KEY (salon_id) REFERENCES salons (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- CRM (salon-scoped view of customers)
-- ---------------------------------------------------------------------------
CREATE TABLE customer_notes (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    salon_id    BIGINT      NOT NULL,
    customer_id BIGINT      NOT NULL,
    author_id   BIGINT      NOT NULL,
    note        TEXT        NOT NULL,
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL,
    KEY idx_customer_notes_salon_customer (salon_id, customer_id),
    CONSTRAINT fk_customer_notes_salon FOREIGN KEY (salon_id) REFERENCES salons (id) ON DELETE CASCADE,
    CONSTRAINT fk_customer_notes_customer FOREIGN KEY (customer_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_customer_notes_author FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE customer_tags (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    salon_id    BIGINT      NOT NULL,
    name        VARCHAR(50) NOT NULL,
    color       CHAR(7)     NOT NULL DEFAULT '#6B7280' COMMENT 'Hex color',
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL,
    UNIQUE KEY uk_customer_tags_salon_name (salon_id, name),
    CONSTRAINT fk_customer_tags_salon FOREIGN KEY (salon_id) REFERENCES salons (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE customer_tag_links (
    customer_id BIGINT NOT NULL,
    tag_id      BIGINT NOT NULL,
    PRIMARY KEY (customer_id, tag_id),
    CONSTRAINT fk_customer_tag_links_customer FOREIGN KEY (customer_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_customer_tag_links_tag FOREIGN KEY (tag_id) REFERENCES customer_tags (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- Audit trail
-- ---------------------------------------------------------------------------
CREATE TABLE audit_logs (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_id    BIGINT       NULL,
    salon_id    BIGINT       NULL,
    action      VARCHAR(50)  NOT NULL COMMENT 'e.g. BOOKING_CONFIRMED, SETTINGS_UPDATED',
    entity      VARCHAR(50)  NOT NULL,
    entity_id   BIGINT       NOT NULL,
    diff_json   JSON         NULL,
    created_at  DATETIME(6)  NOT NULL,
    KEY idx_audit_logs_entity (entity, entity_id),
    KEY idx_audit_logs_salon_created (salon_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- Platform-wide settings (single row per key)
-- ---------------------------------------------------------------------------
CREATE TABLE platform_settings (
    setting_key   VARCHAR(100) NOT NULL PRIMARY KEY,
    setting_value TEXT         NULL,
    updated_at    DATETIME(6)  NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
