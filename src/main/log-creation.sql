CREATE TABLE `log` (
  `datetime` datetime NOT NULL,
  `ip` varchar(45) NOT NULL,
  `method` varchar(45) DEFAULT NULL,
  `status_resp` varchar(45) DEFAULT NULL,
  `client` varchar(200) DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1