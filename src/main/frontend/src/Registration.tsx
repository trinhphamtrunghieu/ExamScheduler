import { useState, useEffect } from "react";
import { API_BASE } from "./common.tsx";
import NavBar from "./NavBar.tsx";
import { useNavigate } from "react-router-dom";

function Registrations() {
  const [registrations, setRegistrations] = useState([]);
  const [filterType, setFilterType] = useState("ma_sinh_vien"); // Default filter by student code
  const [filterValue, setFilterValue] = useState(""); // Value for the selected filter
  const [sortConfig, setSortConfig] = useState({ key: 'ma_sinh_vien', direction: 'asc' });
  const navigate = useNavigate();

  // Fetch registrations with session
  useEffect(() => {
    fetch(`${API_BASE}/registrations`, {
      credentials: "include", // Ensures session cookies are sent
    })
      .then((res) => res.json())
      .then((data) => {
        console.log("Registrations Data:", data);
        setRegistrations(data);
      })
      .catch((error) => {
        console.error("Error fetching registrations:", error);
        navigate("/"); // Redirect to login if there's an error
      });
  }, [navigate]);

  // Sorting function
  const sortedRegistrations = [...registrations].sort((a, b) => {
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

  // Filter registrations based on the selected filterType and filterValue
  const filteredRegistrations = registrations.filter((registration) => {
    if (!filterValue) {
      // If there's no filter value, show all registrations
      return true;
    }

    if (filterType === "ma_sinh_vien" || filterType === "ma_mon_hoc" || filterType === "ten_mon_hoc" || filterType === "ten_giang_vien" || filterType === "ten_sinh_vien") {
      // For text-based filters (student code, subject code, subject name, teacher name, student name)
      return registration[filterType]?.toLowerCase().includes(filterValue.toLowerCase());
    }

    return false;
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
          <h2 className="text-3xl font-semibold text-indigo-600 mb-6">Danh sách đăng ký</h2>

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
                <option value="ma_sinh_vien">Mã Sinh Viên</option>
                <option value="ma_mon_hoc">Mã Môn Học</option>
                <option value="ten_mon_hoc">Tên Môn Học</option>
                <option value="ten_giang_vien">Tên Giảng Viên</option>
                <option value="ten_sinh_vien">Tên Sinh Viên</option>
              </select>
            </div>

            {/* Filter Value */}
            <div className="flex items-center">
              <label htmlFor="filterValue" className="mr-2 text-lg">Enter Value:</label>
              <input
                type="text"
                id="filterValue"
                placeholder={`Enter ${filterType === 'ma_sinh_vien' ? 'Student Code' : filterType === 'ma_mon_hoc' ? 'Subject Code' : 'Value'}`}
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
                  <span className="ml-2">
                    {sortConfig.key === 'ma_sinh_vien' ? (sortConfig.direction === 'asc' ? '🔼' : '🔽') : '↕️'}
                  </span>
                </th>
                <th className="px-6 py-4 text-left cursor-pointer hover:bg-indigo-700" onClick={() => requestSort('ma_mon_hoc')}>
                  Mã Môn Học
                  <span className="ml-2">
                    {sortConfig.key === 'ma_mon_hoc' ? (sortConfig.direction === 'asc' ? '🔼' : '🔽') : '↕️'}
                  </span>
                </th>
                <th className="px-6 py-4 text-left cursor-pointer hover:bg-indigo-700" onClick={() => requestSort('ten_mon_hoc')}>
                  Tên Môn Học
                  <span className="ml-2">
                    {sortConfig.key === 'ten_mon_hoc' ? (sortConfig.direction === 'asc' ? '🔼' : '🔽') : '↕️'}
                  </span>
                </th>
                <th className="px-6 py-4 text-left cursor-pointer hover:bg-indigo-700" onClick={() => requestSort('ten_giang_vien')}>
                  Tên Giảng Viên
                  <span className="ml-2">
                    {sortConfig.key === 'ten_giang_vien' ? (sortConfig.direction === 'asc' ? '🔼' : '🔽') : '↕️'}
                  </span>
                </th>
                <th className="px-6 py-4 text-left cursor-pointer hover:bg-indigo-700" onClick={() => requestSort('ten_sinh_vien')}>
                  Tên Sinh Viên
                  <span className="ml-2">
                    {sortConfig.key === 'ten_sinh_vien' ? (sortConfig.direction === 'asc' ? '🔼' : '🔽') : '↕️'}
                  </span>
                </th>
              </tr>
            </thead>
            <tbody className="text-gray-800">
              {filteredRegistrations.length > 0 ? (
                filteredRegistrations.map((r, index) => (
                  <tr key={index} className="hover:bg-gray-100">
                    <td className="px-6 py-3 border-b">{r.ma_sinh_vien}</td>
                    <td className="px-6 py-3 border-b">{r.ma_mon_hoc}</td>
                    <td className="px-6 py-3 border-b">{r.ten_mon_hoc}</td>
                    <td className="px-6 py-3 border-b">{r.ten_giang_vien}</td>
                    <td className="px-6 py-3 border-b">{r.ten_sinh_vien}</td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={5} className="px-6 py-3 text-center text-gray-500">No registrations found</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
export default Registrations;
