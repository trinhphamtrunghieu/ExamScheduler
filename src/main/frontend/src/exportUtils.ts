export type ExportFormat = "csv" | "xlsx";

type SaveWriterLike = {
  write: (data: Blob) => Promise<void>;
  close: () => Promise<void>;
};

export type SaveFileHandleLike = {
  name?: string;
  createWritable: () => Promise<SaveWriterLike>;
};

type SavePickerWindow = Window & {
  showSaveFilePicker?: (options?: {
    suggestedName?: string;
    types?: Array<{ description?: string; accept: Record<string, string[]> }>;
  }) => Promise<SaveFileHandleLike>;
};

export const ensureExtension = (fileName: string, format: ExportFormat) => {
  const trimmed = fileName.trim();
  if (!trimmed) return `export.${format}`;
  const lower = trimmed.toLowerCase();
  if (lower.endsWith(`.${format}`)) return trimmed;
  return `${trimmed}.${format}`;
};

const acceptedTypes = (format: ExportFormat) =>
  format === "xlsx"
    ? [{ description: "Excel Workbook", accept: { "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet": [".xlsx"] } }]
    : [{ description: "CSV File", accept: { "text/csv": [".csv"] } }];

export const chooseSaveLocation = async (
  suggestedName: string,
  format: ExportFormat
): Promise<SaveFileHandleLike | null> => {
  const pickerWindow = window as SavePickerWindow;
  if (!pickerWindow.showSaveFilePicker) return null;
  return pickerWindow.showSaveFilePicker({
    suggestedName: ensureExtension(suggestedName, format),
    types: acceptedTypes(format),
  });
};

export const saveBlob = async (
  blob: Blob,
  fileName: string,
  format: ExportFormat,
  saveHandle?: SaveFileHandleLike | null
) => {
  const finalFileName = ensureExtension(fileName, format);
  const handle = saveHandle || (await chooseSaveLocation(finalFileName, format));
  if (handle) {
    const writable = await handle.createWritable();
    await writable.write(blob);
    await writable.close();
    return;
  }
  const downloadUrl = window.URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = downloadUrl;
  link.setAttribute("download", finalFileName);
  document.body.appendChild(link);
  link.click();
  link.parentNode?.removeChild(link);
  window.URL.revokeObjectURL(downloadUrl);
};

