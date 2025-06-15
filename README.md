# Exam Scheduler Guide

## 1. How to Get Artifact

1. Access [GitHub Actions](https://github.com/TPTHieuUIT/ExamScheduler/actions).
2. Select the latest successful job (look for the job with a green tick, e.g., [Example Job](https://github.com/TPTHieuUIT/ExamScheduler/actions/runs/13162041340)).
3. Scroll to the **Artifacts** section and download it.

---

## 2. How to Run

### Requirements
- JDK 17 or higher
- MySQL

### Steps to Run

1. **Download and Unzip Artifact**:
   - Download the artifact from GitHub Actions and unzip it.

2. **Move to Artifact Folder**:
   - Navigate to the unzipped artifact folder (e.g., `Java Project`).
   - 
3. **Run the Application**:
   - Execute the following command to start the application:
     ```bash
     <path_to_java> -jar doan-0.0.1-SNAPSHOT.jar
     ```
     Example:
     ```bash
     /usr/java/jdk-17-oracle-x64/bin/java -jar doan-0.0.1-SNAPSHOT.jar
     ```

4. **Wait for Logs**:
   - Look for the following logs to confirm the application has started:
     ```log
     o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port 8080 (http) with context path '/'
     com.doan.DoanApplication                 : Started DoanApplication in ....... seconds
     ```

5. **Access the Application**:
   - Open your browser and go to: [http://localhost:8080/](http://localhost:8080/).


NOTE
1. data saved as data.csv in the same folder with script. Import data via "Cài đặt" function after logged in as professor
2. data periodically flush to disk
3. re-import data will invalidate all current data
