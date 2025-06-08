import { useState, useEffect, useRef, changeEvent } from "react";
import { API_BASE } from "./common.tsx";
import NavBar from "./NavBar.tsx";
import { useNavigate } from "react-router-dom";
import { Download, Upload } from "lucide-react";

function Students() {
    const [students, setStudents] = useState([]);
    const [filterType, setFilterType] = useState("ten_sinh_vien");
    const [filterValue, setFilterValue] = useState("");
    const [sortConfig, setSortConfig] = useState({ key: 'ma_sinh_vien', direction: 'asc' });
    const [userRole, setUserRole] = useState(null); // Store user role
    const navigate = useNavigate();
    const [isImporting, setIsImporting] = useState(false);
    const [importMessage, setImportMessage] = useState("");
    const [importError, setImportError] = useState("");
    const fileInputRef = useRef<HTMLInputElement>(null);
    const [disabledDownload, setDisabledDownload] = useState(false);

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
    fetch(`${API_BASE}/students/list`, {
      credentials: "include", // ✅ Ensure session authentication
    })
      .then((res) => res.json())
      .then((data) => {
        console.log("Students Data:", data);
        setDisabledDownload(false);
        setStudents(data);
      })
      .catch((error) => console.error("Error fetching students:", error));
  };
  // Filter students based on the selected filterType and filterValue
  const filteredStudents = students.filter(student => {
    if (!filterValue) return true;
    return student[filterType]?.toLowerCase().includes(filterValue.toLowerCase());
  });

  // Sorting function
  const sortedStudents = [...filteredStudents].sort((a, b) => {
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
    let direction = sortConfig.key === key && sortConfig.direction === 'asc' ? 'desc' : 'asc';
    setSortConfig({ key, direction });
  };

  // Handle filter value change
  const handleFilterValueChange = (e) => {
    setFilterValue(e.target.value);
  };

    const handleExportCSV = async () => {
        if (students.length === 0) {
            alert("No student data to export!");
            return;
        }

        try {
            const response = await fetch(`${API_BASE}/students/export`, {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
            },
            credentials: 'include',
            body: JSON.stringify(students)
            });

            if (!response.ok) {
                throw new Error('Export failed');
            }

            const blob = await response.blob();
            const downloadUrl = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = downloadUrl;
            link.setAttribute('download', `students_${new Date().toISOString().split('T')[0]}.csv`);
            document.body.appendChild(link);
            link.click();
            link.parentNode.removeChild(link);
            window.URL.revokeObjectURL(downloadUrl);
        } catch (error) {
                console.error('Error exporting CSV:', error);
                alert('Failed to export schedule. Please try again.');
        }
    };

    const handleImportCSV = async (e: ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (!file) return;
        setIsImporting(true);
        setImportMessage("");
        setImportError("");
        const formData = new FormData();
        formData.append("file", file);

        try {
        const response = await fetch(`${API_BASE}/students/import`, {
          method: "POST",
          credentials: "include",
          body: formData,
        });

        const data = await response.json();
        if (response.ok) {
          setImportMessage(data.message || "Students imported successfully!");
          fetchStudents(); // Refresh the student list
        } else {
          setImportError(data.error || "Failed to import students");
        }
        } catch (error) {
        setImportError("Network error. Please try again.");
        console.error("Import error:", error);
        } finally {
        setIsImporting(false);
        // Reset file input
        if (fileInputRef.current) fileInputRef.current.value = "";
        }
    };

  return (
    <div id="root" className="flex">
      <NavBar /> {/* NavBar */}

      <div className="spacing"></div> {/* Spacing */}

      <div className="content-area">

        <div className="p-6 bg-gray-50 min-h-screen flex flex-col items-center">
            <div className="result-section">
                <div className="result-header">
                    <h2>Danh sách sinh viên</h2>
                    <div className="import-export-button-container">
                        <input
                            type="file"
                            ref={fileInputRef}
                            onChange={handleImportCSV}
                            accept=".csv"
                            className="hidden"
                            disabled={isImporting}
                        />
                        <button
                            onClick={() => fileInputRef.current?.click()}
                            disabled={isImporting}
                            className="import-export-button"
                        >
                            <Upload className="w-4 h-4 mr-2" />
                            {isImporting ? "Importing..." : "Import CSV"}
                        </button>
                        <button
                            onClick={handleExportCSV}
                            title="Export as CSV"
                            disabled={disabledDownload}
                            className="import-export-button"
                        >
                            <Download className="w-4 h-4 mr-2" /> Export CSV
                        </button>
                    </div>
                    {importMessage && (
                    <div className="mt-2 text-green-600">{importMessage}</div>
                    )}
                    {importError && (
                    <div className="mt-2 text-red-500">{importError}</div>
                    )}
                </div>
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
              {sortedStudents.length > 0 ? (
                sortedStudents.map((s) => (
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
