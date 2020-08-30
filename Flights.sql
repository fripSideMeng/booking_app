CREATE TABLE Flights(
  fid int,
  month_id tinyint,
  day_of_month tinyint,
  day_of_week_id tinyint,
  carrier_id varchar(7),
  flight_num mediumint,
  origin_city varchar(34),
  origin_state varchar(47),
  dest_city varchar(34),
  dest_state varchar(46),
  departure_delay smallint,
  taxi_out smallint,
  arrival_delay smallint,
  canceled tinyint,
  actual_time smallint,
  distance smallint,
  capacity smallint,
  price smallint,
  PRIMARY KEY (actual_time, fid)
);
ALTER TABLE Flights ENGINE=MyISAM;

