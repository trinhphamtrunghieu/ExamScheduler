CREATE DATABASE  IF NOT EXISTS `quan_ly_lich_thi` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `quan_ly_lich_thi`;
-- MySQL dump 10.13  Distrib 8.0.40, for Win64 (x86_64)
--
-- Host: localhost    Database: quan_ly_lich_thi
-- ------------------------------------------------------
-- Server version	8.0.40

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `dang_ky`
--

DROP TABLE IF EXISTS `dang_ky`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dang_ky` (
  `ma_sinh_vien` varchar(10) NOT NULL,
  `ma_mon_hoc` varchar(20) NOT NULL,
  PRIMARY KEY (`ma_sinh_vien`,`ma_mon_hoc`),
  KEY `ma_mon_hoc` (`ma_mon_hoc`),
  CONSTRAINT `dang_ky_ibfk_1` FOREIGN KEY (`ma_sinh_vien`) REFERENCES `sinh_vien` (`ma_sinh_vien`) ON DELETE CASCADE,
  CONSTRAINT `dang_ky_ibfk_2` FOREIGN KEY (`ma_mon_hoc`) REFERENCES `mon_hoc` (`ma_mon_hoc`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dang_ky`
--

LOCK TABLES `dang_ky` WRITE;
/*!40000 ALTER TABLE `dang_ky` DISABLE KEYS */;
INSERT INTO `dang_ky` VALUES ('21210001','BIGDATA401'),('23410048','BIGDATA401'),('23210156','CAL202'),('23730078','CAL202'),('23730101','CAL202'),('23730127','CAL202'),('23730135','CAL202'),('21730011','COMP202'),('22210127','COMP202'),('22210137','COMP202'),('22210177','COMP202'),('22730031','COMP202'),('23210313','COMP202'),('23410037','COMP202'),('23730024','COMP202'),('23730045','COMP202'),('24410080','COMP202'),('22210154','CS101'),('23210087','CS101'),('23210144','CS101'),('23210155','CS101'),('23210163','CS101'),('23210174','CS101'),('23210215','CS101'),('23210252','CS101'),('23210258','CS101'),('23210295','CS101'),('23410008','CS101'),('23410024','CS101'),('23410059','CS101'),('23410067','CS101'),('23410070','CS101'),('23410096','CS101'),('23410118','CS101'),('23410143','CS101'),('23410162','CS101'),('23410183','CS101'),('23730093','CS101'),('23730153','CS101'),('23730160','CS101'),('23730169','CS101'),('23730193','CS101'),('23730198','CS101'),('23730199','CS101'),('23730217','CS101'),('24210061','CS101'),('21730011','DB202'),('22210130','DB202'),('22210154','DB202'),('22210168','DB202'),('23210087','DB202'),('23210144','DB202'),('23210174','DB202'),('23410024','DB202'),('23410087','DB202'),('23410166','DB202'),('22210133','DISMATH201'),('22210137','DISMATH201'),('22210177','DISMATH201'),('22730016','DISMATH201'),('23210051','DISMATH201'),('23210062','DISMATH201'),('23210204','DISMATH201'),('23210242','DISMATH201'),('23210299','DISMATH201'),('23210313','DISMATH201'),('23410037','DISMATH201'),('23410063','DISMATH201'),('23410070','DISMATH201'),('23410087','DISMATH201'),('23410096','DISMATH201'),('23410133','DISMATH201'),('23730143','DISMATH201'),('21730015','DSALG202'),('22210137','DSALG202'),('22210177','DSALG202'),('23210087','DSALG202'),('23210124','DSALG202'),('23210204','DSALG202'),('23210312','DSALG202'),('23410008','DSALG202'),('23410028','DSALG202'),('23410039','DSALG202'),('23410058','DSALG202'),('23410059','DSALG202'),('23410067','DSALG202'),('23410068','DSALG202'),('23410070','DSALG202'),('23410079','DSALG202'),('23410080','DSALG202'),('23410085','DSALG202'),('23410087','DSALG202'),('23410094','DSALG202'),('23410096','DSALG202'),('23410111','DSALG202'),('23410114','DSALG202'),('23410135','DSALG202'),('23410136','DSALG202'),('23410138','DSALG202'),('23730065','DSALG202'),('23730070','DSALG202'),('23730081','DSALG202'),('23730086','DSALG202'),('23730117','DSALG202'),('21730015','ENG201'),('22210183','ERP302'),('23410048','ERP302'),('23730041','MATH201'),('23730101','MATH201'),('23730109','MATH201'),('23730135','MATH201'),('24410121','MATH201'),('23210051','NET101'),('23210087','NET101'),('23410024','NET101'),('24210094','NET101'),('24410080','NET101'),('24410109','NET101'),('22210127','OOP201'),('22210137','OOP201'),('22210177','OOP201'),('22730023','OOP201'),('23210065','OOP201'),('23210087','OOP201'),('23210124','OOP201'),('23210299','OOP201'),('23210312','OOP201'),('23410037','OOP201'),('23410068','OOP201'),('23410085','OOP201'),('23730081','OOP201'),('23730117','OOP201'),('24410080','OOP201'),('24410109','OOP201'),('22730032','OS201'),('22730043','OS201'),('22730089','OS201'),('22730094','OS201'),('23730027','OS201'),('22210154','SEC101'),('23410039','SEC101'),('23410048','SEC101'),('22210137','SOCMED402'),('23410007','SOCMED402'),('23410039','SOCMED402'),('23210156','STAT201'),('23730023','STAT201'),('22210087','WEB302'),('22210127','WEB302'),('22730016','WEB302'),('23410008','WEB302');
/*!40000 ALTER TABLE `dang_ky` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `lich_thi`
--

DROP TABLE IF EXISTS `lich_thi`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lich_thi` (
  `ma_mon_hoc` varchar(20) NOT NULL,
  `phong_thi` varchar(20) NOT NULL,
  `ngay_thi` date NOT NULL,
  `gio_thi` time NOT NULL,
  PRIMARY KEY (`ma_mon_hoc`,`ngay_thi`),
  CONSTRAINT `lich_thi_ibfk_1` FOREIGN KEY (`ma_mon_hoc`) REFERENCES `mon_hoc` (`ma_mon_hoc`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `lich_thi`
--

LOCK TABLES `lich_thi` WRITE;
/*!40000 ALTER TABLE `lich_thi` DISABLE KEYS */;
/*!40000 ALTER TABLE `lich_thi` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `mon_hoc`
--

LOCK TABLES `mon_hoc` WRITE;
/*!40000 ALTER TABLE `mon_hoc` DISABLE KEYS */;
INSERT INTO `mon_hoc` VALUES ('BIGDATA401','Nguyễn Thị Huỳnh Như','2025-01-13','2025-03-14',60,'Công nghệ phân tích dữ liệu lớn'),('BPM301','Hồ Hải','2025-01-13','2025-03-14',90,'Hệ thống quản trị qui trình nghiệp vụ'),('CAL202','Lê Hoàng Tuấn','2025-01-13','2025-03-14',60,'Giải tích'),('COMP202','Mai Xuân Hùng','2025-01-13','2025-03-14',90,'Tổ chức và cấu trúc máy tính II'),('CS101','Phan Đình Duy','2025-01-13','2025-03-14',60,'Nhập môn Lập trình'),('DB202','Cáp Phạm Đình Thăng','2025-01-13','2025-03-14',60,'Cơ sở dữ liệu'),('DISMATH201','Lê Hoàng Tuấn','2025-01-13','2025-03-14',90,'Cấu trúc rời rạc'),('DSALG202','Nguyễn Hồ Duy Trí','2025-01-13','2025-03-14',90,'Cấu trúc dữ liệu và giải thuật'),('ECOM301','Lê Hoàng Tuấn','2025-01-13','2025-03-14',60,'Thương mại điện tử'),('ENG101','Phạm Thế Sơn','2025-01-13','2025-03-14',60,'Anh văn 1'),('ENG201','Mai Xuân Hùng','2025-01-13','2025-03-14',90,'Anh văn 2'),('ENG301','Nguyễn Hồ Duy Trí','2025-01-13','2025-03-14',90,'Anh văn 3'),('ERP302','Nguyễn Hồ Duy Trí','2025-01-13','2025-03-14',90,'Hoạch định nguồn lực doanh nghiệp'),('GIS301','Nguyễn Hồ Duy Trí','2025-01-13','2025-03-14',60,'Hệ thống thông tin địa lý 3 chiều'),('HCM202','Lê Hoàng Tuấn','2025-01-13','2025-03-14',90,'Tư tưởng Hồ Chí Minh'),('HIS203','Phan Đình Duy','2025-01-13','2025-03-14',60,'Lịch sử Đảng Cộng sản Việt Nam'),('INFOMGT302','Lê Hoàng Tuấn','2025-01-13','2025-03-14',90,'Quản lý thông tin'),('INFRA301','Phạm Thế Sơn','2025-01-13','2025-03-14',60,'Cơ sở hạ tầng công nghệ thông tin'),('IT101','Sử Nhật Hạ','2025-01-13','2025-03-14',60,'Giới thiệu ngành Công nghệ Thông tin'),('JAVA201','Mai Xuân Hùng','2025-01-13','2025-03-14',60,'Công nghệ Java'),('LAW101','Phạm Thế Sơn','2025-01-13','2025-03-14',60,'Pháp luật đại cương'),('MATH201','Cáp Phạm Đình Thăng','2025-01-13','2025-03-14',60,'Đại số tuyến tính'),('NET101','Mai Xuân Hùng','2025-01-13','2025-03-14',90,'Nhập môn mạng máy tính'),('OOP201','Hà Lê Hoài Trung','2025-01-13','2025-03-14',90,'Lập trình hướng đối tượng'),('OS201','Cáp Phạm Đình Thăng','2025-01-13','2025-03-14',90,'Hệ điều hành'),('PHIL201','Lê Hoàng Tuấn','2025-01-13','2025-03-14',90,'Triết học Mác – Lênin'),('PYTHON202','Nguyễn Hồ Duy Trí','2025-01-13','2025-03-14',90,'Kỹ thuật lập trình Python'),('SEC101','Cáp Phạm Đình Thăng','2025-01-13','2025-03-14',60,'Nhập môn bảo đảm và an ninh thông tin'),('SKILL101','Lê Hoàng Tuấn','2025-01-13','2025-03-14',90,'Kỹ năng nghề nghiệp'),('SOCMED402','Phan Đình Duy','2025-01-13','2025-03-14',60,'Khai thác dữ liệu truyền thông xã hội'),('SOCNET301','Sử Nhật Hạ','2025-01-13','2025-03-14',60,'Mạng xã hội'),('STAT201','Phan Đình Duy','2025-01-13','2025-03-14',90,'Xác suất thống kê'),('UID301','Mai Xuân Hùng','2025-01-13','2025-03-14',90,'Thiết kế giao diện người dùng'),('WEB302','Sử Nhật Hạ','2025-01-13','2025-03-14',90,'Internet và công nghệ Web');
/*!40000 ALTER TABLE `mon_hoc` ENABLE KEYS */;
UNLOCK TABLES;

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

--
-- Dumping data for table `sinh_vien`
--

LOCK TABLES `sinh_vien` WRITE;
/*!40000 ALTER TABLE `sinh_vien` DISABLE KEYS */;
INSERT INTO `sinh_vien` VALUES ('21210001','Phạm Minh Cường'),('21410060','Triệu Khánh Huy'),('21730011','Nguyễn Hữu Vương'),('21730015','Đinh Quốc Chính'),('22210087','Nguyễn Nhật Thùy Trân'),('22210127','Nguyễn Phước Khánh Huy'),('22210130','Đoàn Huy Khả'),('22210133','Nguyễn Ngọc Minh Khôi'),('22210137','Trần Tú Nguyên Linh'),('22210154','Hồ Thị Thanh Thanh'),('22210165','Trần Thị Huyền Trang'),('22210168','Đào Thị Việt Trinh'),('22210177','Nguyễn Thị Bích Vân'),('22210183','Hà Như Ý'),('22410003','Nguyễn Văn Chánh'),('22730016','Võ Trường Sinh'),('22730023','Trần Lê Phú An'),('22730031','Nguyễn Minh Hiển'),('22730032','Trịnh Phạm Trung Hiếu'),('22730041','Lê Trọng Nghĩa'),('22730043','Nguyễn Quốc Nhứt'),('22730045','Lê Chiêu Sĩ'),('22730089','Võ Tấn Phát'),('22730094','Trần Hữu Tài'),('22730096','Nguyễn Thanh Thịnh'),('23210001','Phạm Thanh An'),('23210002','Nguyễn Đức Anh'),('23210010','Lương Quế Chi'),('23210012','Nguyễn Mạnh Cường'),('23210037','Hàng Nguyên Hưng'),('23210051','Trần Hoài Nam'),('23210060','Bùi Khánh Sơn'),('23210062','Nguyễn Hoàng Sơn'),('23210065','Nguyễn Viết Thắng'),('23210087','Huỳnh Thanh Tùng'),('23210124','Nguyễn Anh Khoa'),('23210144','Lê Bình Phú'),('23210155','Lê Nguyên Thanh Thảo'),('23210156','Lê Thị Thu Thảo'),('23210163','Lê Trần Minh Thư'),('23210174','Nguyễn Công Tuấn'),('23210204','Đoàn Minh Đạt'),('23210215','Nguyễn Đắc Khánh Duy'),('23210242','Trịnh Thế Long'),('23210252','Nguyễn Trọng Nghĩa'),('23210258','Nguyễn Hữu Nhật'),('23210295','Nguyễn Thị Bích Trâm'),('23210299','Nguyễn Văn Minh Trí'),('23210312','Tô Xuân Tùng'),('23210313','Nguyễn Thị Thanh Tuyền'),('23410007','Dương Tấn Đạt'),('23410008','Lê Thành Đạt'),('23410012','Lê Khả Hân'),('23410013','Nguyễn Trung Hào'),('23410024','Đàm Mỹ Linh'),('23410028','Nguyễn Tấn Miêu'),('23410037','Nguyễn Trọng Tài'),('23410039','Trần Thế Tân'),('23410046','Phạm Trung Tính'),('23410048','Trần Hoàng Tú'),('23410049','Nguyễn Hoàng Tuấn'),('23410058','Bùi Lê Quốc Bảo'),('23410059','Nguyễn Hoàng Bảo'),('23410063','Đàm Đình Cường'),('23410067','Thái Hoa Hoàng Diệu'),('23410068','Hoàng Phan Kim Đức'),('23410070','Dương Minh Duy'),('23410079','Hồ Phạm Sĩ Hưng'),('23410080','Ngô Quốc Hùng'),('23410085','Dương Duy Khánh'),('23410087','Nguyễn Đăng Khoa'),('23410094','Huỳnh Công Tôn Khải Minh'),('23410096','Nguyễn Thị Kim Ngân'),('23410111','Nguyễn Minh Quang'),('23410114','Lâm Nhựt Tân'),('23410118','Vũ Đức Thiện'),('23410133','Trần Lê Anh Tuấn'),('23410135','Nguyễn Cao Vĩ'),('23410136','Nguyễn Quốc Việt'),('23410138','Đào Huệ Quốc Vinh'),('23410143','Lê Phan Thanh Bình'),('23410162','Vũ Duy Khánh'),('23410166','Dương Diệu Linh'),('23410183','Tống Minh Sang'),('23730023','Võ Bằng Kiều'),('23730024','Lê Hoàng Kim'),('23730027','Nguyễn Thị Yến Linh'),('23730034','Nguyễn Trần Ngọc Ngọc'),('23730041','Dương Thanh Quí'),('23730045','Châu Ngọc Thắng'),('23730050','Trần Huỳnh Thiện'),('23730065','Trần Lê Tuấn Anh'),('23730070','Võ Thái Bảo'),('23730071','Dương Thị Quỳnh Châm'),('23730078','Lâm Thanh Đức'),('23730081','Nguyễn Phúc Bảo Duy'),('23730086','Nguyễn Tân Hoàng'),('23730093','Trịnh Hà Viết Huy'),('23730101','Phan Tài Lộc'),('23730109','Đặng Bảo Ngọc'),('23730117','Nguyễn Thuỵ Nghi Quân'),('23730127','Trần Công Thành'),('23730135','Nguyễn Nhật Thùy Trinh'),('23730143','Nguyễn Phi Hùng'),('23730153','Trần Duy Tuấn Anh'),('23730160','Nguyễn Thành Danh'),('23730162','Hà Nguyễn Trường Giang'),('23730169','Hoàng Văn Hoàng'),('23730193','Nguyễn Hoàng Kim Ngân'),('23730198','Phạm Phương Hồng Ngữ'),('23730199','Hứa Gia Nguyễn'),('23730217','Nguyễn Hữu Tín'),('23730231','Thạch Bảo Trọng'),('24210061','Nguyễn Minh Nhựt'),('24210094','Trần Ngọc Thiên Trúc'),('24410052','Nguyễn Thành Đăng Khoa'),('24410057','Trần Tuấn Kiệt'),('24410080','Phùng Kim Phát'),('24410106','Nguyễn Trọng Thái'),('24410108','Phạm Tiến Thành'),('24410109','Nguyễn Thị Thu Thảo'),('24410121','Đỗ Quang Tùng'),('24410122','Đỗ Võ Triệu Vĩ'),('24730083','Bùi Lâm Đồng'),('24730160','Nguyễn Hoàng Khanh'),('24730161','Trần Thế Mạnh'),('24730162','Đinh Đức Tâm'),('24730163','Phan Mạnh Phát'),('24730164','Võ Thành Phương'),('24730165','Lê Hoàng Đức Huy'),('24730166','Phạm Thanh Trúc'),('24730167','Đoàn Đức Huy');
/*!40000 ALTER TABLE `sinh_vien` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-01-13 17:21:54
