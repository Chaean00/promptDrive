CREATE TABLE event_publication (
    id BINARY(16) NOT NULL,
    publication_date DATETIME(6) NOT NULL,
    listener_id VARCHAR(255) NOT NULL,
    serialized_event VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    completion_date DATETIME(6),
    last_resubmission_date DATETIME(6),
    completion_attempts INT NOT NULL,
    status ENUM('PUBLISHED', 'PROCESSING', 'COMPLETED', 'FAILED', 'RESUBMITTED'),
    PRIMARY KEY (id)
);
