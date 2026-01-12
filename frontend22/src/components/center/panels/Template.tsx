import { FileMusic, LayoutTemplate, Plus } from "lucide-react";

const Template: React.FC = () => {
  return (
    <div className="ui-panel">
      <h3 className="ui-panel-title">Templates</h3>

      <div className="ui-panel-sub-section">
        <h4 className="ui-panel-sub-title">New Score</h4>
        <button
          type="button"
          className="ui-list-item text-brand-primary font-medium"
        >
          <Plus size={16} />
          <span>Blank Score</span>
        </button>
      </div>

      <div className="ui-panel-sub-section">
        <h4 className="ui-panel-sub-title">Ensembles</h4>
        <div className="flex flex-col gap-0.5">
          <button type="button" className="ui-list-item">
            <LayoutTemplate size={16} />
            <span>String Quartet</span>
          </button>
          <button type="button" className="ui-list-item">
            <LayoutTemplate size={16} />
            <span>Wind Quintet</span>
          </button>
          <button type="button" className="ui-list-item">
            <LayoutTemplate size={16} />
            <span>Brass Trio</span>
          </button>
        </div>
      </div>

      <div className="ui-panel-sub-section">
        <h4 className="ui-panel-sub-title">Solo Instruments</h4>
        <div className="flex flex-col gap-0.5">
          <button type="button" className="ui-list-item">
            <FileMusic size={16} />
            <span>Piano Solo</span>
          </button>
          <button type="button" className="ui-list-item">
            <FileMusic size={16} />
            <span>Voice & Piano</span>
          </button>
          <button type="button" className="ui-list-item">
            <FileMusic size={16} />
            <span>Guitar Tab</span>
          </button>
        </div>
      </div>
    </div>
  );
};

export default Template;
