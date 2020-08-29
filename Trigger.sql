delimiter //
CREATE TRIGGER id_insertion AFTER INSERT ON Reservations
FOR EACH ROW
BEGIN
  INSERT INTO ID (Username,Day) VALUES (NEW.Username,NEW.Day);
END;//
delimiter ;
