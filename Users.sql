CREATE TABLE Users(
    Username VARCHAR(20) PRIMARY KEY,
    Password VARBINARY(20),
    Salt VARBINARY(16),
    Balance INT,
);

