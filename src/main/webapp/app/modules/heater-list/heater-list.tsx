import React, { useEffect } from 'react';

import { useAppDispatch, useAppSelector } from 'app/config/store';
import { fetchHeaterList, Heater } from 'app/modules/heater-list/heater-list.reducer';
import { Link } from 'react-router-dom';

export const HeaterList = () => {
  const heaters: Heater[] = useAppSelector(state => state.heaterList.heaters);

  const dispatch = useAppDispatch();

  useEffect(() => {
    dispatch(fetchHeaterList());
  }, []);

  const heaterListComponent = heaters.map(heater => (
    <p key={heater.id}>
      {heater.id} <Link to={heater.serial}>{heater.serial}</Link>
    </p>
  ));

  return <div>{heaterListComponent}</div>;
};

export default HeaterList;
