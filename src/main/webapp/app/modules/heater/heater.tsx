import React, { useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { ChartData, ChartOptions } from 'chart.js';
import 'chartjs-adapter-luxon';
import { Bar, Line } from 'react-chartjs-2';
import { DateTime } from 'luxon';
import { Card } from '@tremor/react';

import { useAppDispatch, useAppSelector } from 'app/config/store';
import {
  fetchHeater,
  fetchStatistics,
  StatisticsAggregationPeriod,
  StatisticsData,
  StatisticsField,
} from 'app/modules/heater/heater.reducer';

export const HeaterPage = () => {
  const { serial } = useParams<'serial'>() as { serial: string };

  const heater = useAppSelector(state => state.heater.heater);
  const statistics: StatisticsData | null = useAppSelector(state => state.heater.statistics);

  const dispatch = useAppDispatch();

  useEffect(() => {
    dispatch(fetchHeater(serial));
    dispatch(
      fetchStatistics({
        serial,
        fields: [StatisticsField.ROOM_TEMPERATURE],
        startTime: DateTime.fromISO('2024-05-14T00:00:00+03:00'),
        endTime: DateTime.fromISO('2024-05-15T00:00:00+03:00'),
        aggregationPeriod: StatisticsAggregationPeriod.HOUR,
        timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone,
      })
    );
  }, []);

  if (heater === null) {
    return <p>Loading...</p>;
  }

  let statisticsElem: JSX.Element;
  if (statistics !== null) {
    const chartOptions: ChartOptions<'line'> = {
      scales: {
        x: {
          type: 'time',
          time: {
            displayFormats: {
              hour: 'HH:mm',
            },
          },
        },
      },
    };

    const chartData: ChartData<'line'> = {
      labels: statistics.map(dataPoint => dataPoint.periodStart),
      datasets: [
        {
          label: 'Room Temperature',
          data: statistics.map(dataPoint => dataPoint.data[StatisticsField.ROOM_TEMPERATURE]),
        },
      ],
    };

    statisticsElem = (
      <div className="w-3/4">
        <Line data={chartData} options={chartOptions} />
      </div>
    );
  } else {
    statisticsElem = <div></div>;
  }

  return (
    <div>
      <p key={heater.id} className="text-3xl underline">
        {heater.id} {heater.serial}
      </p>
      {statisticsElem}
      <Card className="max-w-xs">
        <span className="text-tremor-default text-tremor-content dark:text-dark-tremor-content">Total Requests</span>
        <p className="text-tremor-metric font-semibold text-tremor-content-strong dark:text-dark-tremor-content-strong">6,568</p>
      </Card>
    </div>
  );
};

export default HeaterPage;
