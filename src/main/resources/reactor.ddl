DROP TABLE IF EXISTS `reactor`;
CREATE TABLE `reactor` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `counter` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `counter` (`counter`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;