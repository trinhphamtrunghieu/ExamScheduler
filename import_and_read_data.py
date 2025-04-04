from datetime import date

import mysql.connector
import pandas as pd

# === CONFIG ===
CSV_PATH = 'class_data.csv'
DB_CONFIG = {
    'host': 'localhost',
    'user': 'root',
    'password': 'password',  # change this
    'database': 'quan_ly_lich_thi'
}

# === FIXED DATES ===
ngay_bat_dau = date(2025, 1, 1)
ngay_ket_thuc = date(2025, 3, 1)

# === STEP 1: Read CSV ===
df = pd.read_csv(CSV_PATH)

# === STEP 2: Extract and clean data ===
students = set()
subjects = set()
registrations = set()

for _, row in df.iterrows():
    ma_sinh_vien = str(row['MSSV']).strip()
    ten_sinh_vien = str(row['Họ tên']).strip()

    ma_mon_hoc = str(row['Mã lớp học']).strip()[:20]  # use as class code
    ten_mon_hoc = str(row['Môn học']).strip()
    ten_gv = str(row['Giáo viên']).strip()

    # Add to sets
    students.add((ma_sinh_vien, ten_sinh_vien))
    subjects.add((ma_mon_hoc, ten_gv, ngay_bat_dau, ngay_ket_thuc, 90, ten_mon_hoc))
    registrations.add((ma_sinh_vien, ma_mon_hoc))

# === STEP 3: Insert into MySQL ===
conn = mysql.connector.connect(**DB_CONFIG)
cursor = conn.cursor()

# Disable foreign key checks temporarily
cursor.execute("SET FOREIGN_KEY_CHECKS = 0")

# Insert students
for mssv, ten_sv in students:
    cursor.execute("""
        INSERT IGNORE INTO sinh_vien (ma_sinh_vien, ten_sinh_vien)
        VALUES (%s, %s)
    """, (mssv, ten_sv))

# Insert subjects
for ma_mh, gv, start, end, duration, ten_mh in subjects:
    cursor.execute("""
        INSERT IGNORE INTO mon_hoc (ma_mon_hoc, ten_gv_dung_lop, ngay_bat_dau, ngay_ket_thuc, thoi_luong_thi, ten_mon_hoc)
        VALUES (%s, %s, %s, %s, %s, %s)
    """, (ma_mh, gv, start, end, duration, ten_mh))

# Insert registrations
for mssv, ma_mh in registrations:
    cursor.execute("""
        INSERT IGNORE INTO dang_ky (ma_sinh_vien, ma_mon_hoc)
        VALUES (%s, %s)
    """, (mssv, ma_mh))

# Commit and cleanup
conn.commit()
cursor.close()
conn.close()

print("✅ Data imported with fixed dates (01/01/2025 → 01/03/2025)!")
