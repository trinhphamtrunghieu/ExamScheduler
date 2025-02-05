import { useState, useEffect } from "react";
import { API_BASE } from "./common.tsx";
import NavBar from "./NavBar.tsx";
import { useNavigate } from "react-router-dom";

function Students() {
  const [students, setStudents] = useState([]);
  const [filterType, setFilterType] = useState("ten_sinh_vien");
  const [filterValue, setFilterValue] = useState("");
  const [sortConfig, setSortConfig] = useState({ key: 'ma_sinh_vien', direction: 'asc' });
  const [userRole, setUserRole] = useState(null); // Store user role
  const navigate = useNavigate();

  // Check session and fetch students
  useEffect(() => {
    // Fetch user session first
    fetch(`${API_BASE}/auth/session`, {
      method: "GET",
      credentials: "include", // ✅ Ensures session cookies are sent
    })
      .then((res) => res.json())
      .then((data) => {
        if (!data.role) {
          navigate("/"); // Redirect to login if no session
        } else {
          setUserRole(data.role);
          if (data.role === "PROFESSOR") {
            fetchStudents(); // Only fetch students if professor
          }
        }
      })
      .catch((error) => {
        console.error("Error checking session:", error);
        navigate("/");
      });
  }, []);

  // Fetch students
  const fetchStudents = () => {
    fetch(`${API_BASE}/students`, {
      credentials: "include", // ✅ Ensure session authentication
    })
      .then((res) => res.json())
      .then((data) => {
        console.log("Students Data:", data);
        setStudents(data);
      })
      .catch((error) => console.error("Error fetching students:", error));
  };

  // Sorting function
  const sortedStudents = [...students].sort((a, b) => {
    if (a[sortConfig.key] < b[sortConfig.key]) return sortConfig.direction === 'asc' ? -1 : 1;
    if (a[sortConfig.key] > b[sortConfig.key]) return sortConfig.direction === 'asc' ? 1 : -1;
    return 0;
  });

  // Function to handle sorting
  const requestSort = (key) => {
    let direction = sortConfig.key === key && sortConfig.direction === 'asc' ? 'desc' : 'asc';
    setSortConfig({ key, direction });
  };

  // Filter students based on the selected filterType and filterValue
  const filteredStudents = students.filter(student => {
    if (!filterValue) return true;
    return student[filterType]?.toLowerCase().includes(filterValue.toLowerCase());
  });

  // Handle filter value change
  const handleFilterValueChange = (e) => {
    setFilterValue(e.target.value);
  };

  return (
    <div id="root" className="flex">
      <NavBar /> {/* NavBar */}

      <div className="spacing"></div> {/* Spacing */}

      <div className="content-area">
        <div className="p-6 bg-gray-50 min-h-screen flex flex-col items-center">
          <h2 className="text-3xl font-semibold text-indigo-600 mb-6">Danh sách sinh viên</h2>

          {/* Filter Form */}
          <div className="mb-6 w-full max-w-xs space-y-4">
            <div className="flex items-center">
              <label htmlFor="filterType" className="mr-2 text-lg">Filter By:</label>
              <select
                id="filterType"
                value={filterType}
                onChange={(e) => setFilterType(e.target.value)}
                className="p-2 border border-gray-300 rounded-lg"
              >
                <option value="ten_sinh_vien">Tên Sinh Viên</option>
                <option value="ma_sinh_vien">Mã Sinh Viên</option>
              </select>
            </div>

            {/* Filter Value */}
            <div className="flex items-center">
              <label htmlFor="filterValue" className="mr-2 text-lg">Enter Value:</label>
              <input
                type="text"
                id="filterValue"
                placeholder={`Enter ${filterType === 'ten_sinh_vien' ? 'Student Name' : 'Student Code'}`}
                value={filterValue}
                onChange={handleFilterValueChange}
                className="p-2 border border-gray-300 rounded-lg"
              />
            </div>
          </div>

          <table className="min-w-full table-auto border-collapse bg-white rounded-lg shadow-lg overflow-hidden">
            <thead className="bg-indigo-600 text-white">
              <tr>
                <th className="px-6 py-4 text-left cursor-pointer hover:bg-indigo-700" onClick={() => requestSort('ma_sinh_vien')}>
                  Mã Sinh Viên
                  <span className="ml-2">{sortConfig.key === 'ma_sinh_vien' ? (sortConfig.direction === 'asc' ? '🔼' : '🔽') : '↕️'}</span>
                </th>
                <th className="px-6 py-4 text-left cursor-pointer hover:bg-indigo-700" onClick={() => requestSort('ten_sinh_vien')}>
                  Tên Sinh Viên
                  <span className="ml-2">{sortConfig.key === 'ten_sinh_vien' ? (sortConfig.direction === 'asc' ? '🔼' : '🔽') : '↕️'}</span>
                </th>
              </tr>
            </thead>
            <tbody className="text-gray-800">
              {filteredStudents.length > 0 ? (
                filteredStudents.map((s) => (
                  <tr key={s.ma_sinh_vien} className="hover:bg-gray-100">
                    <td className="px-6 py-3 border-b">{s.ma_sinh_vien}</td>
                    <td className="px-6 py-3 border-b">{s.ten_sinh_vien}</td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={2} className="px-6 py-3 text-center text-gray-500">No students found</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

export default Students;
