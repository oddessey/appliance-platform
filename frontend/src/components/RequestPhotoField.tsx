import { useEffect, useId, useState } from "react";

interface RequestPhotoFieldProps {
  description: string;
  disabled: boolean;
  files: File[];
  label: string;
  maxFiles: number;
  onFilesSelected: (files: File[]) => void;
  onRemove: (index: number) => void;
}

export function RequestPhotoField({
  description,
  disabled,
  files,
  label,
  maxFiles,
  onFilesSelected,
  onRemove,
}: RequestPhotoFieldProps) {
  const inputId = useId();
  const canAdd = !disabled && files.length < maxFiles;

  return (
    <section aria-labelledby={`${inputId}-label`} className="request-photo-field">
      <div className="request-photo-field-heading">
        <div>
          <strong id={`${inputId}-label`}>{label} <small>(선택)</small></strong>
          <p>{description}</p>
        </div>
        <span className="request-photo-count">{files.length}/{maxFiles}</span>
      </div>

      <div className="request-photo-picker-row">
        <label className={`request-photo-picker${canAdd ? "" : " is-disabled"}`} htmlFor={inputId}>
          {files.length === 0 ? "사진 선택" : "사진 추가"}
          <input
            accept="image/jpeg,image/png,image/webp"
            disabled={!canAdd}
            id={inputId}
            multiple
            onChange={(event) => {
              const selected = Array.from(event.currentTarget.files ?? []);
              event.currentTarget.value = "";
              if (selected.length > 0) onFilesSelected(selected);
            }}
            type="file"
          />
        </label>
        <span className="request-photo-hint">JPEG, PNG, WebP · 장당 10MB 이하</span>
      </div>

      {files.length === 0 ? (
        <p className="request-photo-empty">선택한 사진이 없습니다.</p>
      ) : (
        <ul className="request-photo-list">
          {files.map((file, index) => (
            <PhotoPreview
              disabled={disabled}
              file={file}
              key={`${file.name}-${file.size}-${file.lastModified}-${index}`}
              label={label}
              onRemove={() => onRemove(index)}
            />
          ))}
        </ul>
      )}
    </section>
  );
}

interface PhotoPreviewProps {
  disabled: boolean;
  file: File;
  label: string;
  onRemove: () => void;
}

function PhotoPreview({ disabled, file, label, onRemove }: PhotoPreviewProps) {
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);

  useEffect(() => {
    const url = URL.createObjectURL(file);
    setPreviewUrl(url);
    return () => URL.revokeObjectURL(url);
  }, [file]);

  return (
    <li className="request-photo-item">
      <div className="request-photo-thumb">
        {previewUrl && <img alt={`${file.name} 미리보기`} src={previewUrl} />}
      </div>
      <div className="request-photo-file">
        <strong title={file.name}>{file.name}</strong>
        <span>{formatFileSize(file.size)}</span>
      </div>
      <button
        aria-label={`${label} ${file.name} 삭제`}
        className="request-photo-remove"
        disabled={disabled}
        onClick={onRemove}
        type="button"
      >
        삭제
      </button>
    </li>
  );
}

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}
