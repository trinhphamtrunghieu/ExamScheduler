import csv
import datetime

# Define course times and the allowed course start times
allowed_start_times = ["08:00:00", "10:00:00", "13:00:00", "15:00:00", "16:30:00"]

# Convert time string to datetime object for easy comparison (now considering date as well)
def convert_to_datetime(date_str, time_str):
    return datetime.datetime.strptime(date_str + " " + time_str, "%Y-%m-%d %H:%M:%S")

def check_course_duplication(schedule: list):
    course_set = set()
    for course in schedule:
        if course['course_name'] in course_set:
            return False
        course_set.add(course['course_name'])
    return True

def check_course_existence(schedule: list, subjects: list):
    course_set = set()
    invalid = True
    for course in subjects:
        course_set.add(course['course_name'])
    for subject in schedule:
        if subject['course_name'] not in course_set:
            print("Course {subject['course_name']} is absent")
            invalid = False
    return invalid

def check_course_duration(schedule: list, subjects: list):
    course_map = dict()
    invalid = True
    for course in subjects:
        course_map[course['course_name']] = course['course_name']
    for subject in schedule:
        if course_map[subject['course_name']] != subject['duration']:
            print("Course {subject['course_name']} has invalid duration")
            invalid = False
    return invalid

# Check if a course overlaps
def check_overlap(course1, course2):
    # Convert start and end times to datetime objects
    course1_start = convert_to_datetime(course1['date_start'], course1['start_time'])
    course1_end = convert_to_datetime(course1['date_start'], course1['end_time'])
    course2_start = convert_to_datetime(course2['date_start'], course2['start_time'])
    course2_end = convert_to_datetime(course2['date_start'], course2['end_time'])
    
    # Check if courses overlap
    return (course1_start < course2_end and course1_end > course2_start)

# Validate the course schedule
def validate_schedule(courses, registrations, subjects):
    # Create a dictionary to store each course's details
    if not check_course_duplication(courses):
        print("Course duplicated")
        return False
    
    if not check_course_existence(courses, subjects):
        print("Course absent")
        return False

    # if not check_course_duration(courses, subjects):
    #     print("Course duration invalid")
    #     return False

    course_details = {}
    
    # Check if there are courses that start or end between 12 PM and 1 PM
    for course in courses:
        start_time = convert_to_datetime(course['date_start'], course['start_time'])
        end_time = convert_to_datetime(course['date_start'], course['end_time'])
        
        # Check for restricted time (12 PM - 1 PM)
        if (start_time >= convert_to_datetime(course['date_start'], "12:00:00") and start_time < convert_to_datetime(course['date_start'], "13:00:00")) or \
           (end_time > convert_to_datetime(course['date_start'], "12:00:00") and end_time <= convert_to_datetime(course['date_start'], "13:00:00")):
            print(f"Course {course['course_name']} starts or ends between 12 PM and 1 PM.")
            return False
        
        # Check if the course start time is within the allowed list
        if course['start_time'] not in allowed_start_times:
            print(f"Course {course['course_name']} has an invalid start time. {course['start_time']}")
            return False

        # Store the course details by course_id
        course_details[course['course_name']] = course

    # Check if any student has overlapping courses
    student_courses = {}
    for registration in registrations:
        student_id, course_id, course_name = registration
        if student_id not in student_courses:
            student_courses[student_id] = []
        student_courses[student_id].append(course_name)
    print(f"{student_courses}")
    # Check for course overlaps for each student
    for student_id, courses in student_courses.items():
        # Check if any student has overlapping courses
        for i in range(len(courses)):
            for j in range(i + 1, len(courses)):
                course1_id = courses[i]
                course2_id = courses[j]
                course1 = course_details[course1_id]
                course2 = course_details[course2_id]
                
                if check_overlap(course1, course2):
                    print(f"Student {student_id} has overlapping courses: {course1_id} and {course2_id}")
                    return False
                
    print("Schedule is valid!")
    return True

# Function to read courses from a CSV file
def read_courses_from_file(file_name):
    courses = []
    with open(file_name, mode='r', encoding='utf-8') as file:
        csv_reader = csv.reader(file)
        i = 0
        for row in csv_reader:
            if i == 0: 
                i += 1
                continue
            courses.append({
                'course_id': row[0],
                'course_name': row[1],
                'date_start': row[2],
                'start_time': row[3],
                'end_time': row[4],
                'duration': row[5]
            })
    return courses

# Function to read registrations from a CSV file
def read_registrations_from_file(file_name):
    registrations = []
    with open(file_name, mode='r') as file:
        csv_reader = csv.reader(file)
        for row in csv_reader:
            student_id = row[0]
            course_id = row[1]
            course_name = row[2]
            registrations.append((student_id, course_id, course_name))
    return registrations

def read_subjects_from_file(file_name):
    subjects = []
    with open(file_name, mode='r', encoding='utf-8') as file:
        csv_reader = csv.reader(file)
        i = 0
        for row in csv_reader:
            if i == 0: 
                i += 1
                continue
            subjects.append({
                'course_id': row[0],
                'course_name': row[1],
                'professor': row[2],
                'start_date': row[3],
                'end_date': row[4],
                'duration': row[5]
            })
    return subjects

# File paths
courses_file = '/home/lap14604/Downloads/UIT/do_an/exam-schedule-generator/schedule.csv'
registrations_file = '/home/lap14604/Downloads/UIT/do_an/exam-schedule-generator/registrations2.csv'
subjects_file = "/home/lap14604/Downloads/UIT/do_an/exam-schedule-generator/subjects.csv"

# Read data from files
courses = read_courses_from_file(courses_file)
registrations = read_registrations_from_file(registrations_file)
subjects = read_subjects_from_file(subjects_file)

# Run the validation
validate_schedule(courses, registrations, subjects)
