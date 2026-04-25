CREATE TABLE IF NOT EXISTS location (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    building VARCHAR(255) NOT NULL,
    room_number VARCHAR(100),
    max_capacity INT NOT NULL,
    image VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS event (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    event_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    image VARCHAR(255),
    location_id INT,
    average_rating DOUBLE DEFAULT 0.0,
    CONSTRAINT fk_event_location FOREIGN KEY (location_id) REFERENCES location(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS event_rating (
    id INT AUTO_INCREMENT PRIMARY KEY,
    score INT NOT NULL,
    event_id INT NOT NULL,
    user_id BINARY(16) NOT NULL,
    CONSTRAINT fk_rating_event FOREIGN KEY (event_id) REFERENCES event(id) ON DELETE CASCADE,
    CONSTRAINT fk_rating_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY(event_id, user_id)
);

CREATE TABLE IF NOT EXISTS user_favourite_events (
    user_id BINARY(16) NOT NULL,
    event_id INT NOT NULL,
    PRIMARY KEY (user_id, event_id),
    CONSTRAINT fk_fav_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_fav_event FOREIGN KEY (event_id) REFERENCES event(id) ON DELETE CASCADE
);
