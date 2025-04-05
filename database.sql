-- MySQL dump 10.13  Distrib 8.4.0, for Linux (x86_64)
--
-- Host: localhost    Database: quan_ly_lich_thi
-- ------------------------------------------------------
-- Server version	8.4.0

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
-- Current Database: `quan_ly_lich_thi`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `quan_ly_lich_thi` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `quan_ly_lich_thi`;

--
-- Table structure for table `dang_ky`
--

DROP TABLE IF EXISTS `dang_ky`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dang_ky` (
  `ma_sinh_vien` varchar(10) NOT NULL,
  `ma_mon_hoc` varchar(20) NOT NULL,
  `ten_mon_hoc` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ma_sinh_vien`,`ma_mon_hoc`),
  KEY `ma_mon_hoc` (`ma_mon_hoc`),
  CONSTRAINT `dang_ky_ibfk_1` FOREIGN KEY (`ma_sinh_vien`) REFERENCES `sinh_vien` (`ma_sinh_vien`) ON DELETE CASCADE,
  CONSTRAINT `dang_ky_ibfk_2` FOREIGN KEY (`ma_mon_hoc`) REFERENCES `mon_hoc` (`ma_mon_hoc`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `lich_thi`
--

DROP TABLE IF EXISTS `lich_thi`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lich_thi` (
  `phong_thi` varchar(20) NOT NULL,
  `ngay_thi` date NOT NULL,
  `gio_thi` time NOT NULL,
  `ten_mon_hoc` varchar(255) NOT NULL,
  PRIMARY KEY (`ten_mon_hoc`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mon_hoc`
--

DROP TABLE IF EXISTS `mon_hoc`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mon_hoc` (
  `ma_mon_hoc` varchar(20) NOT NULL,
  `ten_gv_dung_lop` varchar(100) NOT NULL,
  `ngay_bat_dau` date NOT NULL,
  `ngay_ket_thuc` date NOT NULL,
  `thoi_luong_thi` int NOT NULL,
  `ten_mon_hoc` varchar(255) NOT NULL,
  PRIMARY KEY (`ma_mon_hoc`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sinh_vien`
--

DROP TABLE IF EXISTS `sinh_vien`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sinh_vien` (
  `ma_sinh_vien` varchar(10) NOT NULL,
  `ten_sinh_vien` varchar(100) NOT NULL,
  PRIMARY KEY (`ma_sinh_vien`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-04-05  9:04:32
