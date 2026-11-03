import { useId, useState } from "react";
import { recognizeProduct } from "../api/installations";
import { ApiError } from "../api/client";
import type { ProductRecognitionResult } from "../types/installation";

const MAX_IMAGE_BYTES = 10 * 1024 * 1024;
const ALLOWED_IMAGE_TYPES = new Set(["image/jpeg", "image/png", "image/webp"]);

interface ProductRecognitionPanelProps {
  disabled: boolean;
  onRecognized: (result: ProductRecognitionResult) => void;
}

export function ProductRecognitionPanel({ disabled, onRecognized }: ProductRecognitionPanelProps) {
  const inputId = useId();
  const [file, setFile] = useState<File | null>(null);
  const [result, setResult] = useState<ProductRecognitionResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submitRecognition() {
    if (!file) {
      setError("제품 사진 또는 QR 코드 사진을 먼저 선택해 주세요.");
      return;
    }

    setLoading(true);
    setError(null);
    setResult(null);
    try {
      const recognition = await recognizeProduct(file);
      setResult(recognition);
      onRecognized(recognition);
    } catch (requestError) {
      setError(toMessage(requestError));
    } finally {
      setLoading(false);
    }
  }

  return (
    <section aria-labelledby={`${inputId}-title`} className="product-recognition-panel">
      <div className="product-recognition-heading">
        <div>
          <strong id={`${inputId}-title`}>AI 제품 정보 찾기 <small>(선택)</small></strong>
          <p>제품 전체 사진이나 모델 라벨·QR 코드를 찍으면 제품 정보를 확인합니다.</p>
        </div>
      </div>
      <div className="product-recognition-controls">
        <input
          accept="image/jpeg,image/png,image/webp"
          aria-label="제품 사진 또는 QR 이미지 선택"
          capture="environment"
          disabled={disabled || loading}
          id={inputId}
          onChange={(event) => {
            const selected = event.currentTarget.files?.[0] ?? null;
            event.currentTarget.value = "";
            setResult(null);
            setError(null);
            if (selected && selected.size > MAX_IMAGE_BYTES) {
              setFile(null);
              setError("이미지는 10MB 이하만 선택할 수 있습니다.");
            } else if (selected && !ALLOWED_IMAGE_TYPES.has(selected.type)) {
              setFile(null);
              setError("JPEG, PNG 또는 WebP 이미지만 선택할 수 있습니다.");
            } else {
              setFile(selected);
            }
          }}
          type="file"
        />
        <button
          className="secondary-button"
          disabled={disabled || loading || !file}
          onClick={() => void submitRecognition()}
          type="button"
        >
          {loading ? "제품 정보를 분석하고 있어요..." : "제품 정보 인식"}
        </button>
        <span className="product-recognition-filename">{file?.name ?? "선택한 사진 없음"}</span>
      </div>
      <p className="product-recognition-privacy">
        선택한 사진과 QR 정보는 분석을 위해 OpenAI로 전송됩니다.
      </p>
      {error && <p className="form-error" role="alert">{error}</p>}
      {result && (
        <div className="product-recognition-result" aria-live="polite">
          <strong>인식 결과</strong>
          <dl>
            <div><dt>제품</dt><dd>{result.productName ?? "확인되지 않음"}</dd></div>
            <div><dt>종류</dt><dd>{productTypeName(result.productType)}</dd></div>
            <div><dt>제조사</dt><dd>{result.manufacturer ?? "확인되지 않음"}</dd></div>
            <div><dt>모델명</dt><dd>{result.modelNumber ?? "확인되지 않음"}</dd></div>
            <div><dt>시리얼 번호</dt><dd>{result.serialNumber ?? "확인되지 않음"}</dd></div>
            <div><dt>용량</dt><dd>{result.capacity ?? "확인되지 않음"}</dd></div>
            {result.confidence !== null && (
              <div><dt>인식 신뢰도</dt><dd>{Math.round(result.confidence * 100)}%</dd></div>
            )}
          </dl>
          {result.keyFeatures.length > 0 && (
            <p className="product-recognition-features">{result.keyFeatures.join(" · ")}</p>
          )}
          {result.qrCodeValue && (
            <details className="product-recognition-qr">
              <summary>인식한 QR 정보</summary>
              <code>{result.qrCodeValue}</code>
            </details>
          )}
          <p className="product-recognition-disclaimer">
            AI 인식 결과는 정확하지 않을 수 있으므로 제품 라벨과 함께 확인해 주세요.
          </p>
        </div>
      )}
    </section>
  );
}

function productTypeName(value: string | null): string {
  const names: Record<string, string> = {
    AIR_CONDITIONER: "에어컨",
    REFRIGERATOR: "냉장고",
    WASHING_MACHINE: "세탁기",
    TV: "TV",
    OTHER: "기타",
  };
  return value ? names[value] ?? value : "확인되지 않음";
}

function toMessage(error: unknown): string {
  if (error instanceof ApiError) return error.message;
  if (error instanceof Error) return error.message;
  return "제품 정보 인식을 완료하지 못했습니다. 잠시 후 다시 시도해 주세요.";
}
