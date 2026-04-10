import { useState } from "react";
import { API_BASE } from "./common.tsx";
import NavBar from './NavBar.tsx'; // Assuming you have NavBar component

function AddStudent() {
  const [student, setStudent] = useState({ id: "", name: "" });
  const [isLoading, setIsLoading] = useState(false);
  const [message, setMessage] = useState("");

  const handleSubmit = (e) => {
    e.preventDefault();
    setIsLoading(true); // Set loading state to true
    fetch(`${API_BASE}/students/add`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(student),
    })
      .then((res) => res.json())
      .then((data) => {
        if (data.error) {
            setIsLoading(false);
            alert(`${data.error}`);
        } else {
            setMessage("Student added successfully!");
            setStudent({ id: "", name: "" }); // Clear the form fields
            setIsLoading(false); // Reset loading state
        }
      })
      .catch((error) => {
        console.error("Error adding student:", error);
        setMessage("Error adding student. Please try again.");
        setIsLoading(false); // Reset loading state
      });
  };

  return (
    <div id="root">
      <NavBar /> {/* NavBar */}

      <div className="spacing"></div> {/* Spacing */}

      <div className="content-area">
        <h2 className="text-3xl font-semibold text-indigo-600 mb-6">Add Student</h2>

        {isLoading ? (
          <div className="spinner-container">
            <div className="spinner"></div>
          </div>
        ) : (
          <div>
            {/* Form */}
            <div className="mb-6 w-full max-w-xs space-y-4">
              <form onSubmit={handleSubmit}>
                <div className="flex items-center mb-4">
                  <label
                    htmlFor="student_id"
                    className="mr-2 text-lg font-semibold"
                  >
                    Mã Sinh Viên:
                  </label>
                  <input
                    type="text"
                    id="student_id"
                    placeholder="Mã Sinh Viên"
                    value={student.id}
                    onChange={(e) =>
                      setStudent({ ...student, id: e.target.value })
                    }
                    className="p-2 border border-gray-300 rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-indigo-600"
                  />
                </div>

                <div className="flex items-center mb-4">
                  <label
                    htmlFor="student_name"
                    className="mr-2 text-lg font-semibold"
                  >
                    Tên Sinh Viên:
                  </label>
                  <input
                    type="text"
                    id="student_name"
                    placeholder="Tên Sinh Viên"
                    value={student.name}
                    onChange={(e) =>
                      setStudent({ ...student, name: e.target.value })
                    }
                    className="p-2 border border-gray-300 rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-indigo-600"
                  />
                </div>

                <div className="flex justify-center mt-4">
                  <button
                    type="submit"
                    disabled={isLoading}
                    className="px-6 py-2 text-white bg-indigo-600 rounded-lg hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-indigo-600"
                  >
                    {isLoading ? "Adding..." : "Add Student"}
                  </button>
                </div>
              </form>
            </div>

            {/* Feedback Message */}
            {message && (
              <div
                className={`mt-4 text-center ${message.includes("Error") ? "text-red-500" : "text-green-500"}`}
              >
                {message}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

export default AddStudent;
