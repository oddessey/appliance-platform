import { useState } from "react";
import { AdminMemberManagement } from "../components/AdminMemberManagement";
import { AdminInstallationReviewPanel } from "../components/AdminInstallationReviewPanel";
import { PricingAdminPanel } from "../components/PricingAdminPanel";
import { ReservationAdminPanel } from "../components/ReservationAdminPanel";
import { TechnicianAdminPanel } from "../components/TechnicianAdminPanel";

type AdminModule = "installations" | "members" | "technicians" | "pricing" | "reservations";

const ADMIN_MODULES: Array<{ id: AdminModule; label: string }> = [
  { id: "installations", label: "요청·AI 검토" },
  { id: "members", label: "회원 관리" },
  { id: "technicians", label: "기사·배정 관리" },
  { id: "pricing", label: "가격·견적 관리" },
  { id: "reservations", label: "예약 관리" },
];

interface AdminScreenProps {
  currentMemberId: string;
}

export function AdminScreen({ currentMemberId }: AdminScreenProps) {
  const [activeModule, setActiveModule] = useState<AdminModule>("installations");

  return (
    <>
      <div className="form-card installation-card admin-console-card">
        <header className="admin-workspace-heading">
          <span className="step-label">ADMIN WORKSPACE</span>
          <h2>운영 업무를 한곳에서 관리하세요.</h2>
          <p>요청 검토부터 회원·기사, 가격 정책과 예약 관리까지 필요한 업무를 선택하세요.</p>
        </header>
        <div aria-label="관리자 업무 메뉴" className="admin-module-tabs" role="tablist">
          {ADMIN_MODULES.map((module) => (
            <button
              aria-controls={`admin-panel-${module.id}`}
              aria-selected={activeModule === module.id}
              className={activeModule === module.id ? "active" : ""}
              id={`admin-tab-${module.id}`}
              key={module.id}
              onClick={() => setActiveModule(module.id)}
              role="tab"
              type="button"
            >
              {module.label}
            </button>
          ))}
        </div>
        <div
          aria-labelledby={`admin-tab-${activeModule}`}
          className="admin-module-panel"
          id={`admin-panel-${activeModule}`}
          role="tabpanel"
        >
          {activeModule === "installations" && <AdminInstallationReviewPanel />}
          {activeModule === "members" && <AdminMemberManagement currentMemberId={currentMemberId} />}
          {activeModule === "technicians" && (
            <TechnicianAdminPanel onReviewRequests={() => setActiveModule("installations")} />
          )}
          {activeModule === "pricing" && (
            <PricingAdminPanel onClose={() => setActiveModule("installations")} />
          )}
          {activeModule === "reservations" && (
            <ReservationAdminPanel onClose={() => setActiveModule("installations")} />
          )}
        </div>
      </div>
      <aside className="info-panel admin-info-panel">
        <span className="step-label">ADMIN WORKFLOW</span>
        <h2>운영 흐름을<br />순서대로 확인하세요.</h2>
        <ol className="process-list">
          <li><span>1</span><div><strong>요청·분석 검토</strong><small>접수 정보와 최신 AI 결과를 확인합니다.</small></div></li>
          <li><span>2</span><div><strong>회원·기사 관리</strong><small>역할, 계정 상태, 기사 승인 정보를 검토합니다.</small></div></li>
          <li><span>3</span><div><strong>가격·견적 관리</strong><small>활성 정책을 확인하고 검증된 견적을 발행합니다.</small></div></li>
          <li><span>4</span><div><strong>예약·배정 관리</strong><small>일정과 적격 기사 배정 현황을 확인합니다.</small></div></li>
        </ol>
      </aside>
    </>
  );
}
