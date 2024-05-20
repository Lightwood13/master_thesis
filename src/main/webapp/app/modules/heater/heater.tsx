import React, { ComponentType, SyntheticEvent, useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { ChartData, ChartOptions } from 'chart.js';
import 'chartjs-adapter-luxon';
import { Line } from 'react-chartjs-2';
import { DateTime } from 'luxon';
import { Card, Color, DateRangePicker, DateRangePickerValue, Icon } from '@tremor/react';
import { Grid, Tab, Tabs } from '@mui/material';
import { RiBrainLine, RiTempColdLine, RiTempHotLine, RiHome8Line, RiProgress6Line, RiCheckboxCircleLine } from '@remixicon/react';
import { SlEnergy } from 'react-icons/sl';
import { LuBarChartBig } from 'react-icons/lu';
import { PiEmpty } from 'react-icons/pi';

import { useAppDispatch, useAppSelector } from 'app/config/store';
import {
  fetchHeater,
  fetchStatistics,
  StatisticsAggregationPeriod,
  StatisticsData,
  StatisticsField,
} from 'app/modules/heater/heater.reducer';
import ModelTable from 'app/modules/heater/table';

function StatisticsCard({ title, value, icon, color }: { title: string; value: string; icon: ComponentType; color: Color }) {
  return (
    <Card className="mx-auto max-w-xs h-full" decoration="top" decorationColor={color}>
      <div className="flex items-center space-x-6 h-full">
        <Icon icon={icon} color={color} variant="shadow" size="lg" />
        <div>
          <p className="text-tremor-default text-tremor-content">{title}</p>
          <p className="text-tremor-metric font-semibold text-tremor-content-strong">{value}</p>
        </div>
      </div>
    </Card>
  );
}

export const HeaterPage = () => {
  const { serial } = useParams<'serial'>() as { serial: string };
  const [activeTab, setActiveTab] = useState(0);
  const [dateRange, setDateRange] = useState({
    from: DateTime.now().startOf('day').minus({ days: 7 }).toJSDate(),
    to: DateTime.now().startOf('day').toJSDate(),
    selectValue: 'w',
  }) as [DateRangePickerValue, (value: DateRangePickerValue) => void];

  const handleTabChange = (event: SyntheticEvent, newValue: number) => {
    setActiveTab(newValue);
  };

  const heater = useAppSelector(state => state.heater.heater);
  const statistics: StatisticsData | null = useAppSelector(state => state.heater.statistics);

  const dispatch = useAppDispatch();

  useEffect(() => {
    dispatch(fetchHeater(serial));
  }, []);

  useEffect(() => {
    const startTime = dateRange.from ? DateTime.fromJSDate(dateRange.from) : DateTime.fromISO('2024-05-14T00:00:00+03:00');
    const endTime = dateRange.to ? DateTime.fromJSDate(dateRange.to) : DateTime.fromISO('2024-05-14T00:00:00+03:00');
    let aggregationPeriod: StatisticsAggregationPeriod;
    switch (dateRange.selectValue) {
      case 'w':
        aggregationPeriod = StatisticsAggregationPeriod.HOUR;
        break;
      default:
        aggregationPeriod = StatisticsAggregationPeriod.HOUR;
        break;
    }

    dispatch(
      fetchStatistics({
        serial,
        fields: [StatisticsField.ROOM_TEMPERATURE],
        startTime,
        endTime: endTime.plus({ days: 1 }),
        aggregationPeriod,
        timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone,
      })
    );
  }, [dateRange]);

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
            unit: 'day',
            // unit: 'hour',
            displayFormats: {
              day: 'dd MMM',
              hour: 'HH:mm',
            },
          },
          // ticks: {
          //   maxTicksLimit: 4
          // }
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
      <div>
        <Line data={chartData} options={chartOptions} />
      </div>
    );
  } else {
    statisticsElem = <div></div>;
  }

  console.log(dateRange);

  return (
    <div className="space-y-1">
      <div className="flex items-center">
        <Icon icon={RiHome8Line} size="xl" />
        <p className="text-3xl">Heater {heater.serial}</p>
      </div>

      <Grid container spacing={2}>
        <Grid item xs={7}>
          <Card className="space-y-4">
            <Tabs value={activeTab} onChange={handleTabChange} centered>
              <Tab label="Temperature" />
              <Tab label="Energy consumption" />
              <Tab label="Electricity price" />
              <Tab label="Cost" />
            </Tabs>
            <DateRangePicker className="mx-auto max-w-md" value={dateRange} onValueChange={setDateRange} />
            {statisticsElem}
          </Card>
        </Grid>
        <Grid item xs={5}>
          <Grid container spacing={2} className="h-full" style={{ marginBottom: 29 }}>
            <Grid item xs={6}>
              <StatisticsCard title="Room temperature" value="19.1 °C" icon={RiTempHotLine} color="amber" />
            </Grid>
            <Grid item xs={6}>
              <StatisticsCard title="Outside temperature" value="2.6 °C" icon={RiTempColdLine} color="indigo" />
            </Grid>
            <Grid item xs={6}>
              <StatisticsCard title="Heater power" value="1000 W" icon={SlEnergy} color="green" />
            </Grid>
            <Grid item xs={6}>
              <StatisticsCard title="Consumption this week" value="56 kWh" icon={LuBarChartBig} color="red" />
            </Grid>
            <Grid item xs={12}>
              <Card className="space-y-4 h-full" decoration="top" decorationColor="sky">
                <div className="flex items-center space-x-6">
                  <Icon icon={RiBrainLine} color="sky" variant="shadow" size="lg" />
                  <div>
                    <p className="text-tremor-default text-tremor-content">Active model</p>
                    <p className="text-tremor-title font-semibold text-tremor-content-strong min-w-32">Test Model</p>
                  </div>
                  {/* <div className="flex items-center space-x-6 pl-8"> */}
                  {/*   <Icon */}
                  {/*     icon={PiEmpty} */}
                  {/*     color="red" */}
                  {/*     variant="shadow" */}
                  {/*     size="lg" */}
                  {/*   /> */}
                  {/*   <div> */}
                  {/*     <p className="text-tremor-default text-tremor-content">Status</p> */}
                  {/*     <p className="text-tremor-title font-semibold text-tremor-content-strong">No active model</p> */}
                  {/*   </div> */}
                  {/* </div> */}
                  <div className="flex items-center space-x-6 pl-8">
                    <Icon icon={RiProgress6Line} color="yellow" variant="shadow" size="lg" />
                    <div>
                      <p className="text-tremor-default text-tremor-content">Status</p>
                      <p className="text-tremor-title font-semibold text-tremor-content-strong">Gathering training data</p>
                    </div>
                  </div>
                  {/* <div className="flex items-center space-x-6 pl-8"> */}
                  {/*   <Icon */}
                  {/*     icon={RiCheckboxCircleLine} */}
                  {/*     color="green" */}
                  {/*     variant="shadow" */}
                  {/*     size="lg" */}
                  {/*   /> */}
                  {/*   <div> */}
                  {/*     <p className="text-tremor-default text-tremor-content">Status</p> */}
                  {/*     <p className="text-tremor-title font-semibold text-tremor-content-strong">Working</p> */}
                  {/*   </div> */}
                  {/* </div> */}
                </div>
                <Grid container spacing={2}>
                  <Grid item xs={6}>
                    <div>
                      <p className="text-tremor-default text-tremor-content">Target temperature</p>
                      <p className="text-2xl font-semibold text-tremor-content-strong">21 °C</p>
                    </div>
                  </Grid>
                  <Grid item xs={6}>
                    <div>
                      <p className="text-tremor-default text-tremor-content">Allowed temperature range</p>
                      <p className="text-2xl font-semibold text-tremor-content-strong">19-23 °C</p>
                    </div>
                  </Grid>
                  <Grid item xs={6}>
                    <div>
                      <p className="text-tremor-default text-tremor-content">Active since</p>
                      <p className="text-2xl font-semibold text-tremor-content-strong">16 May 2024</p>
                    </div>
                  </Grid>
                  <Grid item xs={6}>
                    <div>
                      <p className="text-tremor-default text-tremor-content">Estimated money savings</p>
                      <p className="text-2xl font-semibold text-tremor-content-strong">-</p>
                    </div>
                  </Grid>
                </Grid>
              </Card>
            </Grid>
          </Grid>
        </Grid>
      </Grid>

      <ModelTable />
    </div>
  );
};

export default HeaterPage;
