import { useEffect, useState } from "react";
import { ExportFormat } from "./exportUtils";

type ExportConfirmationFormProps = {
  open: boolean;
  isProcessing: boolean;
  defaultFileName: string;
  onCancel: () => void;
  onSubmit: (payload: { format: ExportFormat; fileName: string }) => void;
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

  useEffect(() => {
    if (!open) return;
    setFormat("csv");
    setFileName(defaultFileName);
  }, [open, defaultFileName]);

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
          onClick={() => onSubmit({ format, fileName })}
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
