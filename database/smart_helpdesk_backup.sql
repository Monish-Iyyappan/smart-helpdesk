-- MySQL dump 10.13  Distrib 8.0.46, for Win64 (x86_64)
--
-- Host: localhost    Database: smart_helpdesk
-- ------------------------------------------------------
-- Server version	8.0.46

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `categories`
--

DROP TABLE IF EXISTS `categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `categories` (
  `category_id` int NOT NULL AUTO_INCREMENT,
  `category_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`category_id`),
  UNIQUE KEY `category_name` (`category_name`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `categories`
--

LOCK TABLES `categories` WRITE;
/*!40000 ALTER TABLE `categories` DISABLE KEYS */;
INSERT INTO `categories` VALUES (7,'Campus Facilities'),(2,'Examination'),(4,'Fees and Payments'),(1,'IT Support'),(8,'Other'),(3,'Student Administration'),(6,'Timetable'),(5,'Transport');
/*!40000 ALTER TABLE `categories` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `comments`
--

DROP TABLE IF EXISTS `comments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `comments` (
  `comment_id` int NOT NULL AUTO_INCREMENT,
  `ticket_id` int NOT NULL,
  `user_id` int NOT NULL,
  `comment_text` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `comment_type` enum('GENERAL','ISSUE','REASSIGNMENT','CLOSURE') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'GENERAL',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`comment_id`),
  KEY `fk_comments_user` (`user_id`),
  KEY `idx_comments_ticket_id` (`ticket_id`),
  CONSTRAINT `fk_comments_ticket` FOREIGN KEY (`ticket_id`) REFERENCES `tickets` (`ticket_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_comments_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `comments`
--

LOCK TABLES `comments` WRITE;
/*!40000 ALTER TABLE `comments` DISABLE KEYS */;
INSERT INTO `comments` VALUES (1,1,7,'I am also experiencing this on the second floor near the chemistry stacks.','GENERAL','2026-10-02 09:20:00'),(2,1,1,'We have linked this ticket under the ongoing Campus Wi-Fi Outage incident.','GENERAL','2026-10-02 09:25:00'),(3,2,2,'Assigned to network engineering team. Checking the dorm access points now.','GENERAL','2026-10-02 09:35:00'),(4,2,5,'Thank you, signal keeps disconnecting every few minutes.','GENERAL','2026-10-02 09:40:00'),(5,3,2,'Radius authentication service reboot is in progress to restore logins.','GENERAL','2026-10-02 10:05:00'),(6,4,3,'Duplicate hall ticket generated and sent to your registered email address.','GENERAL','2026-10-03 11:30:00'),(7,4,4,'Received and printed. Thank you for the quick resolution!','GENERAL','2026-10-03 11:45:00'),(8,5,2,'Payment reconciliation verified. The official receipt is now available in your portal.','GENERAL','2026-10-01 14:00:00'),(9,5,1,'Ticket closed following successful student confirmation.','GENERAL','2026-10-01 16:30:00'),(10,6,4,'Lecturer is waiting for AV support in room B102.','GENERAL','2026-10-02 10:15:00'),(11,2,1,'Reassigned from Alex Rivera to Jordan Lee. Reason: he is absent due to a personla reason so i reassign it to jordan lee agent','REASSIGNMENT','2026-10-21 10:25:40'),(12,1,1,'the work has been completed nad checked','CLOSURE','2026-10-21 10:28:06');
/*!40000 ALTER TABLE `comments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `incidents`
--

DROP TABLE IF EXISTS `incidents`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `incidents` (
  `incident_id` int NOT NULL AUTO_INCREMENT,
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `location` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` enum('OPEN','IN_PROGRESS','RESOLVED','CLOSED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `severity` enum('LOW','MEDIUM','HIGH','CRITICAL') COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`incident_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `incidents`
--

LOCK TABLES `incidents` WRITE;
/*!40000 ALTER TABLE `incidents` DISABLE KEYS */;
INSERT INTO `incidents` VALUES (1,'Campus Wi-Fi Outage','Campus-wide intermittent Wi-Fi connectivity degradation affecting academic buildings.','Campus-Wide','IN_PROGRESS','HIGH','2026-10-02 09:00:00','2026-10-02 09:30:00'),(2,'Classroom Projector Failure','Main digital projector in lecture hall B102 is unresponsive.','Lecture Hall B102','OPEN','MEDIUM','2026-10-02 10:00:00','2026-10-02 10:00:00');
/*!40000 ALTER TABLE `incidents` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ticket_assignment_history`
--

DROP TABLE IF EXISTS `ticket_assignment_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ticket_assignment_history` (
  `history_id` int NOT NULL AUTO_INCREMENT,
  `ticket_id` int NOT NULL,
  `from_agent` int DEFAULT NULL,
  `to_agent` int NOT NULL,
  `changed_by` int NOT NULL,
  `reason` text COLLATE utf8mb4_unicode_ci,
  `changed_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`history_id`),
  KEY `fk_tah_from_agent` (`from_agent`),
  KEY `fk_tah_to_agent` (`to_agent`),
  KEY `fk_tah_changed_by` (`changed_by`),
  KEY `idx_assignment_ticket_id` (`ticket_id`),
  CONSTRAINT `fk_tah_changed_by` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_tah_from_agent` FOREIGN KEY (`from_agent`) REFERENCES `users` (`user_id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_tah_ticket` FOREIGN KEY (`ticket_id`) REFERENCES `tickets` (`ticket_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_tah_to_agent` FOREIGN KEY (`to_agent`) REFERENCES `users` (`user_id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ticket_assignment_history`
--

LOCK TABLES `ticket_assignment_history` WRITE;
/*!40000 ALTER TABLE `ticket_assignment_history` DISABLE KEYS */;
INSERT INTO `ticket_assignment_history` VALUES (1,2,2,3,1,'he is absent due to a personla reason so i reassign it to jordan lee agent','2026-10-21 10:25:40');
/*!40000 ALTER TABLE `ticket_assignment_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ticket_participants`
--

DROP TABLE IF EXISTS `ticket_participants`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ticket_participants` (
  `participant_id` int NOT NULL AUTO_INCREMENT,
  `ticket_id` int NOT NULL,
  `user_id` int NOT NULL,
  `joined_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`participant_id`),
  UNIQUE KEY `uq_ticket_participant` (`ticket_id`,`user_id`),
  KEY `idx_participants_user_id` (`user_id`),
  CONSTRAINT `fk_participants_ticket` FOREIGN KEY (`ticket_id`) REFERENCES `tickets` (`ticket_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_participants_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ticket_participants`
--

LOCK TABLES `ticket_participants` WRITE;
/*!40000 ALTER TABLE `ticket_participants` DISABLE KEYS */;
INSERT INTO `ticket_participants` VALUES (1,1,4,'2026-10-02 09:15:00'),(2,1,7,'2026-10-02 09:20:00'),(3,2,5,'2026-10-02 09:20:00'),(4,3,6,'2026-10-02 09:25:00'),(5,4,4,'2026-10-03 08:30:00'),(6,5,7,'2026-10-01 10:00:00'),(7,6,5,'2026-10-02 10:05:00'),(8,6,4,'2026-10-02 10:10:00'),(9,7,6,'2026-10-03 07:45:00'),(10,8,8,'2026-10-18 21:31:40'),(11,1,8,'2026-10-18 21:32:07');
/*!40000 ALTER TABLE `ticket_participants` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tickets`
--

DROP TABLE IF EXISTS `tickets`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tickets` (
  `ticket_id` int NOT NULL AUTO_INCREMENT,
  `ticket_number` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_by` int NOT NULL,
  `assigned_to` int DEFAULT NULL,
  `category_id` int NOT NULL,
  `incident_id` int DEFAULT NULL,
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `priority` enum('LOW','MEDIUM','HIGH','URGENT') COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` enum('OPEN','ASSIGNED','IN_PROGRESS','RESOLVED','CLOSED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`ticket_id`),
  UNIQUE KEY `ticket_number` (`ticket_number`),
  KEY `idx_tickets_status` (`status`),
  KEY `idx_tickets_assigned_to` (`assigned_to`),
  KEY `idx_tickets_created_by` (`created_by`),
  KEY `idx_tickets_category_id` (`category_id`),
  KEY `idx_tickets_incident_id` (`incident_id`),
  CONSTRAINT `fk_tickets_assigned_to` FOREIGN KEY (`assigned_to`) REFERENCES `users` (`user_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_tickets_category` FOREIGN KEY (`category_id`) REFERENCES `categories` (`category_id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_tickets_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`user_id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_tickets_incident` FOREIGN KEY (`incident_id`) REFERENCES `incidents` (`incident_id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tickets`
--

LOCK TABLES `tickets` WRITE;
/*!40000 ALTER TABLE `tickets` DISABLE KEYS */;
INSERT INTO `tickets` VALUES (1,'HD-1001',4,9,1,1,'Library 2nd Floor Wi-Fi Disconnected','Cannot connect to the university student Wi-Fi network from the second floor quiet study area.','HIGH','CLOSED','2026-10-02 09:15:00','2026-10-21 10:28:06'),(2,'HD-1002',5,3,1,1,'Science Building Dorm Wi-Fi Dropping','Wi-Fi signal keeps dropping out in Dorm Block C every few minutes.','HIGH','IN_PROGRESS','2026-10-02 09:20:00','2026-10-21 10:26:47'),(3,'HD-1003',6,2,1,1,'Student Center Wi-Fi Authentication Failure','Receiving an authentication error when entering university credentials at the student center.','URGENT','RESOLVED','2026-10-02 09:25:00','2026-10-20 19:52:37'),(4,'HD-1004',4,3,2,NULL,'Missing Midterm Exam Hall Ticket','The student portal does not show the hall ticket link for upcoming biology midterm examinations.','URGENT','RESOLVED','2026-10-03 08:30:00','2026-10-03 11:45:00'),(5,'HD-1005',7,2,4,NULL,'Tuition Fee Receipt Not Generated','Semester 1 tuition fee payment completed through bank gateway but receipt not listed under student account.','MEDIUM','CLOSED','2026-10-01 10:00:00','2026-10-01 16:30:00'),(6,'HD-1006',5,3,7,2,'Hall B102 Projector Display Fault','Projector lamp blinks orange and screen is blank during scheduled morning seminar.','MEDIUM','RESOLVED','2026-10-02 10:05:00','2026-10-21 10:26:42'),(7,'HD-1007',6,3,5,NULL,'Route 4 Campus Shuttle Delayed','Morning shuttle bus for Route 4 was 35 minutes late without prior notification.','LOW','RESOLVED','2026-10-03 07:45:00','2026-10-03 09:15:00'),(8,'HD-1008',8,NULL,1,NULL,'sample ticket 1','there is no issue simply pass the ticket','MEDIUM','OPEN','2026-10-18 21:31:40','2026-10-18 21:31:40');
/*!40000 ALTER TABLE `tickets` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `user_id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password_hash` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `role` enum('STUDENT','AGENT','ADMIN') COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'Admin User','admin@college.example','$2a$10$p3sxqrfrmATxOsVlnHRckuf9OFZ1d5uvF/xxSZPLjuj5oqHr./.E6','ADMIN','2026-10-01 08:00:00'),(2,'Alex Rivera','agent1@college.example','$2a$10$p3sxqrfrmATxOsVlnHRckuf9OFZ1d5uvF/xxSZPLjuj5oqHr./.E6','AGENT','2026-10-01 08:05:00'),(3,'Jordan Lee','agent2@college.example','$2a$10$p3sxqrfrmATxOsVlnHRckuf9OFZ1d5uvF/xxSZPLjuj5oqHr./.E6','AGENT','2026-10-01 08:10:00'),(4,'Sam Taylor','student1@college.example','$2a$10$p3sxqrfrmATxOsVlnHRckuf9OFZ1d5uvF/xxSZPLjuj5oqHr./.E6','STUDENT','2026-10-01 09:00:00'),(5,'Morgan Chen','student2@college.example','$2a$10$p3sxqrfrmATxOsVlnHRckuf9OFZ1d5uvF/xxSZPLjuj5oqHr./.E6','STUDENT','2026-10-01 09:05:00'),(6,'Casey Patel','student3@college.example','$2a$10$p3sxqrfrmATxOsVlnHRckuf9OFZ1d5uvF/xxSZPLjuj5oqHr./.E6','STUDENT','2026-10-01 09:10:00'),(7,'Riley Brooks','student4@college.example','$2a$10$p3sxqrfrmATxOsVlnHRckuf9OFZ1d5uvF/xxSZPLjuj5oqHr./.E6','STUDENT','2026-10-01 09:15:00'),(8,'Jane','jane@gmail.com','$2a$10$3ntGhPm2.7b9ydbV6FKqTuMUMJWlNLY9EMvz3f8Adi2YwZEYHjWfO','STUDENT','2026-10-18 21:31:02'),(9,'bob','bob@gmail.com','$2a$10$tmEm7S3W/2IvaVIvSB3ZbOA2Nc44yiP.LMoUmaCCpMbHiGWsFStsq','AGENT','2026-10-18 21:34:23');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-21 12:12:17
