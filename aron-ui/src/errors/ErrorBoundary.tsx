import { Component, type ErrorInfo, type ReactNode } from "react";
import { withTranslation, type WithTranslation } from "react-i18next";

interface Props extends WithTranslation {
  children: ReactNode;
}

interface State {
  error: Error | null;
}

/**
 * Last-resort boundary around the whole application: a render crash shows a
 * readable error with its detail instead of a blank page (the class of failure
 * the old UI suffered from). Errors must stay visible so they get resolved.
 */
class ErrorBoundary extends Component<Props, State> {
  state: State = { error: null };

  static getDerivedStateFromError(error: Error): State {
    return { error };
  }

  componentDidCatch(error: Error, info: ErrorInfo): void {
    // the console keeps the full component stack for diagnosis
    console.error("Unhandled application error", error, info.componentStack);
  }

  render() {
    const { t, children } = this.props;
    if (this.state.error === null) {
      return children;
    }
    return (
      <div role="alert" style={{ padding: "2rem", maxWidth: "60rem", margin: "0 auto" }}>
        <h1>{t("errors.boundary.heading")}</h1>
        <p>{t("errors.boundary.text")}</p>
        <pre style={{ whiteSpace: "pre-wrap", overflowWrap: "anywhere" }}>
          {this.state.error.message}
        </pre>
        <button type="button" onClick={() => window.location.reload()}>
          {t("errors.boundary.reload")}
        </button>
      </div>
    );
  }
}

export default withTranslation()(ErrorBoundary);
