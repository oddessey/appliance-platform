import { TechnicianDashboard } from "../components/TechnicianDashboard";

interface TechnicianScreenProps {
  technicianName: string;
  profileOpen: boolean;
  onOpenProfile: () => void;
  onCloseProfile: () => void;
}

export function TechnicianScreen({
  technicianName,
  profileOpen,
  onOpenProfile,
  onCloseProfile,
}: TechnicianScreenProps) {
  return (
    <>
      <div className="form-card installation-card technician-work-card">
        <TechnicianDashboard
          onCloseProfile={onCloseProfile}
          onOpenProfile={onOpenProfile}
          profileOpen={profileOpen}
          technicianName={technicianName}
        />
      </div>
      <aside className="info-panel technician-info-panel technician-status-panel">
        <span className="step-label">WORK FLOW</span>
        <h2>안전하고 정확한<br />작업 진행</h2>
        <ol className="process-list">
          <li><span>1</span><div><strong>배정 검토 및 응답</strong><small>작업 일정과 주소를 확인하고 수락하거나 거절 사유를 제출하세요.</small></div></li>
          <li><span>2</span><div><strong>출발·작업 시작</strong><small>수락한 작업은 출발 및 작업 시작 상태를 순서대로 기록하세요.</small></div></li>
          <li><span>3</span><div><strong>작업 완료</strong><small>작업이 끝나면 완료 처리하고 필요한 증빙을 등록하세요.</small></div></li>
        </ol>
        <div className="support-note">
          <span aria-hidden="true">✦</span>
          <p>고객 연락처와 주소는<br />배정된 작업에서만 확인됩니다.</p>
        </div>
      </aside>
    </>
  );
}
