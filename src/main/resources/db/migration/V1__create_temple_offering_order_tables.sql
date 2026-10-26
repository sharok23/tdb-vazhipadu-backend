CREATE TABLE temple (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE offering (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    temple_id BIGINT         NOT NULL REFERENCES temple (id),
    name      VARCHAR(100)   NOT NULL,
    price     DECIMAL(10, 2) NOT NULL CHECK (price > 0),
    active    BOOLEAN        NOT NULL,
    CONSTRAINT uk_offering_temple_name UNIQUE (temple_id, name)
);

CREATE TABLE orders (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    temple_id    BIGINT                   NOT NULL REFERENCES temple (id),
    devotee_name VARCHAR(100)             NOT NULL,
    phone        VARCHAR(10)              NOT NULL,
    nakshatra    VARCHAR(20)              NOT NULL,
    booking_date DATE                     NOT NULL,
    status       VARCHAR(20)              NOT NULL,
    total_amount DECIMAL(12, 2)           NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE order_line (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id      BIGINT         NOT NULL REFERENCES orders (id),
    offering_id   BIGINT         NOT NULL REFERENCES offering (id),
    offering_name VARCHAR(100)   NOT NULL,
    unit_price    DECIMAL(10, 2) NOT NULL,
    quantity      INT            NOT NULL CHECK (quantity > 0),
    line_total    DECIMAL(12, 2) NOT NULL
);

INSERT INTO temple (name) VALUES ('Sabarimala Sree Dharma Sastha Temple');
