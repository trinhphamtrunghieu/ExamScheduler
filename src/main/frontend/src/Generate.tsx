import { useState, useEffect } from "react";
import { API_BASE } from "./common.tsx";
import NavBar from "./NavBar.tsx";
import { useNavigate } from "react-router-dom";
import { Download } from "lucide-react";

function Generate() {
  const [schedule, setSchedule] = useState([]);
  const [filterType, setFilterType] = useState("maMonHoc");
  const [filterValue, setFilterValue] = useState("");
  const [sortConfig, setSortConfig] = useState({ key: "ngay_thi", direction: "asc" });
  const [isLoading, setIsLoading] = useState(false);
  const [isProfessor, setIsProfessor] = useState(false);
  const [subjects, setSubjects] = useState([]);
  const [selectedSubjects, setSelectedSubjects] = useState([]);
  const [dayFrom, setDayFrom] = useState("");
  const [dayTo, setDayTo] = useState("");
  const [hourFrom, setHourFrom] = useState("");
  const [hourTo, setHourTo] = useState("");
  const [populationSize, setPopulationSize] = useState(100);
  const [crossoverRate, setCrossoverRate] = useState(0.8);
  const [mutationRate, setMutationRate] = useState(0.1);
  const [maxGenerations, setMaxGenerations] = useState(500);
  const [validationError, setValidationError] = useState("");
  const [maxExamPerDay, setMaxExamPerDay] = useState(5);
  const navigate = useNavigate();

  useEffect(() => {
    fetch(`${API_BASE}/auth/session`, {
      credentials: "include",
    })
      .then((res) => res.json())
      .then((data) => {
        if (data.role !== "PROFESSOR") {
          alert("FORBIDDEN");
          navigate("/");
        } else {
          setIsProfessor(true);
        }
      })
      .catch((error) => {
        console.error("Error checking user role:", error);
        navigate("/");
      });
  }, [navigate]);

    useEffect(() => {
      if (isProfessor) {
        fetch(`${API_BASE}/subjects/list`, { credentials: "include" })
          .then((res) => res.json())
          .then((data) => {
            // Process subjects to ensure unique tenMonHoc values
            const uniqueSubjects = [];
            const uniqueSubjectNames = new Set();

            data.forEach(sub => {
              if (!uniqueSubjectNames.has(sub.tenMonHoc)) {
                uniqueSubjectNames.add(sub.tenMonHoc);
                uniqueSubjects.push(sub);
              }
            });

            setSubjects(uniqueSubjects);
          })
          .catch((error) => console.error("Error fetching subjects:", error));
      }
    }, [isProfessor]);

    const fetchScheduleWithRetry = async (options, method, retries = 3) => {
        for (let i = 0; i < retries; i++) {
            try {
                let path = "generate"
                if (method === '1') {
                    path = "generate"
                } else if (method === '2') {
                    path = "generate2"
                } else {
                    throw new Error(`Invalid method: ${method}`)
                }
                const response = await fetch(`${API_BASE}/schedule/${path}`, {
                    method: "POST",
                    credentials: "include",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify(options),
                });

                if (!response.ok) {
                    if (response.status === 500) {
                        console.error("Internal Server Error, retrying...");
                        continue; // Retry on 500 Internal Server Error
                    }
                    throw new Error(`Error: ${response.statusText}`);
                }

                const data = await response.json();
                    return data; // Return successful response data
                } catch (error) {
                    if (i === retries - 1) {
                        throw error; // Throw error if last retry fails
                }
            }
        }
    };

  const handleGenerateSchedule = (method) => {
    setValidationError("");
    const dateFrom = new Date(dayFrom);
    const dateTo = new Date(dayTo);
    const timeFrom = new Date(`1970-01-01T${hourFrom}:00`);
    const timeTo = new Date(`1970-01-01T${hourTo}:00`);

    if (dateTo < dateFrom) {
      setValidationError("Error: 'Date To' must be later than 'Date From'.");
      return;
    }

    if (dateTo.getTime() === dateFrom.getTime() && timeTo <= timeFrom) {
      setValidationError("Error: 'Hour To' must be later than 'Hour From' for the same day.");
      return;
    }

    if (timeTo <= timeFrom && dateTo > dateFrom) {
      setValidationError("Error: 'Hour To' must be later than 'Hour From'.");
      return;
    }

    setIsLoading(true);
    const options = {
      selectedSubjects,
      dayFrom,
      dayTo,
      hourFrom,
      hourTo,
      populationSize,
      crossoverRate,
      mutationRate,
      maxGenerations,
      maxExamPerDay,
    };

    fetchScheduleWithRetry(options, method)
      .then((data) => {
        setIsLoading(false);
        if (data.error) {
          alert(`${data.error}`);
          setSchedule(data.data)
        } else {
          alert(`${data.error}`);
          setSchedule(data);
        }
      })
      .catch((error) => {
        setIsLoading(false);
        console.error("Error fetching schedule:", error);
        alert("Failed to generate schedule after multiple attempts.");
      });
  };

  const handleSelectAll = () => {
    const allSubjects = subjects.map((subject) => subject.tenMonHoc);
    setSelectedSubjects(allSubjects);
  };

  const handleDeselectAll = () => {
    setSelectedSubjects([]);
  };

  const requestSort = (key) => {
    let direction = sortConfig.key === key && sortConfig.direction === 'asc' ? 'desc' : 'asc';

    if (key === 'gio_thi' && sortConfig.key === 'ngay_thi') {
      direction = 'desc'; // Ensure gio_thi is descending if ngay_thi is involved
    }

    setSortConfig({ key, direction });
  };

  const sortedSchedule = [...schedule].sort((a, b) => {
    // First, sort by 'ngay_thi'
    if (a.ngay_thi < b.ngay_thi) return sortConfig.direction === 'asc' ? -1 : 1;
    if (a.ngay_thi > b.ngay_thi) return sortConfig.direction === 'asc' ? 1 : -1;

    // If 'ngay_thi' is the same, then sort by 'gio_thi'
    if (a.gio_thi < b.gio_thi) return sortConfig.direction === 'asc' ? -1 : 1;
    if (a.gio_thi > b.gio_thi) return sortConfig.direction === 'asc' ? 1 : -1;

    return 0;
  });

  if (!isProfessor) {
    return null;
  }

  const handleExportCSV = async () => {
    if (schedule.length === 0) {
      alert("No schedule data to export!");
      return;
    }

    try {
      const response = await fetch(`${API_BASE}/schedule/export`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include',
        body: JSON.stringify(schedule)
      });

      if (!response.ok) {
        throw new Error('Export failed');
      }

      const blob = await response.blob();
      const downloadUrl = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = downloadUrl;
      link.setAttribute('download', `exam_schedule_${new Date().toISOString().split('T')[0]}.csv`);
      document.body.appendChild(link);
      link.click();
      link.parentNode.removeChild(link);
      window.URL.revokeObjectURL(downloadUrl);
    } catch (error) {
      console.error('Error exporting CSV:', error);
      alert('Failed to export schedule. Please try again.');
    }
  };

  return (
    <div id="root">
      <NavBar />

      <div className="spacing"></div>

      <div className="content-area">
        <div className="content-container">
          <div className="options-section">
            <h2>Generate Exam Schedule</h2>

            <label>Subjects:</label>
            <div className="subjects-list">
              <button onClick={handleSelectAll} className="select-all-button">Select All</button>
              <button onClick={handleDeselectAll} className="deselect-all-button">Deselect All</button>
              <select
                multiple
                value={selectedSubjects}
                onChange={(e) => setSelectedSubjects([...e.target.selectedOptions].map((opt) => opt.value))}
              >
                {subjects.map((sub) => (
                  <option key={sub.tenMonHoc} value={sub.tenMonHoc}>
                    {sub.tenMonHoc}
                  </option>
                ))}
              </select>
            </div>

            <label>Date Range:</label>
            <input type="date" value={dayFrom} onChange={(e) => setDayFrom(e.target.value)} />
            <input type="date" value={dayTo} onChange={(e) => setDayTo(e.target.value)} />

            <label>Time Range:</label>
            <input type="time" value={hourFrom} onChange={(e) => setHourFrom(e.target.value)} />
            <input type="time" value={hourTo} onChange={(e) => setHourTo(e.target.value)} />

            <label>Max exams per timeslot:</label>
            <input type="number" onChange={(e) => setMaxExamPerDay(Number(e.target.value))} />

            <label>Population Size:</label>
            <input
              type="number"
              value={populationSize}
              onChange={(e) => setPopulationSize(Number(e.target.value))}
              min="1"
            />

            <label>Crossover Rate:</label>
            <input
              type="range"
              min="0.1"
              max="1.0"
              step="0.01"
              value={crossoverRate}
              onChange={(e) => setCrossoverRate(Number(e.target.value))}
            />

            <label>Mutation Rate:</label>
            <input
              type="range"
              min="0.1"
              max="1.0"
              step="0.01"
              value={mutationRate}
              onChange={(e) => setMutationRate(Number(e.target.value))}
            />

            <label>Max Generations:</label>
            <input
              type="number"
              value={maxGenerations}
              onChange={(e) => setMaxGenerations(Number(e.target.value))}
              min="1"
            />

            {validationError && <div className="error-message">{validationError}</div>}

            <button onClick={() => handleGenerateSchedule('1')} disabled={isLoading}>
              Generate Schedule - Genetic Algorithm
            </button>
            <button onClick={() => handleGenerateSchedule('2')} disabled={isLoading}>
              Generate Schedule - Welsh-Powell Algorithm
            </button>
          </div>

          <div className="result-section">
            {isLoading ? (
              <div className="spinner-container">
                <div className="spinner"></div>
              </div>
            ) : (
              <div className="table-container">
                <div className="result-section">
                    <div className="result-header">
                        <h2>Generated Schedule</h2>
                        {schedule.length > 0 && (
                        <button
                        onClick={handleExportCSV}
                        className="export-button"
                        title="Export as CSV"
                        >
                        <Download className="w-4 h-4 mr-2" />
                        Export CSV
                        </button>
                        )}
                    </div>
                </div>
                <table>
                  <thead>
                    <tr>
                      <th onClick={() => requestSort('ma_mon_hoc')}>Mã Môn Học</th>
                      <th onClick={() => requestSort('ten_mon_hoc')}>Tên Môn Học</th>
                      <th onClick={() => requestSort('ngay_thi')}>Ngày Thi</th>
                      <th onClick={() => requestSort('gio_thi')}>Giờ Bắt Đầu Thi</th>
                      <th onClick={() => requestSort('gio_ket_thuc')}>Giờ Kết Thúc Thi</th>
                      <th onClick={() => requestSort('thoi_luong_thi')}>Thời Lượng</th>
                    </tr>
                  </thead>
                  <tbody>
                    {sortedSchedule.length > 0 ? (
                      sortedSchedule.map((exam, index) => (
                        <tr key={index}>
                          <td>{exam.ma_mon_hoc}</td>
                          <td>{exam.ten_mon_hoc || "N/A"}</td>
                          <td>{exam.ngay_thi}</td>
                          <td>{exam.gio_thi}</td>
                          <td>{exam.gio_ket_thuc}</td>
                          <td>{exam.thoi_luong_thi} phút</td>
                        </tr>
                      ))
                    ) : (
                      <tr>
                        <td colSpan={5} >No schedule generated yet.</td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

export default Generate;
