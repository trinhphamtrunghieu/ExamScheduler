import { useEffect, useState } from "react";
import { ExportFormat, SaveFileHandleLike, chooseSaveLocation, ensureExtension } from "./exportUtils";

type ExportConfirmationFormProps = {
  open: boolean;
  isProcessing: boolean;
  defaultFileName: string;
  onCancel: () => void;
  onSubmit: (payload: { format: ExportFormat; fileName: string; saveHandle: SaveFileHandleLike | null }) => void;
};

function ExportConfirmationForm({
  open,
  isProcessing,
  defaultFileName,
  onCancel,
  onSubmit,
}: ExportConfirmationFormProps) {
  const [format, setFormat] = useState<ExportFormat>("csv");
  const [fileName, setFileName] = useState(defaultFileName);
  const [saveHandle, setSaveHandle] = useState<SaveFileHandleLike | null>(null);
  const [locationLabel, setLocationLabel] = useState("Chưa chọn vị trí");

  useEffect(() => {
    if (!open) return;
    setFormat("csv");
    setFileName(defaultFileName);
    setSaveHandle(null);
    setLocationLabel("Chưa chọn vị trí");
  }, [open, defaultFileName]);

  const handleChooseLocation = async () => {
    try {
      const handle = await chooseSaveLocation(fileName, format);
      if (!handle) {
        setLocationLabel("Trình duyệt sẽ dùng thư mục tải xuống mặc định");
        return;
      }
      setSaveHandle(handle);
      setLocationLabel(handle.name || ensureExtension(fileName, format));
    } catch (error) {
      console.error("Choose save location error:", error);
      setLocationLabel("Không thể chọn vị trí lưu");
    }
  };

  if (!open) return null;

  return (
    <div className="export-confirmation-section">
      <div className="export-confirmation-row">
        <label className="export-confirmation-label" htmlFor="export-format">Định dạng</label>
        <select
          id="export-format"
          className="export-confirmation-input"
          value={format}
          onChange={(e) => setFormat(e.target.value as ExportFormat)}
          disabled={isProcessing}
        >
          <option value="csv">CSV</option>
          <option value="xlsx">XLSX</option>
        </select>
      </div>
      <div className="export-confirmation-row">
        <label className="export-confirmation-label" htmlFor="export-file-name">Tên file</label>
        <input
          id="export-file-name"
          className="export-confirmation-input"
          value={fileName}
          onChange={(e) => setFileName(e.target.value)}
          disabled={isProcessing}
        />
      </div>
      <div className="export-confirmation-row">
        <span className="export-confirmation-label">Vị trí lưu</span>
        <button
          type="button"
          onClick={handleChooseLocation}
          disabled={isProcessing}
          className="export-location-button"
        >
          Chọn vị trí lưu
        </button>
      </div>
      <div className="export-location-label">{locationLabel}</div>
      <div className="export-confirmation-actions">
        <button
          type="button"
          onClick={onCancel}
          disabled={isProcessing}
          className="export-cancel-button"
        >
          Hủy
        </button>
        <button
          type="button"
          onClick={() => onSubmit({ format, fileName, saveHandle })}
          disabled={isProcessing}
          className="export-submit-button"
        >
          {isProcessing ? "Đang xuất..." : "Xác nhận xuất"}
        </button>
      </div>
    </div>
  );
}

export default ExportConfirmationForm;

