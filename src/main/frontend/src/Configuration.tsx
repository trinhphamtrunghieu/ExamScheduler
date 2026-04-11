import { useRef, useState, ChangeEvent, useEffect } from "react";
import { API_BASE } from "./common.tsx";
import NavBar from "./NavBar.tsx";
import { Upload, Download } from "lucide-react";

function Configuration() {
    const expectedHeaders = ["MSSV", "Họ tên", "Mã lớp học", "Môn học", "Giáo viên"];
    const normalizeHeaderText = (value: string) => value.normalize("NFC").trim();
    const fileInputRef = useRef<HTMLInputElement>(null);
    const [isProcessing, setIsProcessing] = useState(false);
    const [isSaving, setIsSaving] = useState(false)
    const [message, setMessage] = useState("");
    const [error, setError] = useState("");
    const [pendingFile, setPendingFile] = useState<File | null>(null);
    const [detectedHeaders, setDetectedHeaders] = useState<string[]>([]);
    const [headerMapping, setHeaderMapping] = useState<Record<string, string>>({});
    const [exportFormat, setExportFormat] = useState<"csv" | "xlsx">("csv");
    const exportFormatOptions = new Set(["csv", "xlsx"]);

    const handleImportAllData = async (e: ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (!file) return;
        setPendingFile(file);
        setMessage("");
        setError("");
        const headersFormData = new FormData();
        headersFormData.append("file", file);
        try {
          const headersResponse = await fetch(`${API_BASE}/config/import/headers`, {
            method: "POST",
            credentials: "include",
            body: headersFormData,
          });
          const headerData = await headersResponse.json();
          if (!headersResponse.ok) {
            setError(headerData.error || "Cannot detect headers");
            return;
          }
          const incomingHeaders = (headerData.headers || []) as string[];
          const normalizedIncomingHeaders = incomingHeaders.map((h) => normalizeHeaderText(h));
          setDetectedHeaders(normalizedIncomingHeaders);
          const defaultMapping: Record<string, string> = {};
          expectedHeaders.forEach((expected) => {
            const expectedNormalized = normalizeHeaderText(expected);
            const exact = normalizedIncomingHeaders.find((h) => h === expectedNormalized);
            defaultMapping[expected] = exact || "";
          });
          setHeaderMapping(defaultMapping);
        } catch (err) {
          console.error("Header detection error:", err);
          setError("Cannot detect headers");
        } finally {
          if (fileInputRef.current) fileInputRef.current.value = "";
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
            if (fileInputRef.current) fileInputRef.current.value = "";
        }
    };

    const handleExportAllData = async () => {
      setIsProcessing(true);
      setMessage("");
      setError("");

      try {
        const response = await fetch(`${API_BASE}/config/export?format=${exportFormat}`, {
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
        const downloadUrl = window.URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = downloadUrl;
        const contentDisposition = response.headers.get("content-disposition");
        const fileNameMatch = contentDisposition?.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/i);
        const fileNameFromHeader = fileNameMatch?.[1]?.replace(/['"]/g, "");
        link.setAttribute(
          "download",
          fileNameFromHeader || `all_data_backup_${new Date().toISOString().split("T")[0]}.${exportFormat}`
        );
        document.body.appendChild(link);
        link.click();
        link.parentNode?.removeChild(link);
        window.URL.revokeObjectURL(downloadUrl);
        setMessage("Data exported successfully.");
      } catch (err) {
        console.error("Export error:", err);
        setError("Network error. Please try again.");
      } finally {
        setIsProcessing(false);
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

    const handleExportFormatChange = (value: string) => {
      if (exportFormatOptions.has(value)) {
        setExportFormat(value as "csv" | "xlsx");
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
                        onClick={handleExportAllData}
                        disabled={isProcessing}
                        className="flex items-center justify-center p-3 bg-green-600 hover:bg-green-700 text-white rounded-lg w-full"
                        >
                        <Download className="w-5 h-5 mr-2" />
                        {isProcessing ? "Exporting..." : "Xuất thông tin đăng ký"}
                        </button>
                        <div className="w-full">
                          <label htmlFor="config-export-format" className="block text-sm text-gray-700 mb-1">Định dạng xuất</label>
                          <select
                            id="config-export-format"
                            aria-label="Export format"
                            value={exportFormat}
                            onChange={(e) => handleExportFormatChange(e.target.value)}
                            disabled={isProcessing}
                            className="p-3 rounded-lg border border-gray-300 w-full"
                          >
                            <option value="csv">CSV</option>
                            <option value="xlsx">XLSX</option>
                          </select>
                        </div>
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
                    {pendingFile && detectedHeaders.length > 0 && (
                      <div className="confirm-header-section">
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
