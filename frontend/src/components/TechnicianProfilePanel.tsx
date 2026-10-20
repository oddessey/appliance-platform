import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { getApplianceTypes, getTechnicianProfile, saveTechnicianProfile } from "../api/installations";
import { ApiError } from "../api/client";
import type {
  ApplianceType,
  TechnicianAvailability,
  TechnicianProfile,
} from "../types/installation";

interface TechnicianProfilePanelProps {
  onClose: () => void;
}

interface DaySchedule {
  enabled: boolean;
  startTime: string;
  endTime: string;
}

const DAY_LABELS = ["월", "화", "수", "목", "금", "토", "일"];

export function TechnicianProfilePanel({ onClose }: TechnicianProfilePanelProps) {
  const [applianceTypes, setApplianceTypes] = useState<ApplianceType[]>([]);
  const [profile, setProfile] = useState<TechnicianProfile | null>(null);
  const [businessName, setBusinessName] = useState("");
  const [licenseInfo, setLicenseInfo] = useState("");
  const [areasText, setAreasText] = useState("");
  const [capabilityCodes, setCapabilityCodes] = useState<string[]>([]);
  const [schedule, setSchedule] = useState<DaySchedule[]>(
    DAY_LABELS.map(() => ({ enabled: false, startTime: "09:00", endTime: "18:00" })),
  );
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;
    async function load() {
      setLoading(true);
      setError(null);
      try {
        const types = await getApplianceTypes();
        if (mounted) setApplianceTypes(types);
      } catch (requestError) {
        if (mounted) setError(errorMessage(requestError));
      }
      try {
        const result = await getTechnicianProfile();
        if (mounted) applyProfile(result);
      } catch (requestError) {
        if (!(requestError instanceof ApiError && requestError.status === 404) && mounted) {
          setError(errorMessage(requestError));
        }
      } finally {
        if (mounted) setLoading(false);
      }
    }
    void load();
    return () => {
      mounted = false;
    };
  }, []);

  function applyProfile(value: TechnicianProfile) {
    setProfile(value);
    setBusinessName(value.businessName);
    setLicenseInfo(value.licenseInfo ?? "");
    setAreasText(value.serviceAreas.join("\n"));
    setCapabilityCodes(value.applianceTypeCodes);
    setSchedule(DAY_LABELS.map((_, index) => {
      const slot = value.availability.find((item) => item.dayOfWeek === index + 1);
      return slot
        ? { enabled: true, startTime: slot.startTime.slice(0, 5), endTime: slot.endTime.slice(0, 5) }
        : { enabled: false, startTime: "09:00", endTime: "18:00" };
    }));
  }

  function updateSchedule(day: number, change: Partial<DaySchedule>) {
    setSchedule((current) => current.map((slot, index) =>
      index === day ? { ...slot, ...change } : slot));
  }

  function toggleCapability(code: string) {
    setCapabilityCodes((current) => current.includes(code)
      ? current.filter((item) => item !== code)
      : [...current, code]);
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setError(null);
    setMessage(null);
    try {
      const areas = [...new Set(areasText.split(/\r?\n/).map((area) => area.trim()).filter(Boolean))];
      const availability: TechnicianAvailability[] = schedule.flatMap((slot, index) => slot.enabled
        ? [{
            dayOfWeek: index + 1,
            startTime: `${slot.startTime}:00`,
            endTime: `${slot.endTime}:00`,
          }]
        : []);
      const updated = await saveTechnicianProfile({
        businessName: businessName.trim(),
        licenseInfo: licenseInfo.trim(),
        serviceAreas: areas,
        applianceTypeCodes: capabilityCodes,
        availability,
      });
      applyProfile(updated);
      setMessage("기사 프로필과 작업 가능 정보를 저장했습니다.");
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setSaving(false);
    }
  }

  return (
    <section aria-labelledby="technician-profile-title" className="technician-profile-panel">
      <div className="technician-profile-heading">
        <div>
          <span className="step-label">TECHNICIAN PROFILE</span>
          <h2 id="technician-profile-title">기사 프로필 및 작업 가능 정보</h2>
          {profile && (
            <p role="status">
              승인 상태: {approvalLabel(profile.approvalStatus)}
              {profile.active ? " · 배정 활성" : " · 배정 비활성"}
            </p>
          )}
        </div>
        <button className="text-button" onClick={onClose} type="button">작업 목록</button>
      </div>

      {error && <p className="inline-error" role="alert">{error}</p>}
      {message && <p className="success-message" role="status">{message}</p>}
      {loading ? (
        <p aria-live="polite" className="history-loading">프로필 정보를 불러오는 중...</p>
      ) : (
        <form className="technician-profile-form" onSubmit={(event) => void submit(event)}>
          <label>
            사업자명
            <input
              maxLength={150}
              onChange={(event) => setBusinessName(event.target.value)}
              required
              value={businessName}
            />
          </label>
          <label>
            자격·면허 정보
            <textarea
              maxLength={2000}
              onChange={(event) => setLicenseInfo(event.target.value)}
              placeholder="예: 보유 자격·면허 및 관련 장비 정보"
              rows={3}
              value={licenseInfo}
            />
          </label>
          <label>
            서비스 지역 (시·도와 시·군·구, 한 줄에 한 지역)
            <textarea
              onChange={(event) => setAreasText(event.target.value)}
              placeholder={"서울특별시 강남구\n경기도 성남시"}
              required
              rows={3}
              value={areasText}
            />
          </label>
          <fieldset>
            <legend>취급 가능한 품목</legend>
            <div className="technician-capabilities">
              {applianceTypes.map((type) => (
                <label key={type.code}>
                  <input
                    checked={capabilityCodes.includes(type.code)}
                    onChange={() => toggleCapability(type.code)}
                    type="checkbox"
                  />
                  {type.name}
                </label>
              ))}
            </div>
          </fieldset>
          <fieldset>
            <legend>주간 작업 가능 시간 (한국 표준시)</legend>
            <div className="technician-schedule">
              {schedule.map((slot, index) => (
                <div className="technician-schedule-row" key={DAY_LABELS[index]}>
                  <label>
                    <input
                      checked={slot.enabled}
                      onChange={(event) => updateSchedule(index, { enabled: event.target.checked })}
                      type="checkbox"
                    />
                    {DAY_LABELS[index]}요일
                  </label>
                  <input
                    aria-label={`${DAY_LABELS[index]}요일 시작 시각`}
                    disabled={!slot.enabled}
                    onChange={(event) => updateSchedule(index, { startTime: event.target.value })}
                    required={slot.enabled}
                    type="time"
                    value={slot.startTime}
                  />
                  <span>~</span>
                  <input
                    aria-label={`${DAY_LABELS[index]}요일 종료 시각`}
                    disabled={!slot.enabled}
                    onChange={(event) => updateSchedule(index, { endTime: event.target.value })}
                    required={slot.enabled}
                    type="time"
                    value={slot.endTime}
                  />
                </div>
              ))}
            </div>
          </fieldset>
          <button
            className="primary-button"
            disabled={saving || capabilityCodes.length === 0 || !schedule.some((slot) => slot.enabled)}
            type="submit"
          >
            {saving ? "저장 중..." : "프로필 저장"}
          </button>
          <p className="technician-profile-note">
            새 프로필은 관리자 승인 후 배정 대상이 됩니다. 자격 정보를 수정하면 재검토 전까지 승인이 해제됩니다.
            지역은 예약 주소의 시·도 및 시·군·구와 일치해야 합니다.
          </p>
        </form>
      )}
    </section>
  );
}

function approvalLabel(status: TechnicianProfile["approvalStatus"]): string {
  const labels = { PENDING: "승인 대기", APPROVED: "승인", REJECTED: "반려" };
  return labels[status];
}

function errorMessage(error: unknown): string {
  if (error instanceof Error) return error.message;
  return "기사 정보를 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.";
}
