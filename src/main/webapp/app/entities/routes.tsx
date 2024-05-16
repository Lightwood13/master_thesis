import React from 'react';
import { Route } from 'react-router-dom';

import ErrorBoundaryRoutes from 'app/shared/error/error-boundary-routes';
import HeaterList from 'app/modules/heater-list/heater-list';
import HeaterPage from 'app/modules/heater/heater';

/* jhipster-needle-add-route-import - JHipster will add routes here */

export default () => {
  return (
    <div>
      <ErrorBoundaryRoutes>
        <Route path="heaters" element={<HeaterList />} />
        <Route path="heaters/:serial" element={<HeaterPage />} />
        {/* prettier-ignore */}
        {/* jhipster-needle-add-route-path - JHipster will add routes here */}
      </ErrorBoundaryRoutes>
    </div>
  );
};
