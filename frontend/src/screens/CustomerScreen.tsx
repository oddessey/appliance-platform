import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { createInstallation, getApplianceTypes, uploadMoveRequestImage } from "../api/installations";
import { ApiError } from "../api/client";
import { AddressSearchInput } from "../components/AddressSearchInput";
import { CustomerQuoteLookup } from "../components/CustomerQuoteLookup";
import { DesiredDateTimeInput } from "../components/DesiredDateTimeInput";
import { InstallationHistory } from "../components/InstallationHistory";
import { RequestPhotoField } from "../components/RequestPhotoField";
import { RequestPhotoGallery } from "../components/RequestPhotoGallery";
import type {
  ApplianceType,
  InstallationResponse,
  MoveRequestImagePurpose,
} from "../types/installation";

const MAX_PHOTO_COUNT = 5;
const MAX_PHOTO_BYTES = 10 * 1024 * 1024;
const ALLOWED_PHOTO_TYPES = new Set(["image/jpeg", "image/png", "image/webp"]);
const PHOTO_TYPE_BY_EXTENSION: Record<string, string> = {
  jpg: "image/jpeg",
  jpeg: "image/jpeg",
  png: "image/png",
  webp: "image/webp",
};

interface PhotoUploadSummary {
  uploaded: number;
  failed: number;
  failures: string[];
}

interface PhotoUploadProgress {
  completed: number;
  total: number;
}

interface CustomerScreenProps {
  showHistory: boolean;
  onViewHistory: () => void;
  onViewHome: () => void;
}

export function CustomerScreen({ showHistory, onViewHistory, onViewHome }: CustomerScreenProps) {
  const [applianceTypes, setApplianceTypes] = useState<ApplianceType[]>([]);
  const [typesLoading, setTypesLoading] = useState(true);
  const [typesError, setTypesError] = useState<string | null>(null);
  const [retryCount, setRetryCount] = useState(0);
  const [requestLoading, setRequestLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<InstallationResponse | null>(null);
  const [showAdditionalPrompt, setShowAdditionalPrompt] = useState(false);
  const [formGeneration, setFormGeneration] = useState(0);
  const [appliancePhotos, setAppliancePhotos] = useState<File[]>([]);
  const [installationPhotos, setInstallationPhotos] = useState<File[]>([]);
  const [photoError, setPhotoError] = useState<string | null>(null);
  const [photoProgress, setPhotoProgress] = useState<PhotoUploadProgress | null>(null);
  const [photoUploadSummary, setPhotoUploadSummary] = useState<PhotoUploadSummary | null>(null);

  useEffect(() => {
    let active = true;
    setTypesLoading(true);
    setTypesError(null);
    getApplianceTypes()
      .then((types) => {
        if (active) setApplianceTypes(types);
      })
      .catch((requestError: unknown) => {
        if (active) setTypesError(toMessage(requestError));
      })
      .finally(() => {
        if (active) setTypesLoading(false);
      });
    return () => {
      active = false;
    };
  }, [retryCount]);

  function addPhotos(purpose: MoveRequestImagePurpose, selected: File[]) {
    setPhotoError(null);
    const normalized = selected.map(normalizePhotoFile);
    for (const file of normalized) {
      const validationError = validatePhotoFile(file);
      if (validationError) {
        setPhotoError(validationError);
        return;
      }
    }

    const current = purpose === "APPLIANCE" ? appliancePhotos : installationPhotos;
    if (current.length + normalized.length > MAX_PHOTO_COUNT) {
      setPhotoError(`사진은 종류별 최대 ${MAX_PHOTO_COUNT}장까지 첨부할 수 있습니다.`);
      return;
    }

    if (purpose === "APPLIANCE") {
      setAppliancePhotos([...current, ...normalized]);
    } else {
      setInstallationPhotos([...current, ...normalized]);
    }
  }

  function removePhoto(purpose: MoveRequestImagePurpose, index: number) {
    setPhotoError(null);
    if (purpose === "APPLIANCE") {
      setAppliancePhotos((files) => files.filter((_, fileIndex) => fileIndex !== index));
    } else {
      setInstallationPhotos((files) => files.filter((_, fileIndex) => fileIndex !== index));
    }
  }

  async function submitInstallation(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setSuccess(null);
    setPhotoError(null);
    setPhotoUploadSummary(null);
    setPhotoProgress(null);
    const formElement = event.currentTarget;
    const form = new FormData(formElement);
    const originAddress = String(form.get("originAddress") ?? "").trim();
    const destinationAddress = String(form.get("destinationAddress") ?? "").trim();
    const desiredDateTime = String(form.get("desiredStartAt") ?? "");
    const desiredStart = new Date(desiredDateTime.replace(" ", "T"));
    if (!originAddress || !destinationAddress) {
      setError("이전 주소와 설치 주소를 입력하거나 주소 검색에서 선택해 주세요.");
      return;
    }
    if (!desiredDateTime || Number.isNaN(desiredStart.getTime()) || desiredStart.getTime() <= Date.now()) {
      setError("현재 시각 이후의 희망 날짜와 시간을 선택해 주세요.");
      return;
    }

    const photosToUpload: Array<{ file: File; purpose: MoveRequestImagePurpose }> = [
      ...appliancePhotos.map((file) => ({ file, purpose: "APPLIANCE" as const })),
      ...installationPhotos.map((file) => ({ file, purpose: "INSTALLATION_ENV" as const })),
    ];
    for (const photo of photosToUpload) {
      const validationError = validatePhotoFile(photo.file);
      if (validationError) {
        setPhotoError(validationError);
        return;
      }
    }

    setRequestLoading(true);
    try {
      const response = await createInstallation({
        applianceTypeCode: String(form.get("applianceTypeCode") ?? ""),
        originAddress,
        destinationAddress,
        desiredStartAt: desiredStart.toISOString(),
        carryingDistanceMeters: Number(form.get("carryingDistanceMeters") ?? 0),
        floorCount: Number(form.get("floorCount") ?? 0),
        additionalPipingMeters: Number(form.get("additionalPipingMeters") ?? 0),
      });

      let uploaded = 0;
      const failures: string[] = [];
      if (photosToUpload.length > 0) {
        setPhotoProgress({ completed: 0, total: photosToUpload.length });
        for (let index = 0; index < photosToUpload.length; index += 1) {
          const photo = photosToUpload[index];
          try {
            await uploadMoveRequestImage(response.id, photo.file, photo.purpose);
            uploaded += 1;
          } catch (uploadError) {
            failures.push(`${photo.file.name}: ${toMessage(uploadError)}`);
          } finally {
            setPhotoProgress({ completed: index + 1, total: photosToUpload.length });
          }
        }
        setPhotoUploadSummary({ uploaded, failed: failures.length, failures });
      }

      setSuccess(response);
      setShowAdditionalPrompt(true);
      setFormGeneration((generation) => generation + 1);
      setAppliancePhotos([]);
      setInstallationPhotos([]);
      formElement.reset();
    } catch (requestError) {
      setError(toMessage(requestError));
    } finally {
      setRequestLoading(false);
      setPhotoProgress(null);
    }
  }

  if (showHistory) {
    return (
      <>
        <InstallationHistory onCreateAnother={onViewHome} />
        <CustomerInformationPanel />
      </>
    );
  }

  return (
    <>
      <div className="form-card installation-card customer-request-card">
        <div className="card-heading">
          <span className="step-label">REQUEST · 01</span>
          <h2>설치 요청하기</h2>
          <p>이전할 제품과 주소, 희망 일정을 입력해 주세요.</p>
        </div>
        <form
          className="form-stack"
          key={formGeneration}
          onInvalidCapture={() => setError("필수 항목을 입력하고 추가 작업 조건의 입력 범위를 확인해 주세요.")}
          onSubmit={(event) => void submitInstallation(event)}
        >
          <label>
            가전제품 종류 <span className="required">*</span>
            <select
              name="applianceTypeCode"
              required
              disabled={typesLoading || applianceTypes.length === 0}
              defaultValue=""
            >
              <option value="" disabled>
                {typesLoading ? "제품 종류를 불러오는 중..." : "제품을 선택해 주세요"}
              </option>
              {applianceTypes.map((type) => (
                <option key={type.code} value={type.code}>{type.name}</option>
              ))}
            </select>
          </label>

          {typesError && (
            <div className="load-error" role="alert">
              <p className="inline-error">{typesError}</p>
              <button
                className="text-button"
                onClick={() => setRetryCount((count) => count + 1)}
                type="button"
              >
                다시 불러오기
              </button>
            </div>
          )}
          {!typesLoading && !typesError && applianceTypes.length === 0 && (
            <p className="empty-state">선택 가능한 제품 종류가 없습니다.</p>
          )}

          <AddressSearchInput label="이전 주소" name="originAddress" />
          <AddressSearchInput label="설치 주소" name="destinationAddress" />
          <DesiredDateTimeInput name="desiredStartAt" />
          <div className="request-photo-section">
            <div className="request-photo-section-heading">
              <strong>사진 첨부 <small>(선택)</small></strong>
              <p>제품 상태와 설치 환경을 미리 보여주시면 더 정확한 상담이 가능합니다.</p>
            </div>
            <div className="request-photo-grid">
              <RequestPhotoField
                description="이전할 제품 전체가 보이도록 촬영해 주세요."
                disabled={requestLoading}
                files={appliancePhotos}
                label="제품 사진"
                maxFiles={MAX_PHOTO_COUNT}
                onFilesSelected={(files) => addPhotos("APPLIANCE", files)}
                onRemove={(index) => removePhoto("APPLIANCE", index)}
              />
              <RequestPhotoField
                description="설치 위치와 배관·전원 주변을 촬영해 주세요."
                disabled={requestLoading}
                files={installationPhotos}
                label="설치 환경 사진"
                maxFiles={MAX_PHOTO_COUNT}
                onFilesSelected={(files) => addPhotos("INSTALLATION_ENV", files)}
                onRemove={(index) => removePhoto("INSTALLATION_ENV", index)}
              />
            </div>
            {photoError && <p className="form-error" role="alert">{photoError}</p>}
          </div>
          <div className="installation-conditions">
            <strong>추가 작업 조건 <small>(선택)</small></strong>
            <p>입력한 조건은 관리자가 발행하는 견적 산출에 사용됩니다.</p>
            <div className="installation-condition-grid">
              <label>
                운반 거리 (m)
                <input defaultValue={0} min={0} max={10000} name="carryingDistanceMeters" type="number" />
              </label>
              <label>
                작업 층수 (층)
                <input defaultValue={0} min={0} max={200} name="floorCount" type="number" />
              </label>
              <label>
                추가 배관 길이 (m)
                <input
                  defaultValue={0}
                  min={0}
                  max={1000}
                  name="additionalPipingMeters"
                  step="0.01"
                  type="number"
                />
              </label>
            </div>
          </div>

          {error && <p className="form-error" role="alert">{error}</p>}
          {success && (
            <div className="success-message" role="status">
              <strong>설치 요청이 접수되었습니다.</strong>
              <span>요청 번호: {success.requestNo}</span>
              {photoUploadSummary && photoUploadSummary.uploaded > 0 && (
                <span>사진 {photoUploadSummary.uploaded}장이 첨부되었습니다.</span>
              )}
              {photoUploadSummary && photoUploadSummary.failed > 0 && (
                <span className="photo-upload-warning">
                  사진 {photoUploadSummary.failed}장을 첨부하지 못했습니다. {photoUploadSummary.failures[0]}
                  {photoUploadSummary.failed > 1 ? ` 외 ${photoUploadSummary.failed - 1}건` : ""}
                </span>
              )}
            </div>
          )}
          <button
            className="primary-button"
            disabled={requestLoading || typesLoading || applianceTypes.length === 0}
            type="submit"
          >
            {requestLoading
              ? photoProgress
                ? `사진 첨부 중... (${photoProgress.completed}/${photoProgress.total})`
                : "요청을 접수하고 있어요..."
              : "설치 요청 접수"}
            {!requestLoading && <span aria-hidden="true">→</span>}
          </button>
          <p className="privacy-note">입력하신 정보는 설치 상담과 서비스 제공을 위해 사용됩니다.</p>
        </form>
      </div>

      <CustomerInformationPanel />
      <CustomerQuoteLookup refreshKey={formGeneration} onViewHistory={onViewHistory} />

      {showAdditionalPrompt && success && (
        <div className="popup-backdrop">
          <section
            aria-labelledby="additional-request-title"
            aria-modal="true"
            className="popup-card additional-request-dialog"
            role="dialog"
          >
            <div className="card-heading">
              <span className="step-label">REQUEST RECEIVED</span>
              <h2 id="additional-request-title">설치 요청이 접수되었습니다.</h2>
              <p>요청 번호: {success.requestNo}</p>
              {photoUploadSummary && photoUploadSummary.failed > 0 && (
                <p className="inline-error">
                  사진 {photoUploadSummary.failed}장을 첨부하지 못했습니다. 접수 내역에서 요청 상태를 확인해 주세요.
                </p>
              )}
              <p>추가로 설치 요청을 접수하시겠습니까?</p>
            </div>
            <RequestPhotoGallery
              description="접수한 요청에 첨부된 사진과 파일 정보를 확인할 수 있습니다."
              emptyMessage="첨부한 사진이 없습니다."
              moveRequestId={success.id}
              title="첨부한 사진"
            />
            <div className="additional-request-actions">
              <button
                className="primary-button"
                onClick={() => {
                  setShowAdditionalPrompt(false);
                  setSuccess(null);
                  setError(null);
                  setPhotoError(null);
                  setPhotoUploadSummary(null);
                }}
                type="button"
              >
                추가 접수하기
              </button>
              <button
                className="secondary-button"
                onClick={() => {
                  setShowAdditionalPrompt(false);
                  onViewHistory();
                }}
                type="button"
              >
                아니요, 접수 내역 확인
              </button>
            </div>
          </section>
        </div>
      )}
    </>
  );
}

function CustomerInformationPanel() {
  return (
    <aside className="info-panel customer-info-panel">
      <span className="step-label">HOW IT WORKS</span>
      <h2>요청은 간단하게,<br />이전은 안심하고.</h2>
      <ol className="process-list">
        <li><span>1</span><div><strong>제품 선택</strong><small>이전할 가전 종류를 알려주세요.</small></div></li>
        <li><span>2</span><div><strong>이동 정보 입력</strong><small>출발지와 도착지를 입력해 주세요.</small></div></li>
        <li><span>3</span><div><strong>일정 선택</strong><small>편하신 날짜와 시간을 선택해 주세요.</small></div></li>
      </ol>
      <div className="support-note">
        <span aria-hidden="true">✦</span>
        <p>요청 접수 후 담당자가<br />일정을 확인해 안내드릴게요.</p>
      </div>
    </aside>
  );
}

function normalizePhotoFile(file: File): File {
  const extension = file.name.split(".").pop()?.toLowerCase() ?? "";
  const inferredType = PHOTO_TYPE_BY_EXTENSION[extension];
  const type = file.type === "image/jpg"
    ? "image/jpeg"
    : file.type && file.type !== "application/octet-stream"
      ? file.type
      : inferredType ?? file.type;
  if (!type || type === file.type) return file;
  return new File([file], file.name, { type, lastModified: file.lastModified });
}

function validatePhotoFile(file: File): string | null {
  if (file.size === 0) {
    return `${file.name}: 빈 파일은 첨부할 수 없습니다.`;
  }
  if (file.size > MAX_PHOTO_BYTES) {
    return `${file.name}: 사진은 장당 10MB 이하만 첨부할 수 있습니다.`;
  }
  if (!ALLOWED_PHOTO_TYPES.has(file.type)) {
    return `${file.name}: JPEG, PNG, WebP 이미지만 첨부할 수 있습니다.`;
  }
  return null;
}

function toMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.fieldErrors?.length) {
      return error.fieldErrors.map((fieldError) => fieldError.reason).join(" ");
    }
    return error.message;
  }
  if (error instanceof Error) return error.message;
  return "요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.";
}
