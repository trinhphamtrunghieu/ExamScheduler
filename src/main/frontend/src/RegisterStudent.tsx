import { useState, useEffect } from "react";
import { API_BASE } from "./common.tsx";
import NavBar from "./NavBar.tsx";

function RegisterStudent() {
  const [studentId, setStudentId] = useState("");
  const [courses, setCourses] = useState([]);
  const [selectedCourses, setSelectedCourses] = useState([]);
  const [isStudent, setIsStudent] = useState(false); // Track if the user is a student

  // Fetch courses from the backend
  useEffect(() => {

    // Check if the logged-in user is a student and get their student ID
    fetch(`${API_BASE}/auth/session`, {
      credentials: "include", // Send session cookies for role check
    })
      .then((res) => res.json())
      .then((data) => {
        if (data.role === "STUDENT") {
          setIsStudent(true);
          setStudentId(data.studentId); // Set the student ID from session or backend
        }
      })
      .catch((error) => {
        console.error("Error checking user role:", error);
      });
    fetch(`${API_BASE}/subjects`, {
        credentials: "include", // Send session cookies for role check
        })
      .then((res) => res.json())
      .then((data) => setCourses(data))
      .catch((error) => console.error("Error fetching courses:", error));
  }, []);

  // Toggle course selection
  const toggleCourseSelection = (courseId) => {
    setSelectedCourses((prev) =>
      prev.includes(courseId)
        ? prev.filter((id) => id !== courseId)
        : [...prev, courseId]
    );
  };

  // Select all courses
  const selectAllCourses = () => {
    setSelectedCourses(courses.map((course) => course.maMonHoc));
  };

  // Deselect all courses
  const deselectAllCourses = () => {
    setSelectedCourses([]);
  };

  // Handle form submission
  const handleSubmit = (e) => {
    e.preventDefault();
    if (!studentId || selectedCourses.length === 0) {
      alert("Please enter a student ID and select at least one course.");
      return;
    }

    const requestBody = {
      maSinhVien: studentId,
      courseIds: selectedCourses,
    };

    fetch(`${API_BASE}/registrations/register`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(requestBody),
    })
      .then((res) => res.json())
      .then((data) => alert("Registration successful!"))
      .catch((error) => console.error("Error registering student:", error));
  };

  return (
    <div id="root">
      <NavBar />
      <div className="spacing" > </div>
      <div className="content-area">
        <h2 className="text-3xl font-semibold text-indigo-600 mb-6">Register Student for Courses</h2>

        <form onSubmit={handleSubmit} className="w-full max-w-md space-y-4">
          <label htmlFor="studentId" className="text-sm font-semibold text-gray-700">Student ID</label>
          <input
            type="text"
            id="studentId"
            placeholder="Enter Student ID"
            required
            value={studentId}
            onChange={(e) => setStudentId(e.target.value)}
            disabled={isStudent} // Disable the input if the user is a student
            className="w-full p-2 border rounded"
          />

          <div className="course-list">
            <h3 className="text-lg font-semibold mb-4">Select Courses:</h3>
            <button
              type="button"
              onClick={selectAllCourses}
              className="select-all"
            >
              Select All
            </button>
            <button
              type="button"
              onClick={deselectAllCourses}
              className="deselect-all"
            >
              Deselect All
            </button>

            <ul className="mt-4 space-y-4">
              {courses.map((course) => (
                <li
                  key={course.maMonHoc}
                  className="flex items-center space-x-3 cursor-pointer"
                  onClick={() => toggleCourseSelection(course.maMonHoc)} // Add click handler to the entire list item
                >
                  <input
                    type="checkbox"
                    id={`course-${course.maMonHoc}`} // Unique ID for the checkbox
                    checked={selectedCourses.includes(course.maMonHoc)}
                    onChange={() => toggleCourseSelection(course.maMonHoc)} // Keeps checkbox toggle functionality
                    className="w-5 h-5"
                  />
                  <label
                    htmlFor={`course-${course.maMonHoc}`} // Associate the label with the checkbox
                    className="text-sm font-medium"
                  >
                    {course.maMonHoc} - {course.tenMonHoc}
                  </label>
                </li>
              ))}
            </ul>
          </div>

          <button
            type="submit"
            className="w-full p-2 bg-blue-500 text-white rounded"
          >
            Register
          </button>
        </form>
      </div>
    </div>
  );
}

export default RegisterStudent;
