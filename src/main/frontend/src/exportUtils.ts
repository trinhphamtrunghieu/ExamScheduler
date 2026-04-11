export type ExportFormat = "csv" | "xlsx";

export const ensureExtension = (fileName: string, format: ExportFormat) => {
  const trimmed = fileName.trim();
  if (!trimmed) return `export.${format}`;
  const lower = trimmed.toLowerCase();
  if (lower.endsWith(`.${format}`)) return trimmed;
  return `${trimmed}.${format}`;
};

export const saveBlob = async (
  blob: Blob,
  fileName: string,
  format: ExportFormat
) => {
  const finalFileName = ensureExtension(fileName, format);
  const downloadUrl = window.URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = downloadUrl;
  link.setAttribute("download", finalFileName);
  document.body.appendChild(link);
  link.click();
  link.parentNode?.removeChild(link);
  window.URL.revokeObjectURL(downloadUrl);
};

