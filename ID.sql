CREATE TABLE ID(
    ReservationID INT AUTO_INCREMENT PRIMARY KEY,
    Username VARCHAR(20),
    Day TINYINT,
    FOREIGN KEY (Username, Day)
      REFERENCES Reservations (Username, Day)
      ON DELETE CASCADE
);
