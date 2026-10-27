import { useEffect, useRef, useState } from "react";

interface PostcodeData {
  roadAddress: string;
  jibunAddress: string;
  zonecode: string;
}

interface PostcodeWidget {
  embed(target: HTMLElement): void;
}

declare global {
  interface Window {
    daum?: {
      Postcode: new (options: { oncomplete: (data: PostcodeData) => void }) => PostcodeWidget;
    };
  }
}

interface AddressSearchInputProps {
  label: string;
  name: string;
}

let postcodeScriptPromise: Promise<void> | null = null;

export function AddressSearchInput({ label, name }: AddressSearchInputProps) {
  const [address, setAddress] = useState("");
  const [detailAddress, setDetailAddress] = useState("");
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const postcodeContainer = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) {
      return;
    }

    let active = true;
    setLoading(true);
    setError(null);
    loadPostcodeScript()
      .then(() => {
        if (!active || !postcodeContainer.current || !window.daum?.Postcode) {
          return;
        }

        new window.daum.Postcode({
          oncomplete: (data) => {
            const selectedAddress = data.roadAddress || data.jibunAddress;
            if (!selectedAddress) {
              setError("선택한 주소를 확인할 수 없습니다. 다시 검색해 주세요.");
              return;
            }
            setAddress(data.zonecode ? `[${data.zonecode}] ${selectedAddress}` : selectedAddress);
            setDetailAddress("");
            setOpen(false);
          },
        }).embed(postcodeContainer.current);
      })
      .catch(() => {
        if (active) {
          setError("주소 검색 서비스를 불러오지 못했습니다. 네트워크 연결 후 다시 시도해 주세요.");
        }
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, [open]);

  const fullAddress = [address, detailAddress.trim()].filter(Boolean).join(" ");

  return (
    <div className="address-field">
      <span className="address-label">
        {label} <span className="required">*</span>
      </span>
      <div className="address-search-row">
        <input
          autoComplete="street-address"
          maxLength={500}
          name={name}
          onChange={(event) => {
            setAddress(event.target.value);
            setDetailAddress("");
          }}
          placeholder="주소를 직접 입력하거나 주소 검색으로 선택해 주세요"
          required
          value={fullAddress}
        />
        <button className="address-search-button" onClick={() => setOpen(true)} type="button">
          주소 검색
        </button>
      </div>
      <input
        aria-label={`${label} 상세 주소`}
        autoComplete="address-line2"
        className="address-detail-input"
        maxLength={300}
        onChange={(event) => setDetailAddress(event.target.value)}
        placeholder="상세 주소를 입력해 주세요 (선택)"
        value={detailAddress}
      />
      {open && (
        <div className="popup-backdrop address-search-backdrop" onMouseDown={(event) => {
          if (event.target === event.currentTarget) {
            setOpen(false);
          }
        }}>
          <section
            aria-labelledby={`${name}-dialog-title`}
            aria-modal="true"
            className="popup-card address-search-dialog"
            role="dialog"
          >
            <div className="popup-heading">
              <h2 id={`${name}-dialog-title`}>주소 검색</h2>
              <button aria-label="주소 검색 닫기" className="text-button" onClick={() => setOpen(false)} type="button">
                닫기
              </button>
            </div>
            {loading && <p aria-live="polite">주소 검색 서비스를 불러오는 중...</p>}
            {error && (
              <div className="load-error" role="alert">
                <p className="inline-error">{error}</p>
                <button className="text-button" onClick={() => setOpen(false)} type="button">닫기</button>
              </div>
            )}
            <div className="postcode-container" ref={postcodeContainer} />
          </section>
        </div>
      )}
    </div>
  );
}

function loadPostcodeScript(): Promise<void> {
  if (window.daum?.Postcode) {
    return Promise.resolve();
  }
  if (postcodeScriptPromise) {
    return postcodeScriptPromise;
  }

  postcodeScriptPromise = new Promise<void>((resolve, reject) => {
    const script = document.createElement("script");
    script.src = "https://t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js";
    script.async = true;
    script.onload = () => {
      if (window.daum?.Postcode) {
        resolve();
      } else {
        reject(new Error("Postcode widget was not initialized"));
      }
    };
    script.onerror = () => {
      script.remove();
      postcodeScriptPromise = null;
      reject(new Error("Postcode script could not be loaded"));
    };
    document.head.append(script);
  });

  return postcodeScriptPromise;
}
