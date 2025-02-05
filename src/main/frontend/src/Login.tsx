import { useState } from "react";
import { useUser } from "./UserContext";
import { useNavigate } from "react-router-dom";
import { API_BASE } from "./common";

function Login() {
  const { login } = useUser();
  const [studentId, setStudentId] = useState("");
  const [professorId, setProfessorId] = useState("");
  const navigate = useNavigate();

  const handleProfessorLogin = () => {
    if (!professorId) {
      alert("Please enter your professor ID");
      return;
    }

    fetch(`${API_BASE}/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "include",
      body: JSON.stringify({ role: "PROFESSOR", professorId }),
    })
      .then((res) => res.json())
      .then(() => {
        login("PROFESSOR");
        navigate("/home");
      })
      .catch((error) => console.error("Login failed:", error));
  };

  const handleStudentLogin = () => {
    if (!studentId) {
      alert("Please enter your student ID");
      return;
    }

    fetch(`${API_BASE}/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "include",
      body: JSON.stringify({ role: "STUDENT", studentId }),
    })
      .then((res) => res.json())
      .then(() => {
        login("STUDENT", studentId);
        navigate("/home");
      })
      .catch((error) => console.error("Login failed:", error));
  };

  return (
    <div id="root">
      <div className="spacing"> </div>
      <div className="content-area">
        <div className="flex justify-center items-center min-h-screen bg-gradient-to-r from-indigo-500 to-blue-500">
          <div className="bg-white p-6 rounded-lg shadow-lg w-96 flex flex-col items-center">
            <h2 className="text-3xl font-semibold text-center text-indigo-600 mb-6">Login</h2>

            <div className="space-y-6 w-full"> {/* Added space-y-6 here for row spacing */}
              {/* Professor Login Row */}
              <div className="flex flex-col items-center space-y-4 w-full">
                <input
                  type="text"
                  placeholder="Enter Professor ID"
                  value={professorId}
                  onChange={(e) => setProfessorId(e.target.value)}
                  className="p-3 border border-gray-300 rounded-lg w-64 shadow-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
                <button
                  onClick={handleProfessorLogin}
                  className="p-3 w-64 text-white bg-blue-600 rounded-lg shadow-md hover:bg-blue-700 transition duration-300"
                >
                  Login as Professor
                </button>
              </div>

              {/* Student Login Row */}
              <div className="flex flex-col items-center space-y-4 w-full">
                <input
                  type="text"
                  placeholder="Enter Student ID"
                  value={studentId}
                  onChange={(e) => setStudentId(e.target.value)}
                  className="p-3 border border-gray-300 rounded-lg w-64 shadow-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
                <button
                  onClick={handleStudentLogin}
                  className="p-3 w-64 text-white bg-green-600 rounded-lg shadow-md hover:bg-green-700 transition duration-300"
                >
                  Login as Student
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default Login;
