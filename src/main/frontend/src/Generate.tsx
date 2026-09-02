import { useState, useEffect } from "react";
import { API_BASE } from "./common.tsx";
import NavBar from "./NavBar.tsx";
import { Download } from "lucide-react";
import ExportConfirmationForm from "./ExportConfirmationForm.tsx";
import { saveBlob } from "./exportUtils.ts";

function Generate() {
  const [schedule, setSchedule] = useState([]);
  const [filterType, setFilterType] = useState("maMonHoc");
  const [filterValue, setFilterValue] = useState("");
  const [sortConfig, setSortConfig] = useState({ key: "ngay_thi", direction: "asc" });
  const [isLoading, setIsLoading] = useState(false);
  const [subjects, setSubjects] = useState([]);
  const [selectedSubjects, setSelectedSubjects] = useState([]);
  const [dayFrom, setDayFrom] = useState("");
  const [dayTo, setDayTo] = useState("");
  const [populationSize, setPopulationSize] = useState(100);
  const [crossoverRate, setCrossoverRate] = useState(0.8);
  const [mutationRate, setMutationRate] = useState(0.25);
  const [maxGenerations, setMaxGenerations] = useState(500);
  const [validationError, setValidationError] = useState("");
  const [maxExamPerDay, setMaxExamPerDay] = useState(5);
  const [showExportForm, setShowExportForm] = useState(false);
  const [isExporting, setIsExporting] = useState(false);

  // Default slots must match scheduler defaults (ExamSchedulerService.generateTimeSlots / getDefaultTimes)
  const DEFAULT_SLOTS = ["08:00", "10:00", "13:00", "15:00", "16:30"];
  const [selectedTimeSlot, setSelectedTimeSlot] = useState("");

  useEffect(() => {
    fetch(`${API_BASE}/subjects/list`, { credentials: "include" })
      .then((res) => res.json())
      .then((data) => {
        // Process subjects to ensure unique name values
        const uniqueSubjects = [];
        const uniqueSubjectNames = new Set();

        data.forEach(sub => {
          if (!uniqueSubjectNames.has(sub.name)) {
            uniqueSubjectNames.add(sub.name);
            uniqueSubjects.push(sub);
          }
        });

        setSubjects(uniqueSubjects);
      })
      .catch((error) => console.error("Error fetching subjects:", error));
  }, []);

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
        console.log(data);
        return data; // Return successful response data
      } catch (error) {
        if (i === retries - 1) {
          throw error; // Throw error if last retry fails
        }
      }
    }
  };

  // Helper: add minutes to "HH:mm" string and return "HH:mm" (24h)
  const addMinutesToTimeString = (timeStr: string, minutesToAdd: number) => {
    const [h, m] = timeStr.split(":").map(Number);
    const dt = new Date(Date.UTC(1970, 0, 1, h, m));
    dt.setUTCMinutes(dt.getUTCMinutes() + minutesToAdd);
    const hh = String(dt.getUTCHours()).padStart(2, "0");
    const mm = String(dt.getUTCMinutes()).padStart(2, "0");
    return `${hh}:${mm}`;
  };

  const handleGenerateSchedule = (method) => {
    setValidationError("");
    if (!dayFrom || !dayTo) {
      setValidationError("Please select both Day From and Day To.");
      return;
    }

    const dateFrom = new Date(dayFrom);
    const dateTo = new Date(dayTo);

    if (dateTo < dateFrom) {
      setValidationError("Error: 'Date To' must be later than 'Date From'.");
      return;
    }

    // Ensure user selected a timeslot
    if (!selectedTimeSlot) {
      setValidationError("Please select a timeslot.");
      return;
    }

    // Compute end time as start + 90 minutes
    const slotStart = selectedTimeSlot;
    const slotEnd = addMinutesToTimeString(slotStart, 90); // 90 minutes duration

    setIsLoading(true);
    const options = {
      selectedSubjects,
      dayFrom,
      dayTo,
      // Backend expects hourFrom/hourTo strings in "HH:mm" — keep compatibility
      hourFrom: slotStart,
      hourTo: slotEnd,
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
    const allSubjects = subjects.map((subject) => subject.name);
    setSelectedSubjects(allSubjects);
  };

  const handleDeselectAll = () => {
    setSelectedSubjects([]);
  };

  const requestSort = (key) => {
    console.log("Sort with: " + key)
    let direction = sortConfig.key === key && sortConfig.direction === 'asc' ? 'desc' : 'asc';

    if (key === 'gio_thi' && sortConfig.key === 'ngay_thi') {
      direction = 'desc'; // Ensure gio_thi is descending if ngay_thi is involved
    }

    setSortConfig({ key, direction });
  };

  const sortedSchedule = [...schedule].sort((a, b) => {
    // First, sort by 'ngay_thi'
    if (a.date < b.date) return sortConfig.direction === 'asc' ? -1 : 1;
    if (a.date > b.date) return sortConfig.direction === 'asc' ? 1 : -1;

    // If 'ngay_thi' is the same, then sort by 'gio_thi'
    if (a.time < b.time) return sortConfig.direction === 'asc' ? -1 : 1;
    if (a.time > b.time) return sortConfig.direction === 'asc' ? 1 : -1;

    return 0;
  });

  const handleExportCSV = async (format: "csv" | "xlsx", fileName: string) => {
    if (schedule.length === 0) {
      alert("No schedule data to export!");
      return;
    }

    setIsExporting(true);
    try {
      const response = await fetch(`${API_BASE}/schedule/export?format=${format}`, {
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
      await saveBlob(blob, fileName, format);
      setShowExportForm(false);
    } catch (error) {
      console.error('Error exporting CSV:', error);
      alert('Failed to export schedule. Please try again.');
    } finally {
      setIsExporting(false);
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
                  <option key={sub.name} value={sub.name}>
                    {sub.name}
                  </option>
                ))}
              </select>
            </div>

            <label>Date Range:</label>
            <input type="date" value={dayFrom} onChange={(e) => setDayFrom(e.target.value)} />
            <input type="date" value={dayTo} onChange={(e) => setDayTo(e.target.value)} />

            <label>Timeslot (select start time):</label>
            <select value={selectedTimeSlot} onChange={(e) => setSelectedTimeSlot(e.target.value)}>
              <option value="">-- Select timeslot --</option>
              {DEFAULT_SLOTS.map((slot) => (
                <option key={slot} value={slot}>
                  {slot} (ends at {addMinutesToTimeString(slot, 90)})
                </option>
              ))}
            </select>
            <div style={{ fontSize: 12, color: "#666", marginTop: 6 }}>
              Each slot is 90 minutes long. The UI will send the selected start time and a computed end time.
            </div>

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
                        onClick={() => setShowExportForm(true)}
                        className="export-button"
                        title="Export"
                      >
                        <Download className="w-4 h-4 mr-2" />
                        Export
                      </button>
                    )}
                  </div>
                  <ExportConfirmationForm
                    open={showExportForm}
                    isProcessing={isExporting}
                    defaultFileName={`exam_schedule_${new Date().toISOString().split('T')[0]}`}
                    onCancel={() => setShowExportForm(false)}
                    onSubmit={({ format, fileName }) => {
                      void handleExportCSV(format, fileName);
                    }}
                  />
                </div>
                <table>
                  <thead>
                    <tr>
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
                          <td>{exam.subjectName || "N/A"}</td>
                          <td>{exam.date}</td>
                          <td>{exam.time}</td>
                          <td>{exam.endTime}</td>
                          <td>{exam.subject.duration} phút</td>
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
