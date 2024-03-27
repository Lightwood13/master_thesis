import React, { useEffect } from 'react';

import { useAppDispatch, useAppSelector } from 'app/config/store';
import { fetchHeaterList, Heater } from 'app/modules/heater-list/heater-list.reducer';

export const HeaterList = () => {
  const heaters: Heater[] = useAppSelector(state => state.heaterList.heaters);

  const dispatch = useAppDispatch();

  useEffect(() => {
    dispatch(fetchHeaterList());
  }, []);

  const heaterListComponent = heaters.map(heater => (
    <p key={heater.id}>
      {heater.id} {heater.serial}
    </p>
  ));

  return <div>{heaterListComponent}</div>;
};

export default HeaterList;
