import { useState } from "react";

interface DesiredDateTimeInputProps {
  name: string;
}

export function DesiredDateTimeInput({ name }: DesiredDateTimeInputProps) {
  const [value, setValue] = useState("");
  const [date, setDate] = useState("");
  const [time, setTime] = useState("");
  const [open, setOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);

  function openPicker() {
    const initial = value ? value.split(" ") : [getLocalDate(), getLocalTimeMinimum()];
    setDate(initial[0]);
    setTime(initial[1]);
    setError(null);
    setOpen(true);
  }

  function confirmSelection() {
    const selected = new Date(`${date}T${time}`);
    if (!date || !time || Number.isNaN(selected.getTime())) {
      setError("희망 날짜와 시간을 선택해 주세요.");
      return;
    }
    if (selected.getTime() <= Date.now()) {
      setError("희망 일시는 현재 시각 이후로 선택해 주세요.");
      return;
    }

    const normalizedTime = time.length === 5 ? `${time}:00` : time;
    setValue(`${date} ${normalizedTime}`);
    setOpen(false);
    setError(null);
  }

  return (
    <div className="date-time-field">
      <label className="date-time-label" htmlFor={`${name}-display`}>
        희망 날짜 및 시간 <span className="required">*</span>
      </label>
      <div className="date-time-row">
        <input
          id={`${name}-display`}
          name={name}
          onClick={openPicker}
          placeholder="yyyy-MM-dd HH:mm:ss"
          readOnly
          required
          value={value}
        />
        <button className="address-search-button" onClick={openPicker} type="button">
          날짜·시간 선택
        </button>
      </div>
      <small className="field-hint">표시 형식: yyyy-MM-dd HH:mm:ss</small>
      {open && (
        <div className="popup-backdrop" onMouseDown={(event) => {
          if (event.target === event.currentTarget) {
            setOpen(false);
          }
        }}>
          <section aria-labelledby={`${name}-dialog-title`} aria-modal="true" className="popup-card date-time-popup" role="dialog">
            <div className="popup-heading">
              <h2 id={`${name}-dialog-title`}>희망 날짜 및 시간</h2>
              <button aria-label="날짜 및 시간 선택 닫기" className="text-button" onClick={() => setOpen(false)} type="button">
                닫기
              </button>
            </div>
            <div className="date-time-controls">
              <label>
                날짜
                <input
                  min={getLocalDate()}
                  onChange={(event) => setDate(event.target.value)}
                  required
                  type="date"
                  value={date}
                />
              </label>
              <label>
                시간
                <input
                  min={date === getLocalDate() ? getLocalTimeMinimum() : undefined}
                  onChange={(event) => setTime(event.target.value)}
                  required
                  step={1}
                  type="time"
                  value={time}
                />
              </label>
            </div>
            {error && <p className="form-error" role="alert">{error}</p>}
            <button className="primary-button" onClick={confirmSelection} type="button">
              선택 완료
            </button>
          </section>
        </div>
      )}
    </div>
  );
}

function getLocalDate(): string {
  const now = new Date();
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
}

function getLocalTimeMinimum(): string {
  const minimum = new Date();
  minimum.setMinutes(minimum.getMinutes() + 1, 0, 0);
  return `${pad(minimum.getHours())}:${pad(minimum.getMinutes())}:${pad(minimum.getSeconds())}`;
}

function pad(value: number): string {
  return String(value).padStart(2, "0");
}
