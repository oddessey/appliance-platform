import { useEffect, useState } from "react";
import { ApiError } from "../api/client";
import { downloadMoveRequestImage, getMoveRequestImages } from "../api/installations";
import type { MoveRequestImage } from "../types/installation";

interface RequestPhotoGalleryProps {
  moveRequestId: string;
  title?: string;
  description?: string;
  emptyMessage?: string;
}

interface PhotoPreview {
  image: MoveRequestImage;
  url: string | null;
}

export function RequestPhotoGallery({
  moveRequestId,
  title = "고객 첨부 사진",
  description = "제품 상태와 설치 환경을 확인하세요.",
  emptyMessage = "첨부된 사진이 없습니다.",
}: RequestPhotoGalleryProps) {
  const [photos, setPhotos] = useState<PhotoPreview[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let cancelled = false;
    const createdUrls: string[] = [];

    async function loadPhotos() {
      setLoading(true);
      setError(null);
      setPhotos([]);
      try {
        const images = await getMoveRequestImages(moveRequestId);
        const previews = await Promise.all(images.map(async (image) => {
          try {
            const blob = await downloadMoveRequestImage(moveRequestId, image.id);
            const url = URL.createObjectURL(blob);
            if (cancelled) {
              URL.revokeObjectURL(url);
              return { image, url: null };
            }
            createdUrls.push(url);
            return { image, url };
          } catch {
            return { image, url: null };
          }
        }));
        if (!cancelled) {
          setPhotos(previews);
        }
      } catch (requestError) {
        if (!cancelled) {
          setError(toMessage(requestError));
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    void loadPhotos();
    return () => {
      cancelled = true;
      createdUrls.forEach((url) => URL.revokeObjectURL(url));
    };
  }, [moveRequestId, reloadKey]);

  return (
    <section aria-label="고객 첨부 사진" className="request-photo-gallery">
      <div className="request-photo-gallery-heading">
        <div>
          <strong>{title}</strong>
          <p>{description}</p>
        </div>
        <span>{loading ? "불러오는 중..." : `${photos.length}장`}</span>
      </div>

      {error && (
        <div className="request-photo-gallery-error">
          <p className="inline-error" role="alert">{error}</p>
          <button className="text-button" onClick={() => setReloadKey((current) => current + 1)} type="button">
            다시 시도
          </button>
        </div>
      )}
      {!error && loading && <p className="request-photo-gallery-empty">사진을 불러오는 중...</p>}
      {!error && !loading && photos.length === 0 && (
        <p className="request-photo-gallery-empty">{emptyMessage}</p>
      )}
      {photos.length > 0 && (
        <ul className="request-photo-gallery-list">
          {photos.map(({ image, url }) => (
            <li className="request-photo-gallery-item" key={image.id}>
              {url ? (
                <a
                  className="request-photo-gallery-thumb"
                  href={url}
                  rel="noreferrer"
                  target="_blank"
                  title={`${image.originalFilename} 크게 보기`}
                >
                  <img alt={`${image.originalFilename} 미리보기`} src={url} />
                </a>
              ) : (
                <span className="request-photo-gallery-thumb is-unavailable">미리보기 불가</span>
              )}
              <div className="request-photo-gallery-meta">
                <strong title={image.originalFilename}>{image.originalFilename}</strong>
                <span>{purposeLabel(image.purpose)} · {formatFileSize(image.sizeBytes)}</span>
                <time dateTime={image.uploadedAt}>{formatDate(image.uploadedAt)}</time>
              </div>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

function purposeLabel(purpose: MoveRequestImage["purpose"]): string {
  return purpose === "INSTALLATION_ENV" ? "설치 환경" : "제품 사진";
}

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function formatDate(value: string): string {
  return new Intl.DateTimeFormat("ko-KR", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

function toMessage(error: unknown): string {
  if (error instanceof ApiError) {
    return error.message;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return "첨부 사진을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.";
}
