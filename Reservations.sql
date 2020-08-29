CREATE TABLE Reservations(
    Username VARCHAR(20),
    PaidOrNot TINYINT,
    Price SMALLINT,
    Day TINYINT,
    Fid1 INT,
    Fid2 INT,
    PRIMARY KEY (Username, Day)
);

