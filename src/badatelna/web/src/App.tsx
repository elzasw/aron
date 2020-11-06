import React from "react";
import { BrowserRouter as Router, Switch, Route } from "react-router-dom";

import { AppWrapper } from "./components";
import { navigationItems } from "./enums";

function App() {
  return (
    <Router>
      <AppWrapper>
        <Switch>
          {navigationItems.map(
            ({ path, exact = false, Component, label }: any) => (
              <Route {...{ key: path, path, exact }}>
                <Component {...{ path, label }} />
              </Route>
            )
          )}
        </Switch>
      </AppWrapper>
    </Router>
  );
}

export default App;
