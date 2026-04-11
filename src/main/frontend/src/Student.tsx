import { useState, useEffect, useRef, ChangeEvent } from "react";
import { API_BASE } from "./common.tsx";
import NavBar from "./NavBar.tsx";
import { Download, Upload } from "lucide-react";
import ExportConfirmationForm from "./ExportConfirmationForm.tsx";
import { saveBlob } from "./exportUtils.ts";

function Students() {
    const expectedHeaders = ["MSSV", "Họ tên"];
    const [students, setStudents] = useState([]);
    const [filterType, setFilterType] = useState("name");
    const [filterValue, setFilterValue] = useState("");
    const [sortConfig, setSortConfig] = useState({ key: 'id', direction: 'asc' });
    const [isImporting, setIsImporting] = useState(false);
    const [importMessage, setImportMessage] = useState("");
    const [importError, setImportError] = useState("");
    const [pendingFile, setPendingFile] = useState<File | null>(null);
    const [detectedHeaders, setDetectedHeaders] = useState<string[]>([]);
    const [headerMapping, setHeaderMapping] = useState<Record<string, string>>({});
    const fileInputRef = useRef<HTMLInputElement>(null);
    const [disabledDownload, setDisabledDownload] = useState(false);
    const [showExportForm, setShowExportForm] = useState(false);
    const [isExporting, setIsExporting] = useState(false);

  // Fetch students on mount
  useEffect(() => {
    fetchStudents();
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

    const handleExportCSV = async (format: "csv" | "xlsx", fileName: string, saveHandle: any) => {
        if (students.length === 0) {
            alert("No student data to export!");
            return;
        }

        setIsExporting(true);
        try {
            const response = await fetch(`${API_BASE}/students/export?format=${format}`, {
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
            await saveBlob(blob, fileName, format, saveHandle);
            setShowExportForm(false);
        } catch (error) {
                console.error('Error exporting CSV:', error);
                alert('Failed to export schedule. Please try again.');
        } finally {
            setIsExporting(false);
        }
    };

    const handleImportCSV = async (e: ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (!file) return;
        setPendingFile(file);
        setImportMessage("");
        setImportError("");
        const headersFormData = new FormData();
        headersFormData.append("file", file);
        try {
          const headersResponse = await fetch(`${API_BASE}/students/import/headers`, {
            method: "POST",
            credentials: "include",
            body: headersFormData,
          });
          const headerData = await headersResponse.json();
          if (!headersResponse.ok) {
            setImportError(headerData.error || "Cannot detect CSV headers");
            return;
          }
          const incomingHeaders = headerData.headers || [];
          setDetectedHeaders(incomingHeaders);
          const defaultMapping: Record<string, string> = {};
          expectedHeaders.forEach((expected) => {
            const exact = incomingHeaders.find((h: string) => h === expected);
            defaultMapping[expected] = exact || "";
          });
          setHeaderMapping(defaultMapping);
        } catch (error) {
          setImportError("Cannot detect CSV headers");
          console.error("Header detection error:", error);
        } finally {
          if (fileInputRef.current) fileInputRef.current.value = "";
        }
    };

    const handleConfirmImport = async () => {
        if (!pendingFile) return;
        setIsImporting(true);
        setImportMessage("");
        setImportError("");
        const formData = new FormData();
        formData.append("file", pendingFile);
        const serializedMapping = Object.entries(headerMapping)
          .filter(([, actual]) => !!actual)
          .map(([expected, actual]) => `${expected}=${actual}`)
          .join(";");
        formData.append("headerMapping", serializedMapping);

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
        setPendingFile(null);
        setDetectedHeaders([]);
        setHeaderMapping({});
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
                            onClick={() => setShowExportForm(true)}
                            title="Export"
                            disabled={disabledDownload}
                            className="import-export-button"
                        >
                            <Download className="w-4 h-4 mr-2" /> Export
                        </button>
                    </div>
                    <ExportConfirmationForm
                      open={showExportForm}
                      isProcessing={isExporting}
                      defaultFileName={`students_${new Date().toISOString().split('T')[0]}`}
                      onCancel={() => setShowExportForm(false)}
                      onSubmit={({ format, fileName, saveHandle }) => {
                        void handleExportCSV(format, fileName, saveHandle);
                      }}
                    />
                    {importMessage && (
                    <div className="mt-2 text-green-600">{importMessage}</div>
                    )}
                    {importError && (
                    <div className="mt-2 text-red-500">{importError}</div>
                    )}
                    {pendingFile && detectedHeaders.length > 0 && (
                      <div className="mt-3">
                        {expectedHeaders.map((expected) => (
                          <div key={expected} className="flex items-center mb-2">
                            <label className="mr-2 text-sm w-28">{expected}</label>
                            <select
                              className="p-1 border border-gray-300 rounded-lg"
                              value={headerMapping[expected] || ""}
                              onChange={(e) => setHeaderMapping((prev) => ({ ...prev, [expected]: e.target.value }))}
                            >
                              <option value="">-- Select header --</option>
                              {detectedHeaders.map((header) => (
                                <option key={header} value={header}>{header}</option>
                              ))}
                            </select>
                          </div>
                        ))}
                        <button
                          onClick={handleConfirmImport}
                          disabled={isImporting || expectedHeaders.some((h) => !headerMapping[h])}
                          className="import-export-button mt-1"
                        >
                          {isImporting ? "Importing..." : "Confirm Import"}
                        </button>
                      </div>
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
                <option value="name">Tên Sinh Viên</option>
                <option value="id">Mã Sinh Viên</option>
              </select>
            </div>

            {/* Filter Value */}
            <div className="flex items-center">
              <label htmlFor="filterValue" className="mr-2 text-lg">Enter Value:</label>
              <input
                type="text"
                id="filterValue"
                placeholder={`Enter ${filterType === 'name' ? 'Student Name' : 'Student Code'}`}
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
                <th className="px-6 py-4 text-left cursor-pointer hover:bg-indigo-700" onClick={() => requestSort('id')}>
                  Mã Sinh Viên
                  <span className="ml-2">{sortConfig.key === 'id' ? (sortConfig.direction === 'asc' ? '🔼' : '🔽') : '↕️'}</span>
                </th>
                <th className="px-6 py-4 text-left cursor-pointer hover:bg-indigo-700" onClick={() => requestSort('name')}>
                  Tên Sinh Viên
                  <span className="ml-2">{sortConfig.key === 'name' ? (sortConfig.direction === 'asc' ? '🔼' : '🔽') : '↕️'}</span>
                </th>
              </tr>
            </thead>
            <tbody className="text-gray-800">
              {sortedStudents.length > 0 ? (
                sortedStudents.map((s) => (
                  <tr key={s.id} className="hover:bg-gray-100">
                    <td className="px-6 py-3 border-b">{s.id}</td>
                    <td className="px-6 py-3 border-b">{s.name}</td>
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
