import { useRef, useState, ChangeEvent, useEffect } from "react";
import { API_BASE } from "./common.tsx";
import NavBar from "./NavBar.tsx";
import { Upload, Download } from "lucide-react";

function Configuration() {
    const fileInputRef = useRef<HTMLInputElement>(null);
    const [isProcessing, setIsProcessing] = useState(false);
    const [isSaving, setIsSaving] = useState(false)
    const [message, setMessage] = useState("");
    const [error, setError] = useState("");

    const handleImportAllData = async (e: ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (!file) return;

        setIsProcessing(true);
        setMessage("");
        setError("");

        const formData = new FormData();
        formData.append("file", file);

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
            if (fileInputRef.current) fileInputRef.current.value = "";
        }
    };

    const handleExportAllData = async () => {
      setIsProcessing(true);
      setMessage("");
      setError("");

      try {
        const response = await fetch(`${API_BASE}/config/export`, {
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
        link.setAttribute(
          "download",
          `all_data_backup_${new Date().toISOString().split("T")[0]}.csv`
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
                        <div className="import-export-button-container">
                            <input
                            type="file"
                            ref={fileInputRef}
                            onChange={handleImportAllData}
                            accept=".csv"
                            className="hidden"
                            disabled={isProcessing}
                            />

                            <button
                            onClick={() => fileInputRef.current?.click()}
                            disabled={isProcessing}
                            className="flex items-center justify-center p-3 bg-blue-600 hover:bg-blue-700 text-white rounded-lg w-full"
                            >
                            <Upload className="w-5 h-5 mr-2" />
                            {isProcessing ? "Importing..." : "Import All Data"}
                            </button>

                            <button
                            onClick={handleExportAllData}
                            disabled={isProcessing}
                            className="flex items-center justify-center p-3 bg-green-600 hover:bg-green-700 text-white rounded-lg w-full"
                            >
                            <Download className="w-5 h-5 mr-2" />
                            {isProcessing ? "Exporting..." : "Export All Data"}
                            </button>
                            <button
                                onClick={handleSaveConfig}
                                disabled={isSaving}
                                className="flex items-center justify-center p-3 bg-purple-600 hover:bg-purple-700 text-white rounded-lg w-full"
                            >
                            <span className="ml-2">
                                {isSaving ? "Saving..." : "Save Data to DB"}
                            </span>
                            </button>
                        </div>
                            {message && <div className="text-green-600">{message}</div>}
                            {error && <div className="text-red-500">{error}</div>}
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
  }
export default Configuration;