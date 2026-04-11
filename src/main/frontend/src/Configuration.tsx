import { useRef, useState, ChangeEvent } from "react";
import { API_BASE } from "./common.tsx";
import NavBar from "./NavBar.tsx";
import { Upload, Download } from "lucide-react";
import ExportConfirmationForm from "./ExportConfirmationForm.tsx";
import { saveBlob } from "./exportUtils.ts";

function Configuration() {
    const expectedHeaders = ["MSSV", "Họ tên", "Mã lớp học", "Môn học", "Giáo viên"];
    const normalizeHeaderText = (value: string) => value.normalize("NFC").trim();
    const fileInputRef = useRef<HTMLInputElement>(null);
    const [isProcessing, setIsProcessing] = useState(false);
    const [isSaving, setIsSaving] = useState(false)
    const [isExporting, setIsExporting] = useState(false);
    const [message, setMessage] = useState("");
    const [error, setError] = useState("");
    const [pendingFile, setPendingFile] = useState<File | null>(null);
    const [detectedHeaders, setDetectedHeaders] = useState<string[]>([]);
    const [headerMapping, setHeaderMapping] = useState<Record<string, string>>({});
    const [availableSheets, setAvailableSheets] = useState<string[]>([]);
    const [selectedSheet, setSelectedSheet] = useState("");
    const [showExportForm, setShowExportForm] = useState(false);

    const detectImportHeaders = async (file: File, sheetName?: string) => {
      const headersFormData = new FormData();
      headersFormData.append("file", file);
      if (sheetName) {
        headersFormData.append("sheetName", sheetName);
      }

      const headersResponse = await fetch(`${API_BASE}/config/import/headers`, {
        method: "POST",
        credentials: "include",
        body: headersFormData,
      });
      const headerData = await headersResponse.json();
      if (!headersResponse.ok) {
        throw new Error(headerData.error || "Cannot detect headers");
      }

      const incomingHeaders = (headerData.headers || []) as string[];
      const normalizedIncomingHeaders = incomingHeaders.map((h) => normalizeHeaderText(h));
      setDetectedHeaders(normalizedIncomingHeaders);

      const incomingSheets = (headerData.sheetNames || []) as string[];
      setAvailableSheets(incomingSheets);
      if (incomingSheets.length === 0) {
        setSelectedSheet("");
      } else if (sheetName && incomingSheets.includes(sheetName)) {
        setSelectedSheet(sheetName);
      } else if (!incomingSheets.includes(selectedSheet)) {
        setSelectedSheet(incomingSheets[0]);
      }

      const defaultMapping: Record<string, string> = {};
      expectedHeaders.forEach((expected) => {
        const expectedNormalized = normalizeHeaderText(expected);
        const exact = normalizedIncomingHeaders.find((h) => h === expectedNormalized);
        defaultMapping[expected] = exact || "";
      });
      setHeaderMapping(defaultMapping);
    };

    const handleImportAllData = async (e: ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (!file) return;
        setPendingFile(file);
        setMessage("");
        setError("");
        setDetectedHeaders([]);
        setHeaderMapping({});
        setAvailableSheets([]);
        setSelectedSheet("");
        setShowExportForm(false);
        try {
          await detectImportHeaders(file);
        } catch (err) {
          console.error("Header detection error:", err);
          setError("Cannot detect headers");
        } finally {
          if (fileInputRef.current) fileInputRef.current.value = "";
        }
    };

    const handleSheetChange = async (sheetName: string) => {
      if (!pendingFile) return;
      setSelectedSheet(sheetName);
      setError("");
      setMessage("");
      try {
        await detectImportHeaders(pendingFile, sheetName);
      } catch (err) {
        console.error("Header detection error:", err);
        setError("Cannot detect headers");
      }
    };

    const handleConfirmImportAllData = async () => {
        if (!pendingFile) return;
        setIsProcessing(true);
        setMessage("");
        setError("");

        const formData = new FormData();
        formData.append("file", pendingFile);
        const serializedMapping = Object.entries(headerMapping)
          .filter(([, actual]) => !!actual)
          .map(([expected, actual]) => `${expected}=${actual}`)
          .join(";");
        formData.append("headerMapping", serializedMapping);
        if (selectedSheet) {
          formData.append("sheetName", selectedSheet);
        }

        try {
            const response = await fetch(`${API_BASE}/config/import`, {
            method: "POST",
            credentials: "include",
            body: formData,
            });

            const data = await response.json();
            if (response.ok) {
                setMessage(data.message || "Data imported successfully.");
            } else {
                setError(data.error || "Import failed.");
            }
        } catch (err) {
            console.error("Import error:", err);
            setError("Network error. Please try again.");
        } finally {
            setIsProcessing(false);
            setPendingFile(null);
            setDetectedHeaders([]);
            setHeaderMapping({});
            setAvailableSheets([]);
            setSelectedSheet("");
            if (fileInputRef.current) fileInputRef.current.value = "";
        }
    };

    const handleExportAllData = async (format: "csv" | "xlsx", fileName: string) => {
      setIsExporting(true);
      setMessage("");
      setError("");

      try {
        const response = await fetch(`${API_BASE}/config/export?format=${format}`, {
          method: "GET",
          credentials: "include",
        });
        console.log(response);

        if (!response.ok) {
          // Try to extract error message from JSON response
          const contentType = response.headers.get("content-type");
          if (contentType && contentType.includes("application/json")) {
            const errorData = await response.json();
            setError(errorData.error || "Failed to export data.");
          } else {
            setError("Failed to export data.");
          }
          return;
        }

        const blob = await response.blob();
        await saveBlob(blob, fileName, format);
        setMessage("Data exported successfully.");
        setShowExportForm(false);
      } catch (err) {
        console.error("Export error:", err);
        setError("Network error. Please try again.");
      } finally {
        setIsExporting(false);
      }
    };

    const handleSaveConfig = async () => {
        setIsSaving(true);
        setMessage("");
        setError("");

        try {
            const response = await fetch(`${API_BASE}/config/save`, {
            method: "POST",
            credentials: "include",
            });

            const data = await response.json();
            if (response.ok) {
                setMessage(data.message || "Configuration saved successfully.");
            } else {
                setError(data.error || "Failed to save configuration.");
            }
        } catch (err) {
            console.error("Save config error:", err);
            setError("Network error. Please try again.");
        } finally {
            setIsSaving(false);
        }
    };

    return (
        <div id="root" className="flex">
            <NavBar /> {/* NavBar */}

            <div className="spacing"></div> {/* Spacing */}

            <div className="content-area">
                <div className="p-6 bg-gray-50 min-h-screen flex flex-col items-center">

                {/* Filter Form */}
                <div className="result-section">
                    <div className="result-header">
                        <h2>Configuration</h2>
                    </div>
                    <div className="import-export-button-container">
                        <input
                        type="file"
                        ref={fileInputRef}
                        onChange={handleImportAllData}
                        accept=".csv,.xlsx"
                        className="hidden"
                        disabled={isProcessing}
                        />

                        <button
                        onClick={() => fileInputRef.current?.click()}
                        disabled={isProcessing}
                        className="flex items-center justify-center p-3 bg-blue-600 hover:bg-blue-700 text-white rounded-lg w-full"
                        >
                        <Upload className="w-5 h-5 mr-2" />
                        {isProcessing ? "Importing..." : "Nhập thông tin đăng ký"}
                        </button>
                        <button
                        onClick={() => setShowExportForm(true)}
                        disabled={isProcessing || isExporting}
                        className="flex items-center justify-center p-3 bg-green-600 hover:bg-green-700 text-white rounded-lg w-full"
                        >
                        <Download className="w-5 h-5 mr-2" />
                        {isExporting ? "Đang xuất..." : "Xuất thông tin đăng ký"}
                        </button>
                        <button
                            onClick={handleSaveConfig}
                            disabled={isSaving}
                            className="flex items-center justify-center p-3 bg-purple-600 hover:bg-purple-700 text-white rounded-lg w-full"
                        >
                        <span className="ml-2">
                            {isSaving ? "Saving..." : "Lưu toàn bộ thông tin"}
                        </span>
                        </button>
                    </div>
                    <ExportConfirmationForm
                      open={showExportForm}
                      isProcessing={isExporting}
                      defaultFileName={`all_data_backup_${new Date().toISOString().split("T")[0]}`}
                      onCancel={() => setShowExportForm(false)}
                      onSubmit={({ format, fileName }) => {
                        void handleExportAllData(format, fileName);
                      }}
                    />
                    {pendingFile && availableSheets.length > 0 && (
                      <div className="import-sheet-section">
                        <label htmlFor="config-import-sheet" className="export-format-label">Sheet name</label>
                        <select
                          id="config-import-sheet"
                          aria-label="Import sheet name"
                          value={selectedSheet}
                          onChange={(e) => handleSheetChange(e.target.value)}
                          disabled={isProcessing}
                          className="export-format-select"
                        >
                          {availableSheets.map((sheetName) => (
                            <option key={sheetName} value={sheetName}>{sheetName}</option>
                          ))}
                        </select>
                      </div>
                    )}
                    {pendingFile && detectedHeaders.length > 0 && (
                      <div className="confirm-header-section confirm-header-section-left">
                        {expectedHeaders.map((expected) => (
                          <div key={expected} className="confirm-header-row">
                            <label className="confirm-header-label">{expected}</label>
                            <select
                              className="confirm-header-select"
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
                          onClick={handleConfirmImportAllData}
                          disabled={isProcessing || expectedHeaders.some((h) => !headerMapping[h])}
                          className="confirm-import-button"
                        >
                          {isProcessing ? "Importing..." : "Confirm Import"}
                        </button>
                    </div>
                    )}
                    {message && <div className="text-green-600">{message}</div>}
                    {error && <div className="text-red-500">{error}</div>}
                </div>
                </div>
            </div>
        </div>
    );
  }
export default Configuration;
