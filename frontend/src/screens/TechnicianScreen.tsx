import { TechnicianDashboard } from "../components/TechnicianDashboard";

interface TechnicianScreenProps {
  technicianName: string;
}

export function TechnicianScreen({ technicianName }: TechnicianScreenProps) {
  return (
    <>
      <div className="form-card installation-card technician-work-card">
        <TechnicianDashboard technicianName={technicianName} />
      </div>
      <aside className="info-panel technician-info-panel technician-status-panel">
        <span className="step-label">WORK FLOW</span>
        <h2>안전하고 정확한<br />작업 진행</h2>
        <ol className="process-list">
          <li><span>1</span><div><strong>배정 확인</strong><small>작업 주소와 고객 요청을 확인하세요.</small></div></li>
          <li><span>2</span><div><strong>진행 상태 업데이트</strong><small>출발·작업 시작 상태를 기록하세요.</small></div></li>
          <li><span>3</span><div><strong>작업 완료</strong><small>작업이 끝나면 완료 처리하세요.</small></div></li>
        </ol>
        <div className="support-note">
          <span aria-hidden="true">✦</span>
          <p>고객 연락처와 주소는<br />배정된 작업에서만 확인됩니다.</p>
        </div>
      </aside>
    </>
  );
}
