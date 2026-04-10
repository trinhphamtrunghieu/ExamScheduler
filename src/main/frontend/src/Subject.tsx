import { useState, useEffect, useRef, ChangeEvent } from "react";
import { API_BASE } from "./common.tsx";
import NavBar from "./NavBar.tsx";
import { Download, Upload } from "lucide-react";

function Subjects() {
    const [subjects, setSubjects] = useState([]);
    const [filterType, setFilterType] = useState("name"); // Default filter by subject name
    const [filterValue, setFilterValue] = useState(""); // Value for the selected filter
    const [sortConfig, setSortConfig] = useState({ key: 'id', direction: 'asc' });
    const [availableExamDurations, setAvailableExamDurations] = useState([]); // To hold unique exam durations
    const [isImporting, setIsImporting] = useState(false);
    const [importMessage, setImportMessage] = useState("");
    const [importError, setImportError] = useState("");
    const fileInputRef = useRef<HTMLInputElement>(null);
    const [disabledDownload, setDisabledDownload] = useState(false);

  // Fetch subjects on mount
  useEffect(() => {
    fetchSubjects();
  }, []);

  // Fetch subjects
  const fetchSubjects = () => {
    fetch(`${API_BASE}/subjects/list`, {
      credentials: "include", // Ensure session authentication
    })
      .then((res) => res.json())
      .then((data) => {
        setSubjects(data);

        // Extract unique exam durations from the subjects data
        const durations = [...new Set(data.map(s => s.duration))];
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

    if (filterType === "startDate" || filterType === "endDate") {
      // For date filters, convert the value to a Date and compare
      const filterDate = new Date(filterValue);
      const subjectDate = new Date(subject[filterType]);

      if (filterType === "startDate") {
        return subjectDate >= filterDate; // Filter for subjects starting from the selected date
      } else if (filterType === "endDate") {
        return subjectDate <= filterDate; // Filter for subjects ending before the selected date
      }
    } else if (filterType === "duration") {
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
    if (filterType === "duration") {
      // Allow only positive numbers or values from the available options
      if (availableExamDurations.includes(Number(value)) || value === "") {
        setFilterValue(value);
      }
    } else {
      setFilterValue(value);
    }
  };
  const handleExportCSV = async () => {
    if (subjects.length === 0) {
      alert("No schedule data to export!");
      return;
    }

    try {
      const response = await fetch(`${API_BASE}/subjects/export`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include',
        body: JSON.stringify(subjects)
      });

      if (!response.ok) {
        throw new Error('Export failed');
      }

      const blob = await response.blob();
      const downloadUrl = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = downloadUrl;
      link.setAttribute('download', `subjects_${new Date().toISOString().split('T')[0]}.csv`);
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
        const response = await fetch(`${API_BASE}/subjects/import`, {
          method: "POST",
          credentials: "include",
          body: formData,
        });

        const data = await response.json();
        if (response.ok) {
          setImportMessage(data.message || "Subjects imported successfully!");
          fetchSubjects(); // Refresh the subject list
        } else {
          setImportError(data.error || "Failed to import subjects");
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
                    <h2> Danh sách môn học </h2>
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
                        <select id="filterType" value={filterType}
                                onChange={(e) => setFilterType(e.target.value)}
                                className="p-2 border border-gray-300 rounded-lg"
                        >
                            <option value="name">Tên Môn Học</option>
                            <option value="id">Mã Môn Học</option>
                            <option value="startDate">Ngày Bắt Đầu</option>
                            <option value="endDate">Ngày Kết Thúc</option>
                            <option value="ten_gv_dung_lop">Giảng Viên</option>
                            <option value="duration">Thời Lượng Thi</option> {/* Added Thời Lượng Thi */}
                        </select>
                    </div>
                    {/* Filter Value */}
                    {filterType === "startDate" || filterType === "endDate" ? (
                    <div className="flex items-center">
                        <label htmlFor="filterValue" className="mr-2 text-lg">Select Date:</label>
                        <input type="date" id="filterValue" value={filterValue}
                            onChange={(e) => setFilterValue(e.target.value)}
                            className="p-2 border border-gray-300 rounded-lg"
                        />
                    </div>
                    ) : filterType === "duration" ? (
                    <div className="flex items-center">
                        <label htmlFor="filterValue" className="mr-2 text-lg">Select Duration (minutes):</label>
                        <select id="filterValue" value={filterValue}
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
                        <input type="text" id="filterValue"
                            placeholder={`Enter ${filterType === 'name' ? 'Subject Name' : filterType === 'ten_gv_dung_lop' ? 'Instructor Name' : 'Subject Code'}`}
                            value={filterValue}
                            onChange={(e) => setFilterValue(e.target.value)}
                            className="p-2 border border-gray-300 rounded-lg"
                        />
                    </div>
                    )}
                </div>
            </div>
          <table className="min-w-full table-auto border-collapse bg-white rounded-lg shadow-lg overflow-hidden">
            <thead className="bg-indigo-600 text-white">
              <tr>
                <th className="px-6 py-4 text-left cursor-pointer hover:bg-indigo-700" onClick={() => requestSort('id')}>
                  Mã Môn Học
                  <span className="ml-2">
                    {sortConfig.key === 'id' ? (sortConfig.direction === 'asc' ? '🔼' : '🔽') : '↕️'}
                  </span>
                </th>
                <th className="px-6 py-4 text-left cursor-pointer hover:bg-indigo-700" onClick={() => requestSort('name')}>
                  Tên Môn Học
                  <span className="ml-2">
                    {sortConfig.key === 'name' ? (sortConfig.direction === 'asc' ? '🔼' : '🔽') : '↕️'}
                  </span>
                </th>
                <th className="px-6 py-4 text-left cursor-pointer hover:bg-indigo-700" onClick={() => requestSort('ten_gv_dung_lop')}>
                  Giảng Viên
                  <span className="ml-2">
                    {sortConfig.key === 'ten_gv_dung_lop' ? (sortConfig.direction === 'asc' ? '🔼' : '🔽') : '↕️'}
                  </span>
                </th>
                <th className="px-6 py-4 text-left cursor-pointer hover:bg-indigo-700" onClick={() => requestSort('startDate')}>
                  Ngày Bắt Đầu
                  <span className="ml-2">
                    {sortConfig.key === 'startDate' ? (sortConfig.direction === 'asc' ? '🔼' : '🔽') : '↕️'}
                  </span>
                </th>
                <th className="px-6 py-4 text-left cursor-pointer hover:bg-indigo-700" onClick={() => requestSort('endDate')}>
                  Ngày Kết Thúc
                  <span className="ml-2">
                    {sortConfig.key === 'endDate' ? (sortConfig.direction === 'asc' ? '🔼' : '🔽') : '↕️'}
                  </span>
                </th>
                <th className="px-6 py-4 text-left cursor-pointer hover:bg-indigo-700" onClick={() => requestSort('duration')}>
                  Thời Lượng Thi
                  <span className="ml-2">
                    {sortConfig.key === 'duration' ? (sortConfig.direction === 'asc' ? '🔼' : '🔽') : '↕️'}
                  </span>
                </th>
              </tr>
            </thead>
            <tbody className="text-gray-800">
              {sortedSubjects.length > 0 ? (
                sortedSubjects.map((s) => (
                  <tr key={s.id} className="hover:bg-gray-100">
                    <td className="px-6 py-3 border-b">{s.id}</td>
                    <td className="px-6 py-3 border-b">{s.name}</td>
                    <td className="px-6 py-3 border-b">{s.ten_gv_dung_lop}</td>
                    <td className="px-6 py-3 border-b">{formatDate(s.startDate)}</td>
                    <td className="px-6 py-3 border-b">{formatDate(s.endDate)}</td>
                    <td className="px-6 py-3 border-b">{s.duration} phút</td>
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