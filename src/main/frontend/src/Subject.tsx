import { useState, useEffect } from "react";
import { API_BASE } from "./common.tsx";
import NavBar from "./NavBar.tsx";
import { useNavigate } from "react-router-dom";

function Subjects() {
  const [subjects, setSubjects] = useState([]);
  const [filterType, setFilterType] = useState("tenMonHoc"); // Default filter by subject name
  const [filterValue, setFilterValue] = useState(""); // Value for the selected filter
  const [sortConfig, setSortConfig] = useState({ key: 'maMonHoc', direction: 'asc' });
  const [availableExamDurations, setAvailableExamDurations] = useState([]); // To hold unique exam durations
  const [userRole, setUserRole] = useState(null); // To store user role (if user is a professor)
  const navigate = useNavigate();

  // Check session and fetch subjects
  useEffect(() => {
    // Fetch user session first
    fetch(`${API_BASE}/auth/session`, {
      method: "GET",
      credentials: "include", // Ensures session cookies are sent
    })
      .then((res) => res.json())
      .then((data) => {
        if (!data.role) {
          navigate("/"); // Redirect to login if no session or role is not found
        } else {
          setUserRole(data.role);
          if (data.role === "PROFESSOR") {
            fetchSubjects(); // Only fetch subjects if the user is a professor
          } else {
              alert("FORBIDDEN")
            navigate("/")
          }
        }
      })
      .catch((error) => {
        console.error("Error checking session:", error);
        navigate("/");
      });
  }, [navigate]);

  // Fetch subjects
  const fetchSubjects = () => {
    fetch(`${API_BASE}/subjects`, {
      credentials: "include", // Ensure session authentication
    })
      .then((res) => res.json())
      .then((data) => {
        console.log("Subjects Data:", data);
        setSubjects(data);

        // Extract unique exam durations from the subjects data
        const durations = [...new Set(data.map(s => s.thoi_luong_thi))];
        setAvailableExamDurations(durations); // Set available exam durations for filter
      })
      .catch((error) => console.error("Error fetching subjects:", error));
  };

  // Function to format date (optional)
  const formatDate = (date) => {
    if (!date) return '';
    const d = new Date(date);
    return d.toISOString().split('T')[0]; // Returns YYYY-MM-DD
  };

  // Filter subjects based on the selected filterType and filterValue
  const filteredSubjects = subjects.filter(subject => {
    if (!filterValue) {
      // If there's no filter value, show all subjects
      return true;
    }

    if (filterType === "ngay_bat_dau" || filterType === "ngay_ket_thuc") {
      // For date filters, convert the value to a Date and compare
      const filterDate = new Date(filterValue);
      const subjectDate = new Date(subject[filterType]);

      if (filterType === "ngay_bat_dau") {
        return subjectDate >= filterDate; // Filter for subjects starting from the selected date
      } else if (filterType === "ngay_ket_thuc") {
        return subjectDate <= filterDate; // Filter for subjects ending before the selected date
      }
    } else if (filterType === "thoi_luong_thi") {
      // For duration filter, compare values (ensure it's a positive number)
      return subject[filterType] == filterValue; // Exact match for exam duration
    } else {
      // For text-based filters (subject name, etc.)
      return subject[filterType]?.toLowerCase().includes(filterValue.toLowerCase());
    }
  });

  // Sorting function
  const sortedSubjects = [...filteredSubjects].sort((a, b) => {
    if (a[sortConfig.key] < b[sortConfig.key]) {
      return sortConfig.direction === 'asc' ? -1 : 1;
    }
    if (a[sortConfig.key] > b[sortConfig.key]) {
      return sortConfig.direction === 'asc' ? 1 : -1;
    }
    return 0;
  });

  // Function to handle sorting
  const requestSort = (key) => {
    let direction = 'asc';
    if (sortConfig.key === key && sortConfig.direction === 'asc') {
      direction = 'desc';
    }
    setSortConfig({ key, direction });
  };

  // Handle filter value change with validation for Thời Lượng Thi
  const handleFilterValueChange = (e) => {
    const value = e.target.value;
    if (filterType === "thoi_luong_thi") {
      // Allow only positive numbers or values from the available options
      if (availableExamDurations.includes(Number(value)) || value === "") {
        setFilterValue(value);
      }
    } else {
      setFilterValue(value);
    }
  };

  return (
    <div id="root" className="flex">
      <NavBar /> {/* NavBar */}

      <div className="spacing"></div> {/* Spacing */}

      <div className="content-area">

        <div className="p-6 bg-gray-50 min-h-screen flex flex-col items-center">
          <h2 className="text-3xl font-semibold text-indigo-600 mb-6">Danh sách môn học</h2>

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
                <option value="tenMonHoc">Tên Môn Học</option>
                <option value="maMonHoc">Mã Môn Học</option>
                <option value="ngay_bat_dau">Ngày Bắt Đầu</option>
                <option value="ngay_ket_thuc">Ngày Kết Thúc</option>
                <option value="ten_gv_dung_lop">Giảng Viên</option>
                <option value="thoi_luong_thi">Thời Lượng Thi</option> {/* Added Thời Lượng Thi */}
              </select>
            </div>

            {/* Filter Value */}
            {filterType === "ngay_bat_dau" || filterType === "ngay_ket_thuc" ? (
              <div className="flex items-center">
                <label htmlFor="filterValue" className="mr-2 text-lg">Select Date:</label>
                <input
                  type="date"
                  id="filterValue"
                  value={filterValue}
                  onChange={(e) => setFilterValue(e.target.value)}
                  className="p-2 border border-gray-300 rounded-lg"
                />
              </div>
            ) : filterType === "thoi_luong_thi" ? (
              <div className="flex items-center">
                <label htmlFor="filterValue" className="mr-2 text-lg">Select Duration (minutes):</label>
                <select
                  id="filterValue"
                  value={filterValue}
                  onChange={handleFilterValueChange}
                  className="p-2 border border-gray-300 rounded-lg"
                >
                  <option value="">-- Select Duration --</option>
                  {availableExamDurations.map((duration) => (
                    <option key={duration} value={duration}>{duration} minutes</option>
                  ))}
                </select>
              </div>
            ) : (
              <div className="flex items-center">
                <label htmlFor="filterValue" className="mr-2 text-lg">Enter Value:</label>
                <input
                  type="text"
                  id="filterValue"
                  placeholder={`Enter ${filterType === 'tenMonHoc' ? 'Subject Name' : filterType === 'ten_gv_dung_lop' ? 'Instructor Name' : 'Subject Code'}`}
                  value={filterValue}
                  onChange={(e) => setFilterValue(e.target.value)}
                  className="p-2 border border-gray-300 rounded-lg"
                />
              </div>
            )}
          </div>

          <table className="min-w-full table-auto border-collapse bg-white rounded-lg shadow-lg overflow-hidden">
            <thead className="bg-indigo-600 text-white">
              <tr>
                <th className="px-6 py-4 text-left cursor-pointer hover:bg-indigo-700" onClick={() => requestSort('maMonHoc')}>
                  Mã Môn Học
                  <span className="ml-2">
                    {sortConfig.key === 'maMonHoc' ? (sortConfig.direction === 'asc' ? '🔼' : '🔽') : '↕️'}
                  </span>
                </th>
                <th className="px-6 py-4 text-left cursor-pointer hover:bg-indigo-700" onClick={() => requestSort('tenMonHoc')}>
                  Tên Môn Học
                  <span className="ml-2">
                    {sortConfig.key === 'tenMonHoc' ? (sortConfig.direction === 'asc' ? '🔼' : '🔽') : '↕️'}
                  </span>
                </th>
                <th className="px-6 py-4 text-left cursor-pointer hover:bg-indigo-700" onClick={() => requestSort('ten_gv_dung_lop')}>
                  Giảng Viên
                  <span className="ml-2">
                    {sortConfig.key === 'ten_gv_dung_lop' ? (sortConfig.direction === 'asc' ? '🔼' : '🔽') : '↕️'}
                  </span>
                </th>
                <th className="px-6 py-4 text-left cursor-pointer hover:bg-indigo-700" onClick={() => requestSort('ngay_bat_dau')}>
                  Ngày Bắt Đầu
                  <span className="ml-2">
                    {sortConfig.key === 'ngay_bat_dau' ? (sortConfig.direction === 'asc' ? '🔼' : '🔽') : '↕️'}
                  </span>
                </th>
                <th className="px-6 py-4 text-left cursor-pointer hover:bg-indigo-700" onClick={() => requestSort('ngay_ket_thuc')}>
                  Ngày Kết Thúc
                  <span className="ml-2">
                    {sortConfig.key === 'ngay_ket_thuc' ? (sortConfig.direction === 'asc' ? '🔼' : '🔽') : '↕️'}
                  </span>
                </th>
                <th className="px-6 py-4 text-left cursor-pointer hover:bg-indigo-700" onClick={() => requestSort('thoi_luong_thi')}>
                  Thời Lượng Thi
                  <span className="ml-2">
                    {sortConfig.key === 'thoi_luong_thi' ? (sortConfig.direction === 'asc' ? '🔼' : '🔽') : '↕️'}
                  </span>
                </th>
              </tr>
            </thead>
            <tbody className="text-gray-800">
              {sortedSubjects.length > 0 ? (
                sortedSubjects.map((s) => (
                  <tr key={s.maMonHoc} className="hover:bg-gray-100">
                    <td className="px-6 py-3 border-b">{s.maMonHoc}</td>
                    <td className="px-6 py-3 border-b">{s.tenMonHoc}</td>
                    <td className="px-6 py-3 border-b">{s.ten_gv_dung_lop}</td>
                    <td className="px-6 py-3 border-b">{formatDate(s.ngay_bat_dau)}</td>
                    <td className="px-6 py-3 border-b">{formatDate(s.ngay_ket_thuc)}</td>
                    <td className="px-6 py-3 border-b">{s.thoi_luong_thi} phút</td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={6} className="px-6 py-3 text-center text-gray-500">No subjects found</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
export default Subjects;
