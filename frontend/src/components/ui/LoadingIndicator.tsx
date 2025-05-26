import React from "react";

const LoadingIndicator: React.FC = () => (
  <div role="status" aria-live="polite" style={{ textAlign: "center", margin: "2rem" }}>
    <span>Yükleniyor...</span>
  </div>
);

export default LoadingIndicator; 